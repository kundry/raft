package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;


public class RestoringLogWorker implements Runnable {
    //public boolean behind;
    private String hostAndPort;
    private int next_index;
    protected static final LogData log = LogData.getInstance();
    final static Logger logger = Logger.getLogger(RestoringLogWorker.class);

    public RestoringLogWorker(String hostAndPort, int index) {
        this.hostAndPort = hostAndPort;
        this.next_index = index;
    }

    public void decrementNextIndex(){
        synchronized(this) {
            System.out.println("decrementing index  in method");
            this.next_index --;
            this.notify();
        }
    }

    @Override
    public void run() {
        synchronized (this) {
                LogEntry nextLogEntryToSend = log.getLogEntryByIndex(next_index);
                String url = hostAndPort + "/appendentry/entry";
                try {
                    URL urlObj = new URL(url);
                    HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
                    setPostRequestProperties(conn);
                    OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                    String wrappedEntry = log.wrapOldLogEntry(nextLogEntryToSend, next_index);
                    out.write(wrappedEntry);
                    out.flush();
                    out.close();
                    int responseCode = conn.getResponseCode();
                    switch (responseCode) {
                        case HttpServletResponse.SC_OK:
                            logger.debug("Entry Matched" + System.lineSeparator());
                            if (next_index == LogData.INDEX) {
                                logger.debug("Log Restored");
                            } else {
                                RestoringLogWorker worker2 = new RestoringLogWorker(hostAndPort, next_index +1);
                                worker2.run();
                            }
                            break;
                        case HttpServletResponse.SC_BAD_REQUEST:
                            logger.debug("Entry Mismatched" + System.lineSeparator());
                            RestoringLogWorker worker = new RestoringLogWorker(hostAndPort, next_index -1);
                            worker.run();
                            break;
                        default:
                            break;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
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
