package cs682;

import org.json.simple.JSONObject;

public class Entry {

    private JSONObject operationData;

    public Entry(JSONObject operationData){
        this.operationData = operationData;
    }
    public JSONObject getOperationData(){
        return this.operationData;
    }
}
