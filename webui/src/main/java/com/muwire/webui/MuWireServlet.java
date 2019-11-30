package com.muwire.webui;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.i2p.I2PAppContext;
import net.i2p.router.RouterContext;

public class MuWireServlet extends HttpServlet {

    private volatile MuWireClient client;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        RouterContext ctx = (RouterContext) I2PAppContext.getGlobalContext();
        
        
        while(!ctx.clientManager().isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new ServletException(e);
            }
        }
            
        
        String home = ctx.getConfigDir()+File.separator+"plugins"+File.separator+"MuWire";
        String version = config.getInitParameter("version");
        
        client = new MuWireClient(ctx, home, version, config.getServletContext());
        try {
            client.start();
        } catch (Throwable bad) {
            throw new ServletException(bad);
        }
        config.getServletContext().setAttribute("mwClient", client);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().setAttribute("mwClient", client);
        if (client.needsMWInit()) {
            resp.sendRedirect("/MuWire/MuWire.jsp");
        } else {
            resp.setContentType("text/html");
            if (client.getCore() == null) {
                resp.getWriter().println("<html>MW is initializing, please wait</html>");
                resp.setIntHeader("Refresh", 5);
            } else
                resp.sendRedirect("/MuWire/Home.jsp");
        }
    }

    @Override
    public void destroy() {
        if (client != null) {
            try {client.stop();} catch (Throwable ignore) {}
            client = null;
        }
    }

    
}
