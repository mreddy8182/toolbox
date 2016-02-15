/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package eu.amidst.flinklink.core.learning.parametric;


import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataOnMemoryListContainer;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.learning.parametric.bayesian.*;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.CompoundVector;
import eu.amidst.core.utils.Serialization;
import eu.amidst.core.variables.Variable;
import eu.amidst.flinklink.core.data.DataFlink;
import org.apache.flink.api.common.aggregators.ConvergenceCriterion;
import org.apache.flink.api.common.aggregators.DoubleSumAggregator;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.IterativeDataSet;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.types.DoubleValue;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * This class implements the {@link ParameterLearningAlgorithm} interface, and defines the parallel Maximum Likelihood algorithm.
 *
 * <p> For an example of use follow this link </p>
 * <p> <a href="http://amidst.github.io/toolbox/CodeExamples.html#pmlexample"> http://amidst.github.io/toolbox/CodeExamples.html#pmlexample </a>  </p>
 *
 */
public class ParallelVB implements ParameterLearningAlgorithm, Serializable {

    /** Represents the serial version ID for serializing the object. */
    private static final long serialVersionUID = 4107783324901370839L;

    static Logger logger = LoggerFactory.getLogger(ParallelVB.class);

    public static String PRIOR="PRIOR";
    public static String SVB="SVB";
    public static String LATENT_VARS="LATENT_VARS";

    /**
     * Represents the {@link DataFlink} used for learning the parameters.
     */
    protected DataFlink<DataInstance> dataFlink;

    /**
     * Represents the directed acyclic graph {@link DAG}.
     */
    protected DAG dag;

    protected SVB svb;

    protected int batchSize = 100;

    protected int maximumGlobalIterations = 10;

    protected int maximumLocalIterations = 100;

    protected double globalThreshold = 0.01;

    protected double localThreshold = 0.1;

    protected double timeLimit = -1;

    protected double globalELBO = Double.NaN;

    IdenitifableModelling idenitifableModelling = new ParameterIdentifiableModel();

    boolean randomStart = true;


    public ParallelVB(){
        this.svb = new SVB();
    }


    public void setIdenitifableModelling(IdenitifableModelling idenitifableModelling) {
        this.idenitifableModelling = idenitifableModelling;
    }

    public void setPlateuStructure(PlateuStructure plateuStructure){
        this.svb.setPlateuStructure(plateuStructure);
    }

    public void setTransitionMethod(TransitionMethod transitionMethod){
        this.svb.setTransitionMethod(transitionMethod);
    }

    public void setGlobalThreshold(double globalThreshold) {
        this.globalThreshold = globalThreshold;
    }

    public void setLocalThreshold(double localThreshold) {
        this.localThreshold = localThreshold;
    }


    public void setMaximumGlobalIterations(int maximumGlobalIterations) {
        this.maximumGlobalIterations = maximumGlobalIterations;
    }

    public void setMaximumLocalIterations(int maximumLocalIterations) {
        this.maximumLocalIterations = maximumLocalIterations;
    }

    public void setTimeLimit(double timeLimit){
        this.timeLimit = timeLimit;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public SVB getSVB() {
        return svb;
    }

    public void initLearning() {
        this.svb.getPlateuStructure().getVMP().setMaxIter(this.maximumLocalIterations);
        this.svb.getPlateuStructure().getVMP().setThreshold(this.localThreshold);
        this.svb.setDAG(this.dag);
        this.svb.setWindowsSize(batchSize);
        this.svb.initLearning(); //Init learning is peformed in each mapper.
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void setDataFlink(DataFlink<DataInstance> data) {
        this.dataFlink = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLogMarginalProbability() {
        return this.globalELBO;
    }

    public DataSet<DataPosteriorAssignment> computePosteriorAssignment(List<Variable> latentVariables){

        Attribute seq_id = this.dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));
            config.setBytes(LATENT_VARS, Serialization.serializeObject(latentVariables));

            return this.dataFlink
                    .getBatchedDataSet(this.batchSize)
                    .flatMap(new ParallelVBMapInferenceAssignment())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }

    public DataSet<DataPosterior> computePosterior(List<Variable> latentVariables){

        Attribute seq_id = this.dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));
            config.setBytes(LATENT_VARS, Serialization.serializeObject(latentVariables));

