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
import java.util.Timer;
import java.util.concurrent.CountDownLatch;


public class RPCServlet extends HttpServlet {
    protected static final LogData log = LogData.getInstance();
    protected static final Membership membership = Membership.getInstance();
    private static List<SendingReplicaWorker> sendingReplicaChannel = new ArrayList<>();
    public static final ReceivingAppendEntryWorker receiverWorker = new ReceivingAppendEntryWorker();
    final static Logger logger = Logger.getLogger(RPCServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        System.out.println("In do get");
//        if (pathInfo.equals("/entry")) {
//            System.out.println("Heartbeat received");
//            membership.resetElectionTimer();
//        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        String jsonContent = getRequestBody(request);
        JSONObject json = to_jsonObject(jsonContent);
        System.out.println("in do post " + pathInfo );
        if (pathInfo.equals("/entry")) {
            if (!json.containsKey("entry")){
                System.out.println("Heartbeat received");
                membership.resetElectionTimer();
            } else {
                System.out.println("contains entry "  );
                if(Membership.LEADER){
                    response.setStatus(HttpServletResponse.SC_OK);  /** The leader will get back to the app once the entry is committed */
                    processAppAppendEntry(json);
                } else {
                    processLeadersAppendEntry(jsonContent, response); /** Follower parses the jason and do the consistency check */
                }
            }
        } else if(pathInfo.equals("/vote")) {
            System.out.println("Request of Vote Received");
            if (Membership.VOTED_FOR.equals("none")){
                processRequestVote(json, response);
            } else {
                logger.debug("Vote not granted. Server already voted for " + Membership.VOTED_FOR);
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
            //sendCommittedEntrytoClient();
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

    private void processRequestVote(JSONObject json, HttpServletResponse response){
        boolean myLogIsAhead = false;
        int term = ((Long)json.get("term")).intValue();
        String cadidateId = (String)json.get("candidateId");
        int candidateLastLogIndex = ((Long)json.get("lastlogindex")).intValue();
        int candidateLastLogTerm = ((Long)json.get("lastlogterm")).intValue();

        myLogIsAhead = isMyLogMoreUpToDate(candidateLastLogIndex, candidateLastLogTerm);
        if (!myLogIsAhead) {
            /** An status code OK it is replied when the vote is granted.*/
            response.setStatus(HttpServletResponse.SC_OK);
            Membership.VOTED_FOR = cadidateId;
        } else {
            /** A Bad Request is sent when the vote can not be granted. The term is not returning because according
             * to my implementation once the leader starts receiving append entries that are ahead he will be able
             * to reply that that append entry is not the expected and he will be able to catch up with the leader's log*/
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private boolean isMyLogMoreUpToDate(int candidateLastLogIndex, int candidateLastLogTerm) {
        boolean myLogIsMoreUpToDate = false;
        int myLastTerm = log.getLastLogEntryTerm();
        int myLastIndex = log.getLastLogIndex();

        if (myLastTerm != candidateLastLogTerm){
            if (myLastTerm > candidateLastLogTerm) myLogIsMoreUpToDate = true;
        } else {
            if (myLastIndex > candidateLastLogIndex) myLogIsMoreUpToDate = true;
        }
        return myLogIsMoreUpToDate;
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

