package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

public class Membership {

    private List<Member> members;
    private ReentrantLock lock;
    public static boolean LEADER;
    public static boolean CANDIDATE;
    public static int SELF_PORT;
    public static String SELF_HOST;
    public static String LEADER_HOST;
    public static int LEADER_PORT;
    //public static volatile int ID_COUNT;
    public static boolean IN_ELECTION;
    public static String VOTED_FOR;
    //public static int VOTES_RECEIVED;
    public static int MAJORITY;
    //public static boolean ELECTION_REPLY;
    private static ExecutorService notificationThreadPool = Executors.newFixedThreadPool(6);
    public static ExecutorService replicationThreadPool = Executors.newFixedThreadPool(6);
    protected static final LogData log = LogData.getInstance();
    public static Timer ELECTION_TIMER = new Timer("Timer");
    public static long ELECTION_DELAY  = 15000L;
    public static long ELECTION_PERIOD  = 15000L;
    final static Logger logger = Logger.getLogger(Membership.class);

    /** Makes sure only one Membership is instantiated. */
    private static Membership singleton = new Membership();

    /** Constructor */
    private Membership() {
        members = Collections.synchronizedList(new ArrayList<Member>());
        lock = new ReentrantLock();
    }

    /** Makes sure only one Membership is instantiated. Returns the Singleton */
    public static Membership getInstance(){
        return singleton;
    }

    public void loadSelfConfiguration(Properties config){
        SELF_PORT = Integer.parseInt(config.getProperty("selfport"));
        SELF_HOST = config.getProperty("selfhost");
    }

    /**
     * It parses the properties file with configuration information
     * and load the data of the configuration of the initial nodes
     * @param config property object to parse
     * */
    public void loadInitMembers(Properties config) {
        LEADER_HOST = "http://" + config.getProperty("leaderhost");
        LEADER_PORT = Integer.parseInt(config.getProperty("leaderport"));
        IN_ELECTION = false;
        CANDIDATE = false;
        VOTED_FOR = "none";

//        JSONObject initEntry = new JSONObject();
//        initEntry.put("init",0);
//        LogEntry init = new LogEntry(0, new Entry(initEntry));
//        log.addInitLogEntry(init);

        String leaderStatus =  config.getProperty("leader");
        if (leaderStatus.equalsIgnoreCase("on")) {
            logger.debug(System.lineSeparator() + "Leader Started");
            LEADER = true;
            MAJORITY = 0;
            LogData.TERM = 1;
            Member leader = new Member(config.getProperty("leaderhost"), config.getProperty("leaderport"), true, 0);
            members.add(leader);
            printMemberList();

            JSONObject initEntry = new JSONObject();
            initEntry.put("init",0);
            LogEntry init = new LogEntry(0, new Entry(initEntry));
            log.addInitLogEntry(init);
            LogData.COMMIT_INDEX = 0;
            //initSendingReplicaChannel();

            Timer timer = new Timer("Timer");
            long delay  = 10000L;
            long period = 10000L;
            timer.scheduleAtFixedRate(new HeartBeatTimeTask(), delay, period);

        } else {
            logger.debug(System.lineSeparator() + "Raft Instance Started at " + SELF_HOST + ":" + SELF_PORT );
            LEADER = false;
            LogData.COMMIT_INDEX = 0;
            ArrayList<Member> membersFromLeader = register();
            members.addAll(membersFromLeader);
            printMemberList();
            logger.debug("Members received");
            LogData.INDEX = -1;
            log.loadRaftLogBackup();
            replicationThreadPool.submit(RPCServlet.receiverWorker);
            //Timer election_timer = new Timer("Timer");
            //long election_delay  = 12000L;
            //long election_period = 20000L;
            ELECTION_TIMER.scheduleAtFixedRate(new ElectionTimerTask(), ELECTION_DELAY, ELECTION_PERIOD);
        }
    }