            return this.dataFlink
                    .getBatchedDataSet(this.batchSize)
                    .flatMap(new ParallelVBMapInference())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }

    public DataSet<DataPosterior> computePosterior(){

        Attribute seq_id = this.dataFlink.getAttributes().getSeq_id();
        if (seq_id==null)
            throw new IllegalArgumentException("Functionality only available for data sets with a seq_id attribute");

        try{
            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));

            return this.dataFlink
                    .getBatchedDataSet(this.batchSize)
                    .flatMap(new ParallelVBMapInference())
                    .withParameters(config);

        }catch(Exception ex){
            throw new UndeclaredThrowableException(ex);
        }

    }
    public void updateModel(DataFlink<DataInstance> dataUpdate){

        try{
            final ExecutionEnvironment env = dataUpdate.getDataSet().getExecutionEnvironment();

            // get input data
            CompoundVector parameterPrior = this.svb.getNaturalParameterPrior();

            DataSet<CompoundVector> paramSet = env.fromElements(parameterPrior);

            ConvergenceCriterion convergenceELBO;
            if(timeLimit == -1) {
                convergenceELBO = new ConvergenceELBO(this.globalThreshold, System.nanoTime());
            }
            else {
                convergenceELBO = new ConvergenceELBObyTime(this.timeLimit, System.nanoTime());
                this.setMaximumGlobalIterations(5000);
            }
            // set number of bulk iterations for KMeans algorithm
            IterativeDataSet<CompoundVector> loop = paramSet.iterate(maximumGlobalIterations)
                    .registerAggregationConvergenceCriterion("ELBO_" + this.dag.getName(), new DoubleSumAggregator(),convergenceELBO);

            Configuration config = new Configuration();
            config.setString(ParameterLearningAlgorithm.BN_NAME, this.dag.getName());
            config.setBytes(SVB, Serialization.serializeObject(svb));

            //We add an empty batched data set to emit the updated prior.
            DataOnMemory<DataInstance> emtpyBatch = new DataOnMemoryListContainer<DataInstance>(dataUpdate.getAttributes());
            DataSet<DataOnMemory<DataInstance>> unionData = null;

            unionData =
                    dataUpdate.getBatchedDataSet(this.batchSize)
                            .union(env.fromCollection(Arrays.asList(emtpyBatch),
                                    TypeExtractor.getForClass((Class<DataOnMemory<DataInstance>>) Class.forName("eu.amidst.core.datastream.DataOnMemory"))));

            DataSet<CompoundVector> newparamSet =
                    unionData
                    .map(new ParallelVBMap(randomStart, idenitifableModelling))
                    .withParameters(config)
                    .withBroadcastSet(loop, "VB_PARAMS_" + this.dag.getName())
                    .reduce(new ParallelVBReduce());

            // feed new centroids back into next iteration
            DataSet<CompoundVector> finlparamSet = loop.closeWith(newparamSet);

            parameterPrior = finlparamSet.collect().get(0);

            this.svb.updateNaturalParameterPosteriors(parameterPrior);

            this.svb.updateNaturalParameterPrior(parameterPrior);

            if(timeLimit == -1)
                this.globalELBO = ((ConvergenceELBO)loop.getAggregators().getConvergenceCriterion()).getELBO();
            else
                this.globalELBO = ((ConvergenceELBObyTime)loop.getAggregators().getConvergenceCriterion()).getELBO();

            this.svb.applyTransition();

        }catch(Exception ex){
            throw new RuntimeException(ex.getMessage());
        }

        this.randomStart=false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void runLearning() {
        this.initLearning();
        this.updateModel(this.dataFlink);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDAG(DAG dag_) {
        this.dag = dag_;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSeed(int seed) {
        this.svb.setSeed(seed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BayesianNetwork getLearntBayesianNetwork() {
        return this.svb.getLearntBayesianNetwork();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOutput(boolean activateOutput) {
        this.svb.setOutput(activateOutput);
    }


    public <E extends UnivariateDistribution> E getParameterPosterior(Variable parameter) {
            return this.svb.getParameterPosterior(parameter);
    }





    public static class ParallelVBMap extends RichMapFunction<DataOnMemory<DataInstance>, CompoundVector> {

        DoubleSumAggregator elbo;

        double basedELBO = -Double.MAX_VALUE;

        SVB svb;

        CompoundVector prior;

        CompoundVector initialPosterior;

        CompoundVector updatedPrior;


        String bnName;

        Map<Double,CompoundVector> partialVectors;

        IdenitifableModelling idenitifableModelling;

        boolean randomStart;

        public ParallelVBMap(boolean randomStart, IdenitifableModelling idenitifableModelling) {
            this.randomStart = randomStart;
            this.idenitifableModelling = idenitifableModelling;
        }


        @Override
        public CompoundVector map(DataOnMemory<DataInstance> dataBatch) throws Exception {
            int superstep = getIterationRuntimeContext().getSuperstepNumber() - 1;

            if (dataBatch.getNumberOfDataInstances()==0){
                elbo.aggregate(basedELBO);
                return prior;//this.svb.getNaturalParameterPrior();
            }else {
                //Compute ELBO
                this.svb.updateNaturalParameterPrior(updatedPrior);
                this.svb.updateNaturalParameterPosteriors(updatedPrior);
                svb.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(false));
                SVB.BatchOutput outElbo = svb.updateModelOnBatchParallel(dataBatch);


                if (Double.isNaN(outElbo.getElbo()))
                    throw new IllegalStateException("NaN elbo");

                elbo.aggregate(outElbo.getElbo());


                //elbo.aggregate(svb.getPlateuStructure().getReplicatedNodes().filter(node-> node.isActive()).mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum());


                //Set Active Parameters
                svb.getPlateuStructure()
                        .getNonReplictedNodes()
                        .filter(node -> this.idenitifableModelling.isActiveAtEpoch(node.getMainVariable(), superstep))
                        .forEach(node -> node.setActive(true));


                if (partialVectors.containsKey(dataBatch.getBatchID())) {
                    CompoundVector newVector = Serialization.deepCopy(updatedPrior);
                    newVector.substract(partialVectors.get(dataBatch.getBatchID()));
                    this.svb.updateNaturalParameterPrior(newVector);
                    this.svb.updateNaturalParameterPosteriors(updatedPrior);
                }else {
                    this.svb.updateNaturalParameterPrior(this.prior);
                    this.svb.updateNaturalParameterPosteriors(this.initialPosterior);
                }

                //System.out.println("PRIOR");
                //System.out.println(svb.getLearntBayesianNetwork());

                SVB.BatchOutput out = svb.updateModelOnBatchParallel(dataBatch);



                partialVectors.put(dataBatch.getBatchID(),out.getVector());

                //this.svb.updateNaturalParameterPrior(this.svb.getPlateuStructure().getPlateauNaturalParameterPosterior());
                //System.out.println("POSTERIOR");
                //System.out.println(svb.getLearntBayesianNetwork());

                //System.out.println(out.getVector().getVectorByPosition(0).get(0));

                svb.getPlateuStructure().getNonReplictedNodes().forEach(node -> node.setActive(true));

                return out.getVector();
            }

        }


        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            bnName = parameters.getString(BN_NAME, "");
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));
            svb.initLearning();

            Collection<CompoundVector> collection = getRuntimeContext().getBroadcastVariable("VB_PARAMS_" + bnName);
            updatedPrior = collection.iterator().next();


            if (prior!=null) {
                svb.updateNaturalParameterPrior(prior);
                svb.updateNaturalParameterPosteriors(updatedPrior);
                basedELBO = svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();
                //svb.initLearning();
                //System.out.println("BaseELBO:"+ basedELBO);
            }else{
                this.prior=Serialization.deepCopy(updatedPrior);
                this.svb.updateNaturalParameterPrior(prior);
                if (randomStart) {
                    this.svb.getPlateuStructure().setSeed(this.svb.getSeed());
                    this.svb.getPlateuStructure().resetQs();
                    initialPosterior = Serialization.deepCopy(this.svb.getPlateuStructure().getPlateauNaturalParameterPosterior());
                    initialPosterior.sum(prior);
                }else{
                    initialPosterior=Serialization.deepCopy(svb.getNaturalParameterPrior());
                }

                this.svb.updateNaturalParameterPosteriors(initialPosterior);

                basedELBO = svb.getPlateuStructure().getNonReplictedNodes().mapToDouble(node -> svb.getPlateuStructure().getVMP().computeELBO(node)).sum();

            }

            svb.updateNaturalParameterPrior(updatedPrior);

            elbo = getIterationRuntimeContext().getIterationAggregator("ELBO_"+bnName);


            if (partialVectors==null){
                partialVectors = new HashMap();
            }

        }
    }

    public static class ParallelVBMapInferenceAssignment extends RichFlatMapFunction<DataOnMemory<DataInstance>, DataPosteriorAssignment> {

        List<Variable> latentVariables;
        SVB svb;

        @Override
        public void flatMap(DataOnMemory<DataInstance> dataBatch, Collector<DataPosteriorAssignment> out) {
            for (DataPosteriorAssignment posterior: svb.computePosteriorAssignment(dataBatch, latentVariables)){
                out.collect(posterior);
            }
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));
            svb.initLearning();
            latentVariables = Serialization.deserializeObject(parameters.getBytes(LATENT_VARS, null));
        }
    }

    public static class ParallelVBMapInference extends RichFlatMapFunction<DataOnMemory<DataInstance>, DataPosterior> {

        List<Variable> latentVariables;
        SVB svb;

        @Override
        public void flatMap(DataOnMemory<DataInstance> dataBatch, Collector<DataPosterior> out) {
            if (latentVariables==null){
                for (DataPosterior posterior: svb.computePosterior(dataBatch)){
                    out.collect(posterior);
                }

            }else {
                for (DataPosterior posterior: svb.computePosterior(dataBatch, latentVariables)){
                    out.collect(posterior);
                }
            }
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            svb = Serialization.deserializeObject(parameters.getBytes(SVB, null));
            svb.initLearning();
            latentVariables = Serialization.deserializeObject(parameters.getBytes(LATENT_VARS, null));
        }
    }

    public static class ParallelVBReduce extends RichReduceFunction<CompoundVector> {
        @Override
        public CompoundVector reduce(CompoundVector value1, CompoundVector value2) throws Exception {
/*            value2.sum(value1);
            return value2;
*/

            CompoundVector newValue  = Serialization.deepCopy(value1);
            newValue.sum(value2);
            return newValue;
        }
    }


    public static class ConvergenceELBO implements ConvergenceCriterion<DoubleValue>{

        final double threshold;
        double previousELBO = Double.NaN;
        long start;

        public ConvergenceELBO(double threshold, long start){
            this.threshold=threshold;
            this.start = start;
        }

        public double getELBO() {
            return previousELBO;
        }

        @Override
        public boolean isConverged(int iteration, DoubleValue value) {


            if (iteration==1)
                return false;

            iteration--;

            if (Double.isNaN(value.getValue()))
                throw new IllegalStateException("A NaN elbo");

            if (value.getValue()==Double.NEGATIVE_INFINITY)
                value.setValue(-Double.MAX_VALUE);

            double percentage = 100*(value.getValue() - previousELBO)/Math.abs(previousELBO);

            DecimalFormat df = new DecimalFormat("0.0000");

            if (iteration==1) {
                previousELBO=value.getValue();
                logger.info("Global bound at first iteration: 1,{},{} seconds",df.format(value.getValue()),
                        df.format((System.nanoTime() - start) / 1000000000.0));
                //System.out.println("Global bound at first iteration: 1," + df.format(value.getValue())+ "," +
                //        df.format((System.nanoTime() - start) / 1000000000.0) + " seconds");

                return false;
            }else if (percentage<0 && percentage < -threshold){
                logger.info("Global bound is not monotonically increasing: {},{},{}<{}",iteration, df.format(
                        percentage), df.format(value.getValue()), df.format(previousELBO));
                throw new IllegalStateException("Global bound is not monotonically increasing: "+ iteration +","+
                        df.format(percentage) +"," + df.format(value.getValue()) +" < " + df.format(previousELBO));
                //System.out.println("Global bound is not monotonically increasing: "+ iteration +", "+ percentage +
                // ", "+ (value.getValue() +">" + previousELBO));
                //this.previousELBO=value.getValue();
                //return false;
            }else if (percentage>0 && percentage>threshold) {
                logger.info("Global bound is monotonically increasing: {},{},{}>{},{} seconds",iteration,
                        df.format(percentage), df.format(value.getValue()), df.format(previousELBO),
                        df.format((System.nanoTime() - start) / 1000000000.0));
                //System.out.println("Global bound is monotonically increasing: "+ iteration +","+df.format(percentage)+
                //        "," + (df.format(value.getValue()) +">" + df.format(previousELBO))+ ","+
                //        df.format((System.nanoTime() - start) / 1000000000.0) + " seconds");
                this.previousELBO=value.getValue();
                return false;
            }else {
                logger.info("Global bound Convergence: {},{},{},{} seconds",iteration,df.format(percentage),
                        df.format(value.getValue()), df.format((System.nanoTime() - start) / 1000000000.0));
                //System.out.println("Global bound Convergence: "+ iteration +"," + df.format(percentage) + "," +
                //        df.format(value.getValue())+ "," + df.format((System.nanoTime() - start) / 1000000000.0) +
                //        " seconds");
                return true;
            }
        }
    }

    public static class ConvergenceELBObyTime implements ConvergenceCriterion<DoubleValue>{

        double previousELBO = Double.NaN;
        final double timeLimit;
        long start;

        public ConvergenceELBObyTime(double timeLimit, long start){
            this.start = start;
            this.timeLimit = timeLimit;
        }

        public double getELBO() {
            return previousELBO;
        }

        @Override
        public boolean isConverged(int iteration, DoubleValue value) {


            if (iteration==1)
                return false;

            iteration--;

            if (Double.isNaN(value.getValue()))
                throw new IllegalStateException("A NaN elbo");

            if (value.getValue()==Double.NEGATIVE_INFINITY)
                value.setValue(-Double.MAX_VALUE);

            double percentage = 100*(value.getValue() - previousELBO)/Math.abs(previousELBO);

            double timeIteration = (System.nanoTime() - start) / 1000000000.0;

            DecimalFormat df = new DecimalFormat("0.0000");

            if (iteration==1) {
                previousELBO=value.getValue();
                logger.info("Global bound at first iteration: 1,{},{} seconds",df.format(value.getValue()),
                        df.format((System.nanoTime() - start) / 1000000000.0));
                return false;
            }else if (percentage<-1){
                logger.info("Global bound is not monotonically increasing: {},{},{}<{}",iteration, df.format(
                        percentage), df.format(value.getValue()), df.format(previousELBO));
                throw new IllegalStateException("Global bound is not monotonically increasing: "+ iteration +","+
                        df.format(percentage) +"," + df.format(value.getValue()) +" < " + df.format(previousELBO));
            }else if (percentage>-1 && timeIteration < timeLimit) {
                logger.info("Global bound is monotonically increasing: {},{},{}>{},{} seconds",iteration,
                        df.format(percentage), df.format(value.getValue()), df.format(previousELBO),
                        df.format((System.nanoTime() - start) / 1000000000.0));
                this.previousELBO=value.getValue();
                return false;
            }else {
                logger.info("Global bound Convergence: {},{},{},{} seconds",iteration,df.format(percentage),
                        df.format(value.getValue()), df.format((System.nanoTime() - start) / 1000000000.0));
                return true;
            }
        }
    }
}