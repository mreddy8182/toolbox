/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.dynamic.examples.learning;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.learning.dynamic.DynamicMaximumLikelihood;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;

import java.io.IOException;
import java.util.Random;

/**
 *
 * This example shows how to learn the parameters of a dynamic Bayesian network using maximum likelihood
 * from a sample data.
 *
 * Created by ana@cs.aau.dk on 01/12/15.
 */
public class MLforDBNsampling {

    public static void main(String[] args) throws IOException {
        Random random = new Random(1);

        //We first generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(3);
        DynamicBayesianNetwork dbnRandom = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random,2,true);

        //Sample dynamic data from the created dbn with random parameters
        DynamicBayesianNetworkSampler sampler = new DynamicBayesianNetworkSampler(dbnRandom);
        sampler.setSeed(0);
        //Sample 3 sequences of 100K instances
        DataStream<DynamicDataInstance> data = sampler.sampleToDataBase(3,10000);

        /*Parameter Learning with ML*/
        //We set the batch size which will be employed to learn the model in parallel
        DynamicMaximumLikelihood.setBatchSize(1000);
        DynamicMaximumLikelihood.setParallelMode(true);


        //We fix the DAG structure, the data and learn the DBN
        DynamicBayesianNetwork dbnLearnt = DynamicMaximumLikelihood.learnDynamic(dbnRandom.getDynamicDAG(), data);

        //We print the model
        System.out.println(dbnLearnt.toString());
    }

}
