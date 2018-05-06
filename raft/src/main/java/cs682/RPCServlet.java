package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class RPCServlet extends HttpServlet {
    protected static final LogData log = LogData.getInstance();
    final static Logger logger = Logger.getLogger(RPCServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/")) {
            //listAllEvents(response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/entry")) {
            String jsonContent = getRequestBody(request);
            if(Membership.LEADER){
                Entry entry = new Entry(jsonContent);
                LogEntry logentry = new LogEntry(LogData.TERM,entry);
                log.add(logentry);
                //boolean replicationSuccess = replicateEntry(logentry);
                //if (replicationSuccess) {
                    //send commit to the app .. respond sending the commit
                //}
                //send append entries to all the followers
                //should I change the end point ? send the term in a jason or put it in the path
                //send in the json the log entry
            } else {
                //follower part decompose the jason take the term and do the sanity check
            }

        }
    }

    /**
     * Gets the jason of the body of the request and converted into string
     * @param request http request
     * @return json received in the request converted into a string
     * */
    private String getRequestBody(HttpServletRequest request) {
        BufferedReader in;
        String line;
        String body = null;
        try {
            in = new BufferedReader(new InputStreamReader(request.getInputStream()));
            StringBuffer sb = new StringBuffer();
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            body = sb.toString();
            in.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
        return body;
    }

}