    /**
     * Sends the petition of registering with the leader and get its
     * list of members
     * @return ArrayList of members
     * */
    private ArrayList<Member> register() {
        ArrayList<Member> members = new ArrayList<>();
        try {
            HttpURLConnection conn = registerWithLeader();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    String jsonResponse = getResponseBody(conn);
                    members = parseMembers(jsonResponse);
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    logger.debug("400: New raft instance could not be registered");
                    break;
                default:
                    logger.debug("Status Code Received Unknown when registering new raft instance");
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Sends the request of registering with the leader and returns the corresponding
     * established connection
     * @return HttpURLConnection
     * */
    private HttpURLConnection registerWithLeader() {
        String host = LEADER_HOST + ":" + String.valueOf(LEADER_PORT);
        String path = "/members/register";
        String url = host + path;
        HttpURLConnection conn = null;
        try {
            URL urlObj = new URL(url);
            conn  = (HttpURLConnection) urlObj.openConnection();
            setPostRequestProperties(conn);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            JSONObject newInstanceConfig = createJsonWithOwnConfig();
            //System.out.println(newInstanceConfig.toString());
            out.write(newInstanceConfig.toString());
            out.flush();
            out.close();
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
            return conn;
        }
    }

    /**
     * Creates a json object with the configuration of the running server
     * @return JSONObject
     * */
    private JSONObject createJsonWithOwnConfig() {
        JSONObject json = new JSONObject();
        json.put("host", SELF_HOST);
        json.put("port", SELF_PORT);
        return json;
    }
    /**
     * Sets post reuqest properties in a given HttpURLConnection
     * @param conn HttpURLConnection
     * */
    private void setPostRequestProperties(HttpURLConnection conn){
        try {
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the jason of the body of the response and converted into string
     * @param conn http request
     * @return json received in the request converted into a string
     * */
    private String getResponseBody(HttpURLConnection conn) throws IOException {
        BufferedReader in;
        String line, body;
        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer sb = new StringBuffer();
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
        }
        body = sb.toString();
        in.close();
        return body;
    }

    /**
     * Converts a string with the format of a json into a list of members
     * @param JsonOfMembersReceived string with json format
     * @return Array list of members
     * */
    private ArrayList<Member> parseMembers(String JsonOfMembersReceived) {
        ArrayList<Member> members = new ArrayList<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject jsonOfMembers = (JSONObject) parser.parse(JsonOfMembersReceived);
            JSONArray arrayOfMembers = (JSONArray) jsonOfMembers.get("members");
            Iterator<JSONObject> iterator = arrayOfMembers.iterator();
            while (iterator.hasNext()) {
                JSONObject obj = iterator.next();
                Member member = Member.fromJsonToMemberObj(obj);
                members.add(member);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Registers the new raft instance. It adds it to the member list and respond back
     * with the list of the current members
     * @param request http request
     * @param response http request
     * */
    public void registerServer(HttpServletRequest request, HttpServletResponse response){
        try {
            String requestBody = getRequestBody(request);
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(requestBody);
            String host = (String)jsonObj.get("host");
            int port = ((Long)jsonObj.get("port")).intValue();
            logger.debug(System.lineSeparator() + "New " + host + ":" + port);
            Member member = new Member(host, String.valueOf(port), false);
            notifyOtherServers(member);
            if (!isRegistered(member)){
                members.add(member);
                setMajority();
                updateChannel(member);
            }
            sendMyListOfMembers(response);
            logger.debug("Members sent");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException p) {
            p.printStackTrace();
        }
    }

    private boolean isRegistered(Member member){
        boolean present = false;
        synchronized (members) {
            for (Member server : members) {
                if (server.getPort().equals(member.getPort()) && server.getHost().equals(member.getHost())) {
                    present = true;
                }
            }
        }
        return present;
    }

    /**
     * It updates the Channel of sending replication information adding the
     * given member
     * @param m Member object
     */
    public void updateChannel(Member m) {
        String hostAndPort = "http://" + m.getHost() + ":" + m.getPort();
        //System.out.println("Updating channel: " + hostAndPort);
        SendingReplicaWorker worker = new SendingReplicaWorker(hostAndPort);
        replicationThreadPool.submit(worker);
        RPCServlet.registerInChannel(worker);
    }

    /**
     * Gets the jason of the body of the request and converted into string
     * @param request http request
     * @return json received in the request converted into a string
     * */
    private String getRequestBody(HttpServletRequest request) throws IOException {
        BufferedReader in;
        String line, body;
        in = new BufferedReader(new InputStreamReader(request.getInputStream()));
        StringBuffer sb = new StringBuffer();
        while ((line = in.readLine()) != null) {
            sb.append(line);
            sb.append(System.lineSeparator());
        }
        body = sb.toString();
        in.close();
        return body;
    }

    /**
     * Attaches and sends in the response the list of current server members
     * @param response Http Response
     * */
    private void sendMyListOfMembers(HttpServletResponse response){
        try {
            JSONObject membersList = createJSONOfMembers();
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write(membersList.toString());
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a json object of all the current members
     * @return JSON Object of members
     * */
    private JSONObject createJSONOfMembers(){
        JSONObject obj = new JSONObject();
        JSONArray array = new JSONArray();
        synchronized (members) {
            for (Member m : members) {
                JSONObject subObj = new JSONObject();
                subObj.put("host", m.getHost());
                subObj.put("port", m.getPort());
                String isLeader;
                if (m.getIsLeader()) isLeader = "true";
                else isLeader = "false";
                subObj.put("isLeader", isLeader);
                array.add(subObj);
            }
        }
        obj.put("members", array);
        return obj;
    }

    /**
     * Notifies the servers that a new raft instance was added
     * @param newMember Member object
     * */
    private void notifyOtherServers(Member newMember){
        synchronized (members) {
            for (Member server : members) {
                if ((!server.getIsLeader())) {
                    String url = "http://" + server.getHost() + ":" + server.getPort() + "/members/add";
                    notificationThreadPool.submit(new NotificationWorker(url, newMember.generateJson().toString()));
                }
            }
        }
    }

    /**
     * Once a notification of new raft instance added is received, it adds the node to the list
     * of members
     * @param request Http Request
     * @param response Http Response
     * */
    public void addNotifiedServer(HttpServletRequest request, HttpServletResponse response) {
        lock.lock();
        try {
            String requestBody = getRequestBody(request);
            JSONParser parser = new JSONParser();
            JSONObject jsonObj = (JSONObject) parser.parse(requestBody);
            Member member = Member.fromJsonToMemberObj(jsonObj);
            if (!isRegistered(member)){
                members.add(member);
            }
            logger.debug("Notified " + member.getHost()+":"+ member.getPort());
            printMemberList();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (IOException e){
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public int getMajority(){
        synchronized (members) {
            int followers = 0;
            int half = 0 ;
            for (Member m : members) {
                if(!m.getIsLeader()) {
                    followers ++;
                }
            }
            if (followers%2 == 0){
                half = followers/2;
            } else {
                half = (followers/2) + 1;
            }
            return half;
        }
    }

    public void setMajority(){
        synchronized (members) {
            int followers = 0;
            int half = 0 ;
            for (Member m : members) {
                if(!m.getIsLeader()) {
                    followers ++;
                }
            }
            if (followers%2 == 0){
                half = followers/2;
            } else {
                half = (followers/2) + 1;
            }
            MAJORITY = half;
        }
    }

    /**
     * Method that generates a list of the current members in the architecture
     * @return ArrayList of members
     */
    public ArrayList<Member> getMembers(){
        synchronized (members) {
            ArrayList<Member> list = new ArrayList<>();
            for (Member m : members) {
                list.add(m);
            }
            return list;
        }
    }

    public void election(){
        logger.debug("In election");
        IN_ELECTION = true;
        LogData.TERM ++;
        CANDIDATE = true;
        VOTED_FOR = SELF_HOST+":"+SELF_PORT;
        resetElectionTimer();
        //VOTES_RECEIVED ++;
        sendRequestVotesRPCS(buildRequestVoteBody());

    }

    private void sendRequestVotesRPCS(JSONObject requestVoteBody){
        logger.debug("Sending request votes");
        synchronized (members) {
            final CountDownLatch latch = new CountDownLatch(Membership.MAJORITY);
            for (Member server : members) {
                if ((!server.getHost().equals(SELF_HOST)) || (Integer.parseInt(server.getPort())!=SELF_PORT)) {
                    logger.debug("Sending to " + server.getHost() +":"+ server.getPort());
                    String url = "http://" + server.getHost() + ":" + server.getPort() + "/requestvote/vote";
                    notificationThreadPool.submit(new RequestVoteRPCWorker(url, requestVoteBody.toString(),latch));
                }
            }
            try {
                latch.await(10, TimeUnit.SECONDS);
                //logger.debug("Latch woke up + " + latch.getCount());
                if (latch.getCount() == 0){
                    logger.debug("Votes of majority received");
                    ELECTION_TIMER.cancel();
                    LEADER = true;
                    CANDIDATE = false;
                    IN_ELECTION = false;
                    VOTED_FOR = "none";
                    updateLeaderInMembership();
                    Timer timer = new Timer("Timer");
                    long delay  = 10000L;
                    long period = 10000L;
                    timer.scheduleAtFixedRate(new HeartBeatTimeTask(), delay, period);
                } else {
                    logger.debug("Not enough votes received");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateLeaderInMembership() {
        synchronized (members) {
            for (Member m : members) {
                if (m.getIsLeader()) m.setIsLeader(false);
                if ((Integer.parseInt(m.getPort())==SELF_PORT) && m.getHost().equals(SELF_HOST)) m.setIsLeader(true);
            }
        }
    }

    private JSONObject buildRequestVoteBody(){
        logger.debug("Building request vote body");
        JSONObject json = new JSONObject();
        json.put("term", LogData.TERM);
        json.put("candidateid", SELF_HOST+":"+SELF_PORT);
        json.put("lastlogindex", log.getLastLogIndex());
        json.put("lastlogterm", log.getLastLogEntryTerm());
        System.out.println(json.toString());
        return json;
    }

    public void resetElectionTimer(){
        ELECTION_TIMER.cancel();
        ELECTION_TIMER = new Timer("Timer");
        ELECTION_TIMER.scheduleAtFixedRate(new ElectionTimerTask(), ELECTION_DELAY, ELECTION_PERIOD);
    }

    /**
     * Method that prints on the console the content
     * of the data structure of members
     */
    public void printMemberList() {
        StringBuilder sb = new StringBuilder();
        sb.append("Members List:");
        sb.append(System.lineSeparator());
        synchronized (members) {
            for (Member m : members) {
                sb.append(m);
            }
        }
        logger.debug(sb.toString());
    }

}
