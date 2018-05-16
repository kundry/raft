package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Runnable that sends a heartbeat to the given node
 * */
public class HeartBeatWorker implements Runnable {

    private String url;
    private Member member;
    protected static final LogData log = LogData.getInstance();
    final static Logger logger = Logger.getLogger(HeartBeatWorker.class);

    /**
     * Constructor
     * @param m Member to send the heartbeat
     * */
    public HeartBeatWorker(Member m){
        this.member = m;
        this.url = "http://" + m.getHost() + ":" + m.getPort() + "/appendentry/entry";
    }
    @Override
    public void run() {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            setPostRequestProperties(conn);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            JSONObject heartbeatBody = log.buildHeartBeatBody();
            out.write(heartbeatBody.toString());
            out.flush();
            out.close();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    logger.debug("Heartbeat rejected");
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            logger.debug("Heartbeat did not reach follower");
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
