package cs682;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.http.HttpServletResponse;


/** Thread that receives the append entries and perform its processing.
 *  One thread is created per entry received
 */
public class ReceivingRepAppendEntryWorker implements Runnable {

    private String json;
    private HttpServletResponse response;
    protected static final LogData log = LogData.getInstance();
    final static Logger logger = Logger.getLogger(ReceivingRepAppendEntryWorker.class);

    public ReceivingRepAppendEntryWorker( String json, HttpServletResponse response){
        this.json = json;
        this.response = response;
    }

    @Override
    public void run() {

        try {
            logger.debug(System.lineSeparator() + "appendentry/entry ");
            JSONParser parser = new JSONParser();
            JSONObject wrappedEntry = (JSONObject) parser.parse(json);
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
            System.out.println( "Result of check "+  checkSuccess);
            if (checkSuccess){
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
