package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

/** Class that performs the replication. It receives the log entries and queue them
 * to be sent  to the followers
 */
public class SendingReplicaWorker implements Runnable {
    public boolean beingLeader;
    private String hostAndPort;
    private Queue<LogEntry> logEntriesQueue;
    final static Logger logger = Logger.getLogger(SendingReplicaWorker.class);

    /** Constructor of the class that initialize the  parameters needed to establish the
     *  communication with the corresponding follower
     *  @param hostAndPort host and port to replicate
     */
    public SendingReplicaWorker(String hostAndPort) {
        this.hostAndPort = hostAndPort;
        this.logEntriesQueue = new LinkedList<>();
        this.beingLeader = true;
    }

    /** Method that adds the log entry received to a queue and performs the notify
     *  to activate the processing of the incoming log entry
     *  @param logentry received by the remote host
     */
    public void queueLogEntry(LogEntry logentry){
        synchronized(this) {
            System.out.println("in queueLogentry()");
            logEntriesQueue.add(logentry);
            this.notify();
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            while (beingLeader) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (!logEntriesQueue.isEmpty()) {
                    System.out.println("Dequeueing");
                    LogEntry incomingLogEntry = logEntriesQueue.remove();
                    String url = hostAndPort + "/appendentry/entry";
                    try {
                        URL urlObj = new URL(url);
                        HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
                        setPostRequestProperties(conn);
                        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                        out.write(incomingLogEntry.wrap());
                        out.flush();
                        out.close();
                        int responseCode = conn.getResponseCode();
                        switch (responseCode) {
                            case HttpServletResponse.SC_OK:
                                System.out.println("latch count " + incomingLogEntry.latch.getCount());
                                if (incomingLogEntry.latch.getCount()>0) {
                                    System.out.println("Decrementing latch");
                                    incomingLogEntry.decrementLatch();
                                }
                                break;
                            default:
                                break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /** Method that sets the properties of a post request
     * @param conn HttpURLConnection
     **/
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
}
