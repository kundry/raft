package cs682;


import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;


public class RPCServlet extends HttpServlet {
    protected static final LogData log = LogData.getInstance();
    //protected static final Membership membership = Membership.getInstance();
    private static List<SendingReplicaWorker> sendingReplicaChannel = new ArrayList<>();
    public static final ReceivingAppendEntryWorker receiverWorker = new ReceivingAppendEntryWorker();
    final static Logger logger = Logger.getLogger(RPCServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/entry")) {
            System.out.println("Heartbeat received");
            Membership.ELECTION_TIMER.cancel();
            Membership.ELECTION_TIMER = new Timer("Timer");
            Membership.ELECTION_TIMER.scheduleAtFixedRate(new ElectionTimerTask(), Membership.ELECTION_DELAY, Membership.ELECTION_PERIOD);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/entry")) {
            String jsonContent = getRequestBody(request);
            JSONObject json = to_jsonObject(jsonContent);
            if(Membership.LEADER){
                response.setStatus(HttpServletResponse.SC_OK);  /** The leader will get back to the app once the entry is committed */
                processAppAppendEntry(json);
            } else {
                processLeadersAppendEntry(jsonContent, response); /** Follower parses the jason and do the consistency check */
            }
        }
    }

    private void processAppAppendEntry(JSONObject json){
        Entry entry = new Entry(json);
        logger.debug(System.lineSeparator() + "appendentry/entry ");
        logger.debug(entry.getOperationData().toString());
        LogEntry logentry = new LogEntry(LogData.TERM,entry);
        log.add(logentry);
        boolean replicationSuccess = replicateEntry(logentry);
        if (replicationSuccess) {
            //send commit to the app .. respond sending the commit
            System.out.println("Entry can be committed...");
        }
        //send append entries to all the followers
        //should I change the end point ? send the term in a jason or put it in the path
        //send in the json the log entry
    }


    private void processLeadersAppendEntry(String jsonContent, HttpServletResponse response){
        ReplicatedLogEntry repLogEntry = new ReplicatedLogEntry(jsonContent, response);
        receiverWorker.queueReplicatedLogEntry(repLogEntry);
    }

    private JSONObject to_jsonObject(String stringData){
        JSONObject json = new JSONObject();
        try {
            JSONParser parser = new JSONParser();
            json = (JSONObject) parser.parse(stringData);
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            return json;
        }
    }

    private boolean replicateEntry(LogEntry logentry) {
        boolean success = false;
        try {
            logger.debug("Replication started");
            //logger.debug("Majority = " + Membership.MAJORITY);
            final CountDownLatch latch = new CountDownLatch(Membership.MAJORITY);
            logentry.setLatch(latch);
            //logger.debug(" replica channel size " + sendingReplicaChannel.size());
            for (SendingReplicaWorker follower: sendingReplicaChannel) {
                follower.queueLogEntry(logentry);
            }
            logger.debug("Replication finished");
            latch.await();
            success = true;
            logger.debug("Majority has replied");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return success;
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

    /**
     * Registers workers in the Channel of replication of entries
     * @param worker  thread that sends the data to be replicated
     * */
    public static void registerInChannel(SendingReplicaWorker worker){
        sendingReplicaChannel.add(worker);
    }
}

