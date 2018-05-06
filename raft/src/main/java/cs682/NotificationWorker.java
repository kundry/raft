package cs682;

import org.apache.log4j.Logger;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Runnable that sends messages to the nodes of the architecture
 * Used by different kind of notification messages
 * */
public class NotificationWorker implements Runnable {

    private String url;
    private String body;
    final static Logger logger = Logger.getLogger(NotificationWorker.class);

    public NotificationWorker(String url, String body) {
        this.url = url;
        this.body = body;
    }

    @Override
    public void run() {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection conn  = (HttpURLConnection) urlObj.openConnection();
            setPostRequestProperties(conn);
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(body);
            out.flush();
            out.close();
            int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HttpServletResponse.SC_OK:
                    break;
                case HttpServletResponse.SC_BAD_REQUEST:
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
