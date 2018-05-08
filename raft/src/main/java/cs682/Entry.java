package cs682;

import org.json.simple.JSONObject;

public class Entry {
    //private String operationData;
    private JSONObject operationData;

//    public Entry( String operationData){
//        this.operationData = operationData;
//    }

//    public void setOperationData(String operationData) {
//        this.operationData = operationData;
//    }
//    public String getOperationData(){
//        return this.operationData;
//    }


    public Entry(JSONObject operationData){
        this.operationData = operationData;
    }
    public JSONObject getOperationData(){
        return this.operationData;
    }
}
