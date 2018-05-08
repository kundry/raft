package cs682;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.concurrent.CountDownLatch;
/**
 * Class that holds a raft log entry with the corresponding term and
 * operation.
 */
public class LogEntry {
    private int term;
    private Entry entry;
    public CountDownLatch latch;

    /**
     * Constructor
     * @param term term number
     * @param entry entry object
     */
    public LogEntry(int term, Entry entry){
        this.term = term;
        this.entry = entry;
    }

    /**
     * Sets  a value for the latch
     * @param latch counter
     */
    public void setLatch(CountDownLatch latch){
        this.latch = latch;
    }

    public Entry getEntry(){
        return this.entry;
    }
    public int getTerm(){
        return this.term;
    }
    /**
     * Decrements the latch
     */
    public void decrementLatch(){
        this.latch.countDown();
    }


}
