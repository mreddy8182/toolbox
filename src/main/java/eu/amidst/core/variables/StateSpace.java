package eu.amidst.core.variables;

/**
 * Created by andresmasegosa on 25/11/14.
 */
public abstract class StateSpace {

    private StateSpaceType stateSpaceType;
    private String unit="NA";

    public StateSpace(StateSpaceType type){
        this.stateSpaceType=type;
    }

    public StateSpaceType getStateSpaceType(){
        return this.stateSpaceType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
