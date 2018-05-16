package cs682;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.FileReader;
import java.util.Properties;
import java.util.Timer;

public class Driver {

    protected static final Membership membership = Membership.getInstance();

    public static void main(String[] args) {
        try {

            Properties config = loadConfig("config.properties");
            membership.loadSelfConfiguration(config);
            Server jettyHttpServer = new Server(Membership.SELF_PORT);
            ServletHandler jettyHandler = new ServletHandler();
            jettyHandler.addServletWithMapping(new ServletHolder(new RPCServlet()), "/appendentry/*");
            jettyHandler.addServletWithMapping(new ServletHolder(new RPCServlet()), "/requestvote/*");
            jettyHandler.addServletWithMapping(new ServletHolder(new MembershipServlet()), "/members/*");
            jettyHttpServer.setHandler(jettyHandler);
            jettyHttpServer.start();
            membership.loadInitMembers(config);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * It Loads the properties file with configuration information
     * Ports and Hosts of the different services
     * @param configPath name of the file
     * */
    private static Properties loadConfig(String configPath){
        Properties config = new Properties();
        try {
            config.load(new FileReader(configPath));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return config;
    }

}
