package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Membership {

    private List<Member> members;
    private ReentrantLock lock;
    public static boolean LEADER;
    public static int SELF_PORT;
    public static String SELF_HOST;
    public static String LEADER_HOST;
    public static int LEADER_PORT;
    //public static volatile int ID_COUNT;
    public static boolean IN_ELECTION;
    //public static boolean ELECTION_REPLY;
    //private static ExecutorService notificationThreadPool = Executors.newFixedThreadPool(6);
    //private static ExecutorService replicationThreadPool = Executors.newFixedThreadPool(6);
    protected static final LogData log = LogData.getInstance();
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
        //ELECTION_REPLY = false;
        String leaderStatus =  config.getProperty("leader");


        if (leaderStatus.equalsIgnoreCase("on")) {
            logger.debug("Leader Started");
            LEADER = true;
            LogData.TERM = 1;
            Member leader = new Member(config.getProperty("leaderhost"), config.getProperty("leaderport"), true, 0);
            members.add(leader);
            //initSendingReplicaChannel();
        } else {
            logger.debug("Raft Instance Started");
            LEADER = false;
            //ArrayList<Member> membersFromPrimary = register();
            //members.addAll(membersFromPrimary);
            //logger.debug("Members received");
            //replicationThreadPool.submit(EventServlet.receiverWorker);
        }
    }
}
