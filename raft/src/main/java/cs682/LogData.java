package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
/** Class that holds the Raft Log and has all the operations available over the log
 * It also keeps track of global variables used by other classes related with different
 * raft operations*/
public class LogData {
    private List<LogEntry> log;
    private ReentrantLock lock;
    public static int TERM;
    public static int INDEX;
    public static int COMMIT_INDEX;
    private JSONArray jsonLog;
    private static final String BACKUP_DIR = "out";
    final static Logger logger = Logger.getLogger(LogData.class);

    /** Makes sure only one LogData is instantiated. */
    private static LogData singleton = new LogData();

    /** Constructor */
    private LogData() {
        log = Collections.synchronizedList(new ArrayList<LogEntry>());
        lock = new ReentrantLock();
        jsonLog = new JSONArray();
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
        INDEX ++;
        printLog();
        updateJsonLog(logentry);
        sendJsonLogToDisk();
    }

    /**
     * Method that allows to add the init log entry at position 0 to the log
     * @param logentry new log entry to be inserted
     */
    public void addInitLogEntry(LogEntry logentry) {
        log.add(logentry);
        INDEX = 0;
        updateJsonLog(logentry);
        sendJsonLogToDisk();
    }


    private void updateJsonLog(LogEntry logentry){
        lock.lock();
        JSONObject json = new JSONObject();
        json.put("term", logentry.getTerm());
        JSONArray array = new JSONArray();
        array.add(logentry.getEntry().getOperationData());
        json.put("entry", array);
        jsonLog.add(json);
        lock.unlock();
    }

    /**
     * Saves the Raft log to disk for recovering purposes
     */
    private void sendJsonLogToDisk() {
        lock.lock();
        String backupName = BACKUP_DIR + File.separator + "raft_log_backup.json";
        try (PrintWriter pw = new PrintWriter(backupName)) {
            String content = jsonLog.toString();
            pw.println(content);
            pw.flush();
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Builds the json like string with the content of the log entry to be
     * sent to the followers
     */
    public String wrap(LogEntry logentry){
        JSONObject wrappedJson = new JSONObject();
        int prevTerm = getPrevTerm();
        int prevIndex = getPrevIndex();
        wrappedJson.put("prevterm", prevTerm);
        wrappedJson.put("previndex", prevIndex);
        wrappedJson.put("term", TERM);
        JSONArray array = new JSONArray();
        array.add(logentry.getEntry().getOperationData());
        wrappedJson.put("entry", array);
        return wrappedJson.toString();
    }

    /**
     * Method that returns the term of the entry before the last one
     * @return prevTerm
     */
    public int getPrevTerm() {
        lock.lock();
        int prevTerm;
        prevTerm = log.get(log.size()-2).getTerm();
        lock.unlock();
        return prevTerm;
    }

    /**
     * Method that returns the term of the entry before the last one
     * @return prevIndex
     */
    public int getPrevIndex() {
        lock.lock();
        int prevIndex;
        prevIndex = log.size()-2;
        lock.unlock();
        return prevIndex;
    }
    /**
     * Method that returns the term of the last entry stored
     * @return lastTerm
     */
    public int getLastLogEntryTerm() {
        int lastTerm;
        lastTerm = log.get(log.size()-1).getTerm();
        return lastTerm;
    }

    /**
     * Method that returns the index of the last entry stored
     * @return lastTerm
     */
    public int getLastLogIndex() {
        int lastIndex;
        lastIndex = log.size()-1;
        return lastIndex;
    }

    public boolean consistencyCheck(int leaderPrevTerm, int leaderPrevIndex, LogEntry logEntryToAdd) {
        if ((leaderPrevIndex == INDEX) && (leaderPrevTerm == getLastLogEntryTerm())) {
            logger.debug("AppendEntry accepted");
            add(logEntryToAdd);
            return true;
        } else {
            logger.debug("AppendEntry rejected");
            return false;
        }
    }


    public void printLog(){
        synchronized (log) {
            logger.debug(System.lineSeparator() + "Log content:");
            int i = 0;
            for (LogEntry logentry : log) {
                logger.debug(i + " [ Term: " + logentry.getTerm() + ", Entry: " + logentry.getEntry().getOperationData().toString() + " ]");
                i++;
            }
            logger.debug(System.lineSeparator());
        }
    }

    public void loadRaftLogBackup(){
        JSONParser parser = new JSONParser();
        String backupName = BACKUP_DIR + File.separator + "raft_log_backup.json";
        try {
            JSONArray array = (JSONArray)parser.parse(new FileReader(backupName));
            Iterator<JSONObject> iterator = array.iterator();
            while (iterator.hasNext()) {
                JSONObject logEntry = iterator.next();
                int term = ((Long)logEntry.get("term")).intValue();
                JSONArray entryArray = (JSONArray)logEntry.get("entry");
                JSONObject appOperation = (JSONObject)entryArray.get(0);
                Entry entry = new Entry(appOperation);
                LogEntry logEntryObj = new LogEntry(term, entry);
                add(logEntryObj);
            }
        } catch  (FileNotFoundException e) {
            System.out.println("Could not find file: " + backupName);
        } catch (org.json.simple.parser.ParseException e) {
            System.out.println("Can not parse a given json file. ");
        } catch (IOException e) {
            System.out.println("General IO Exception");
        }
    }

    public LogEntry getLogEntryByIndex(int index) {
        lock.lock();
        LogEntry result = log.get(index);
        lock.unlock();
        return result;
    }

    public String wrapOldLogEntry(LogEntry logentry, int index){
        lock.lock();
        JSONObject wrappedJson = new JSONObject();
        int prevTerm = getPrevTerm(index);
        int prevIndex = index-1;
        wrappedJson.put("prevterm", prevTerm);
        wrappedJson.put("previndex", prevIndex);
        wrappedJson.put("term", logentry.getTerm());
        JSONArray array = new JSONArray();
        array.add(logentry.getEntry().getOperationData());
        wrappedJson.put("entry", array);

        logger.debug("Preparing Previous LogEntry: ");
        logger.debug(wrappedJson.toString());
        lock.unlock();
        return wrappedJson.toString();
    }

    /**
     * Method that returns the term of the entry before the one in de given
     * index
     * @return prevTerm
     */
    public int getPrevTerm(int index) {
        lock.lock();
        int prevTerm;
        prevTerm = log.get(index-1).getTerm();
        lock.unlock();
        return prevTerm;
    }

    /**
     * Method that builds the body of the heartbeat and returns it as a JSONObject
     * @return heartbeat body
     */
    public JSONObject buildHeartBeatBody(){
        lock.lock();
        JSONObject json = new JSONObject();
        String leaderId = Membership.SELF_HOST+":"+Membership.SELF_HOST;
        json.put("term", TERM);
        json.put("leaderid", leaderId);
        json.put("commitindex", COMMIT_INDEX);
        lock.unlock();
        return json;
    }
}
