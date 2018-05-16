package cs682;

import javax.servlet.http.HttpServletResponse;

public class ReplicatedLogEntry {
    private String LogEntryContent;
    private HttpServletResponse response;

    public ReplicatedLogEntry( String logEntryContent, HttpServletResponse response){
        this.LogEntryContent = logEntryContent;
        this.response = response;
    }

    public String getLogEntryContent(){
        return this.LogEntryContent;
    }

    public HttpServletResponse getResponse(){
        return this.response;
    }
}
