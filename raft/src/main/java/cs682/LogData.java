package cs682;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class LogData {
    private List<LogEntry> log;
    private ReentrantLock lock;
    public static int TERM;
    final static Logger logger = Logger.getLogger(Membership.class);

    /** Makes sure only one LogData is instantiated. */
    private static LogData singleton = new LogData();

    /** Constructor */
    private LogData() {
        log = Collections.synchronizedList(new ArrayList<LogEntry>());
        lock = new ReentrantLock();
    }

    /** Makes sure only one LogData is instantiated. Returns the Singleton */
    public static LogData getInstance(){
        return singleton;
    }

    /**
     * Method that allows to add a new log entry to the log
     * @param logentry new log entry to be inserted
     */
    public void add(LogEntry logentry) {
        log.add(logentry);
    }

}
