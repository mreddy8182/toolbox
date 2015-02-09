package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataBase;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.StaticDataInstance;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.Normal_NormalParents;
import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.EF_ConditionalDistribution;
import eu.amidst.core.learning.MaximumLikelihoodForBN;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.BayesianNetworkLoader;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Hanen on 06/02/15.
 */

public class BaseDistributionMultinomialParentsTest {

    // TODO: Consider smoothing method!!!!

    @Test
    public void testingProbabilities_MultinomialMultinomialParents() throws IOException, ClassNotFoundException  {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/asia.bn");

        //Here we had to modify the CPT of the variable E because no smoothing is considered yet
        Multinomial_MultinomialParents distE = testnet.getDistribution(testnet.getStaticVariables().getVariableByName("E"));
        //distE.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});
        //distE.getMultinomial(1).setProbabilities(new double[]{0.9, 0.1});
        //distE.getMultinomial(2).setProbabilities(new double[]{0.9, 0.1});
        //distE.getMultinomial(3).setProbabilities(new double[]{0.1, 0.9});

        System.out.println(testnet.toString());

        System.out.println("\nMultinomial_MultinomialParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataBase<StaticDataInstance> data = sampler.sampleToDataBase(10000);

        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);

        for(DataInstance e: data) {
            double ef_logProb = 0, logProb = 0;
                for (EF_ConditionalDistribution ef_dist : ef_testnet.getDistributionList()) {
                    ef_logProb += ef_dist.computeLogProbabilityOf(e);
                }

            logProb = testnet.getLogProbabiltyOfFullAssignment(e);

            //System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            assertEquals(logProb, ef_logProb, 0.0001);

        }
    }


    @Test
    public void testingProbabilities_NormalMultinomialParents() throws IOException, ClassNotFoundException  {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_MultinomialParents.bn");

        System.out.println(testnet.toString());
        System.out.println("\nNormal_MultinomialParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataBase<StaticDataInstance> data = sampler.sampleToDataBase(10000);


        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);

        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }
            logProb = testnet.getLogProbabiltyOfFullAssignment(e);
            //System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            assertEquals(logProb, ef_logProb, 0.0001);

        }
    }

    @Test
    public void testingProbabilities_NormalMultinomialNormalParents() throws IOException, ClassNotFoundException  {

        BayesianNetwork testnet = BayesianNetworkLoader.loadFromFile("networks/Normal_MultinomialNormalParents.bn");

        System.out.println(testnet.toString());

        System.out.println("\nNormal_MultinomialNormalParents probabilities comparison \n ");

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(testnet);
        sampler.setSeed(0);
        sampler.setParallelMode(true);
        DataBase<StaticDataInstance> data = sampler.sampleToDataBase(10000);


        //Compare predictions between distributions and EF distributions.

        EF_BayesianNetwork ef_testnet = new EF_BayesianNetwork(testnet);

        for(DataInstance e: data){
            double ef_logProb = 0,logProb = 0;
            for(EF_ConditionalDistribution ef_dist: ef_testnet.getDistributionList()){
                ef_logProb += ef_dist.computeLogProbabilityOf(e);
            }

            logProb = testnet.getLogProbabiltyOfFullAssignment(e);

            System.out.println("Distributions: "+ logProb + " = EF-Distributions: "+ ef_logProb);
            assertEquals(logProb, ef_logProb, 0.0001);

        }
    }

}