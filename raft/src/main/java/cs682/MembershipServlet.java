package cs682;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MembershipServlet extends HttpServlet {

    protected static final Membership membership = Membership.getInstance();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response){
        String pathInfo = request.getPathInfo();
        if (pathInfo.equals("/register")) {
            membership.registerServer(request, response);
        } else if(pathInfo.equals("/add")){
            membership.addNotifiedServer(request, response);
        }
    }
}
