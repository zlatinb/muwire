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
import static com.muwire.webui.Util._t;

public class MuWireServlet extends HttpServlet {

    private volatile MuWireClient client;
    private volatile String version;
    
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
        version = config.getInitParameter("version");
        
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
        if (client.needsMWInit()) {
            resp.sendRedirect("/MuWire/MuWire");
        } else {
            if (client.getCore() == null) {
                resp.setContentType("text/html");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().println("<html><head>\n" +
                                         "<title>" + _t("MuWire") + ' ' + version + "</title>\n" +
                                         "<link href=\"i2pbote.css?" + version + "\" rel=\"stylesheet\" type=\"text/css\">\n" +
                                         "<link href=\"muwire.css?" + version + "\" rel=\"stylesheet\" type=\"text/css\">\n" +
                                         "</head><body>\n" +
                                         "<header class=\"titlebar\">" +
                                         "<div class=\"title\">" +
                                         "<img src=\"images/muwire.png\" alt=\"\"><br>" +
                                         _t("Welcome to MuWire") +
                                         "</div>" +
                                         "<div class=\"subtitle\"><br><br><br><br></div>" +
                                         "<div class=\"pagetitle\">" +
                                         _t("MuWire is initializing, please wait") +
                                         "&hellip;</div>" +
                                         "</header>" +
                                         "<noscript><center><b>" +
                                         _t("MuWire requires JavaScript. Please enable JavaScript in your browser.") +
                                         "</b></center></noscript>" +
                                         "</body></html>");
                resp.setIntHeader("Refresh", 5);
            } else
                resp.sendRedirect("/MuWire/Home");
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
