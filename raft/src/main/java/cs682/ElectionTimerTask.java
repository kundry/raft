package cs682;

import java.util.TimerTask;

public class ElectionTimerTask extends TimerTask {

    /** Class that initiates the election process
     * once the election time out has expired
     */
    @Override
    public void run() {
        System.out.println("Election Timeout");
    }
}
