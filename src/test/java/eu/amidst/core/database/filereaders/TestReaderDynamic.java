package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attributes;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by ana@cs.aau.dk on 18/11/14.
 */
public class TestReaderDynamic {

    private static final double DELTA = 1e-15;
    private static WekaDataFileReader reader;
    private static Attributes attributes;
    private static DynamicVariables dynamicVariables;

    private static DataInstance nextInstance = null;
    private static int index;
    private static Variable var;
    private static List<Variable> obsVars;
    private static List<Variable> temporalClones;
    private static DataOnDisk dataOnDisk;
    private static Iterator<DataInstance> dataOnDiskIterator;


    public static void loadFileAndInitialize(String s) {
        reader = new WekaDataFileReader(s);
        attributes = reader.getAttributes();
        dataOnDisk = new DynamicDataOnDiskFromFile(reader);
        dataOnDiskIterator = dataOnDisk.iterator();
        dynamicVariables = new DynamicVariables(attributes);
        obsVars = dynamicVariables.getListOfDynamicVariables();
        //temporalClones = dynamicVariables.getListOfTemporalClones();
    }

    /**********************************************************
     *                    NoTimeID & NoSeq
     **********************************************************/

    @Test
    public void nOfVars_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        assertEquals(17, attributes.getList().size());
        assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void numericAttributeValue_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void reachEOF_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(89,(int)nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void checkAutomaticTimeID_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(56,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_NoTimeID_NoSeq(){
        loadFileAndInitialize("data/dataWeka/labor.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(0,(int)nextInstance.getSequenceID());
    }

    /**********************************************************
     *                       TimeID
     **********************************************************/

    @Test
    public void nOfVars_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        assertEquals(18, attributes.getList().size());
        assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicVariables.getNumberOfVars());
    }



    @Test
    public void attributeValue_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,1]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[1,2]
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(35,(int)nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[2,3]
        assertEquals(2,(int)nextInstance.getValue(dynamicVariables.getTemporalCloneByName("CONTRIBUTION-TO-HEALTH-PLAN")));
        assertEquals(1,(int)nextInstance.getValue(dynamicVariables.getVariableByName("CONTRIBUTION-TO-HEALTH-PLAN")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[3,?]
        assertEquals(1,(int)nextInstance.getValue(dynamicVariables.getTemporalCloneByName("CONTRIBUTION-TO-HEALTH-PLAN")));
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("CONTRIBUTION-TO-HEALTH-PLAN")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("CONTRIBUTION-TO-HEALTH-PLAN")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("CONTRIBUTION-TO-HEALTH-PLAN")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[?,5]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("CONTRIBUTION-TO-HEALTH-PLAN")), DELTA);
        assertEquals(4.5,nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[5,6]

    }

    @Test
    public void attributeValue_TimeID2(){
        loadFileAndInitialize("data/dataWeka/laborTimeID2.arff");

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,1]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }
        //[1,?]
        assertEquals(40,(int)nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,?]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,5]
        assertEquals(Double.NaN,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(35,nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")), DELTA);

        if(dataOnDiskIterator.hasNext()){
            nextInstance = dataOnDiskIterator.next();
        }

        //[5,6]
        assertEquals(35,nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(38,nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")), DELTA);
    }



    @Test
    public void reachEOF_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(89,(int)nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void checkAutomaticTimeID_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        for(DataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(59,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_TimeID(){
        loadFileAndInitialize("data/dataWeka/laborTimeID.arff");

        for(DataInstance instance: dataOnDisk)
        {
            nextInstance = instance;
        }

        /*Test values for the last instance*/
        assertEquals(0,(int)nextInstance.getSequenceID());
    }


    /**********************************************************
     *                        Seq
     **********************************************************/

    @Test
    public void nOfVars_seqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        assertEquals(18, attributes.getList().size());
        assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void attributeValue_seqID() {
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 1: Instances 1-4
        //[?,1]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[1,2]
        assertEquals(40, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(35, (int) nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[2,3]
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[3,4]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(3.7, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 2: Instances 5-17
        //[5,6] (Every time we change sequence we add a missing row)
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[7,8]
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(2, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
    }

    @Test
    public void reachEOF_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(89,(int)nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void checkAutomaticTimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(39,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborSeqID.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(2,(int)nextInstance.getSequenceID());
    }


    /**********************************************************
     *                    TimeID & Seq
     **********************************************************/

    @Test
    public void nOfVars_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        assertEquals(19, attributes.getList().size());
        assertEquals(17, obsVars.size());
        //assertEquals(17, temporalClones.size());
        assertEquals(17, dynamicVariables.getNumberOfVars());
    }

    @Test
    public void attributeValue_TimeID_SeqID() {
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 1: Instances 1-5
        //[?,1]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(40, (int) nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[1,2]
        assertEquals(40, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")), DELTA);
        assertEquals(35, (int) nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[2,3]
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[3,?]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[?,5]
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(3.7, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //Seq 2: Instances 1-13 (5-17)
        //[6,7] (Every time we change sequence we add a missing row)
        assertEquals(Double.NaN, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);

        if (dataOnDiskIterator.hasNext()) {
            nextInstance = dataOnDiskIterator.next();
        }

        //[7,8] (Every time we change sequence we add a missing row)
        assertEquals(4.5, nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
        assertEquals(2, nextInstance.getValue(dynamicVariables.getVariableByName("WAGE-INCREASE-FIRST-YEAR")), DELTA);
    }

    @Test
    public void reachEOF_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(88,(int)nextInstance.getValue(dynamicVariables.getTemporalCloneByName("WORKING-HOURS")));
        assertEquals(89,(int)nextInstance.getValue(dynamicVariables.getVariableByName("WORKING-HOURS")));
    }

    @Test
    public void checkAutomaticTimeID_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(39,(int)nextInstance.getTimeID());
    }

    @Test
    public void checkAutomaticSeq_TimeID_SeqID(){
        loadFileAndInitialize("data/dataWeka/laborTimeIDSeqID.arff");

        for(DataInstance instance: dataOnDisk){}

        /*Test values for the last instance*/
        assertEquals(2,(int)nextInstance.getSequenceID());
    }


}
