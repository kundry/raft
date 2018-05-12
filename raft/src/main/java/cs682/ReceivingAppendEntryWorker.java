package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.Queue;

/** Class that receives the append entries and queue them to be later stored in the raft log.
 */

public class ReceivingAppendEntryWorker implements Runnable{

    boolean beingFollower = true;
    private Queue<ReplicatedLogEntry> replicatedEntriesQueue = new LinkedList<>();
    protected static final LogData log = LogData.getInstance();
    final static Logger logger = Logger.getLogger(ReceivingAppendEntryWorker.class);

    /** Method that adds the replicated log entry received to a queue and performs the notify
     *  to activate its processing
     *  @param replicatedLogEntry received by the remote host
     */
    public void queueReplicatedLogEntry(ReplicatedLogEntry replicatedLogEntry){
        synchronized(this) {
            replicatedEntriesQueue.add(replicatedLogEntry);
            this.notify();
        }
    }
    @Override
    public void run() {
        synchronized (this) {
            while (beingFollower) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (!replicatedEntriesQueue.isEmpty()) {
                    try {
                        logger.debug(System.lineSeparator() + "appendentry/entry ");
                        ReplicatedLogEntry incomingReplicatedEntry = replicatedEntriesQueue.remove();
                        JSONParser parser = new JSONParser();
                        JSONObject wrappedEntry = (JSONObject) parser.parse(incomingReplicatedEntry.getLogEntryContent());
                        int prevTerm = ((Long)wrappedEntry.get("prevterm")).intValue();
                        int prevIndex = ((Long)wrappedEntry.get("previndex")).intValue();
                        int term = ((Long)wrappedEntry.get("term")).intValue();
                        JSONArray entryArray = (JSONArray)wrappedEntry.get("entry");
                        JSONObject appOperation = (JSONObject)entryArray.get(0);
                        Entry entry = new Entry(appOperation);
                        LogEntry logEntryToAdd = new LogEntry(term, entry);
                        logger.debug("prevTerm " + prevTerm);
                        logger.debug("prevIndex " + prevIndex);
                        logger.debug("term " + term);
                        logger.debug("entry " + appOperation.toString());
                        boolean checkSuccess = log.consistencyCheck(prevTerm, prevIndex, logEntryToAdd);
                        if (checkSuccess){
                            incomingReplicatedEntry.getResponse().setStatus(HttpServletResponse.SC_OK);
                        } else {
                            incomingReplicatedEntry.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
