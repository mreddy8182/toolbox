package eu.amidst.core.distribution.exponentialfamily;


import eu.amidst.core.database.statics.DataInstance;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.potential.Potential;

/**
 * Created by afa on 03/07/14.
 */
public class GM_ConditionalDistribution implements ConditionalDistribution {
    private GaussianDistribution[] estim;

    @Override
    public Potential getRestrictedPotential(DataInstance instance) {
        return null;
    }

    @Override
    public double[] getSufficientStatistics(DataInstance instance) {
        return new double[0];
    }

    @Override
    public double[] getExpectedSufficientStatistics(DataInstance instance, Potential pot) {
        return new double[0];
    }

    @Override
    public void setExpectationParameters(double[] ss) {

    }

    @Override
    public double[] getExpectationParameters() {
        return new double[0];
    }

    @Override
    public double getProbability(DataInstance data) {
        return 0;
    }
}