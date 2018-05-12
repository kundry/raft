package cs682;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Runnable that sends a heartbeat to the given node
 * */
public class HeartBeatWorker implements Runnable {

    private String url;
    private Member member;
    protected static final Membership membership = Membership.getInstance();
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
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            logger.debug("Follower Down");
        }

    }
}
