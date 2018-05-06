package cs682;

import org.json.simple.JSONObject;

public class Member {
    private String host;
    private String port;
    private boolean isLeader;
    private int nextIndex;


    public Member( String host, String port, boolean isLeader, int nextIndex){
        this.host = host;
        this.port = port;
        this.isLeader = isLeader;
        this.nextIndex = nextIndex;
    }

    public Member( String host, String port, boolean isLeader){
        this.host = host;
        this.port = port;
        this.isLeader = isLeader;
        this.nextIndex = 0;
    }

    public String getHost(){
        return this.host;
    }
    public String getPort(){
        return this.port;
    }
    public boolean getIsLeader(){
        return this.isLeader;
    }
    public int getNextIndex(){
        return this.nextIndex;
    }

    public void setHost(String host){
        this.host = host;
    }
    public void setPort(String port){
        this.port = port;
    }
    public void setIsLeader(boolean isLeader){
        this.isLeader = isLeader;
    }
    public void setNextIndex(int nextIndex){ this.nextIndex = nextIndex; }

    /**
     * Creates a json representation of a member object
     * @return JSON Obj representation of the member
     */
    public JSONObject generateJson() {
        JSONObject obj = new JSONObject();
        obj.put("host", this.host);
        obj.put("port", this.port);
        obj.put("isLeader", this.isLeader);
        obj.put("nextIndex", this.nextIndex);
        return obj;
    }

    /**
     * Shows the String representation of a member object
     * @return string representation of the member
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append(host).append(":").append(port).append(", ");
        sb.append("Leader: ").append(isLeader).append(", ");
        sb.append("Next Index:  ").append(nextIndex).append("]").append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Converts the json representation of a member into a member object
     * @param json json object that contains data of a member
     * @return member object
     */
    public static Member fromJsonToMemberObj(JSONObject json){
        boolean leader;
        if (json.get("isLeader").equals("true")) leader = true;
        else leader = false;
        Member member = new Member((String)json.get("host"), (String)json.get("port"),leader);
        return member;
    }
}
