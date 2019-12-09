package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.Persona;
import com.muwire.core.trust.RemoteTrustList;
import com.muwire.core.trust.TrustEvent;
import com.muwire.core.trust.TrustLevel;
import com.muwire.core.trust.TrustService;
import com.muwire.core.trust.TrustService.TrustEntry;
import com.muwire.core.trust.TrustSubscriber;
import com.muwire.core.trust.TrustSubscriptionEvent;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class TrustServlet extends HttpServlet {
    
    private TrustManager trustManager;
    private Core core;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String section = req.getParameter("section");
        if (section == null) {
            resp.sendError(403, "Bad action param");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        
        if (section.equals("revision")) {
            sb.append("<Revision>").append(trustManager.getRevision()).append("</Revision>");
        } else if (section.equals("users")) {
            sb.append("<Users>");
            
            sb.append("<Trusted>");
            for (TrustEntry te : core.getTrustService().getGood().values()) {
                TEtoXML(te,sb, core.getTrustSubscriber());
            }
            sb.append("</Trusted>");
            
            sb.append("<Distrusted>");
            for (TrustEntry te : core.getTrustService().getBad().values()) {
                TEtoXML(te, sb, core.getTrustSubscriber());
            }
            sb.append("</Distrusted>");
            
            sb.append("</Users>");
            
        } else if (section.equals("subscriptions")) {
            sb.append("<Subscriptions>");
            
            for (RemoteTrustList list : core.getTrustSubscriber().getRemoteTrustLists().values()) {
                sb.append("<Subscription>");
                sb.append("<User>").append(Util.escapeHTMLinXML(list.getPersona().getHumanReadableName())).append("</User>");
                sb.append("<UserB64>").append(list.getPersona().toBase64()).append("</UserB64>");
                sb.append("<Status>").append(list.getStatus()).append("</Status>");
                String timestamp = "Never";
                if (list.getTimestamp() > 0)
                    timestamp = DataHelper.formatTime(list.getTimestamp());
                sb.append("<Timestamp>").append(timestamp).append("</Timestamp>");
                sb.append("<Trusted>").append(list.getGood().size()).append("</Trusted>");
                sb.append("<Distrusted>").append(list.getBad().size()).append("</Distrusted>");
                sb.append("</Subscription>");
            }
            
            sb.append("</Subscriptions>");
            
        } else if (section.equals("list")) {
            String userB64 = req.getParameter("user");
            Persona p;
            try {
                p = new Persona(new ByteArrayInputStream(Base64.decode(userB64)));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            RemoteTrustList list = core.getTrustSubscriber().getRemoteTrustLists().get(p.getDestination());
            if (list == null) 
                return;
            
            sb.append("<List>");
            
            sb.append("<Trusted>");
            for (TrustEntry te : list.getGood()) {
                TEtoXML(te, sb, core.getTrustService());
            }
            sb.append("</Trusted>");
            
            sb.append("<Distrusted>");
            for (TrustEntry te : list.getBad()) {
                TEtoXML(te, sb, core.getTrustService());
            }
            sb.append("</Distrusted>");
            
            sb.append("</List>");
        }
        
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-store, max-age=0, no-cache, must-revalidate");
        byte[] out = sb.toString().getBytes("UTF-8");
        resp.setContentLength(out.length);
        resp.getOutputStream().write(out);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (core == null) {
            resp.sendError(403, "Not initialized");
            return;
        }
       
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(403, "Bad param");
            return;
        }

        String persona = req.getParameter("persona");
        Persona p;
        try {
            p = new Persona(new ByteArrayInputStream(Base64.decode(persona)));
        } catch (Exception bad) {
            resp.sendError(403, "Bad param");
            return;
        }
        
        if (action.equals("subscribe")) {
            core.getMuOptions().getTrustSubscriptions().add(p);
            TrustSubscriptionEvent event = new TrustSubscriptionEvent();
            event.setPersona(p);
            event.setSubscribe(true);
            core.getEventBus().publish(event);
        } else if (action.equals("unsubscribe")) {
            core.getMuOptions().getTrustSubscriptions().remove(p);
            TrustSubscriptionEvent event = new TrustSubscriptionEvent();
            event.setPersona(p);
            event.setSubscribe(false);
            core.getEventBus().publish(event);
        } else if (action.equals("trust")) {
            doTrust(p, TrustLevel.TRUSTED, req.getParameter("reason"));
        } else if (action.equals("neutral")) {
            doTrust(p, TrustLevel.NEUTRAL, req.getParameter("reason"));
        } else if (action.equals("distrust")) {
            doTrust(p, TrustLevel.DISTRUSTED, req.getParameter("reason"));
        }
    }
    
    private void doTrust(Persona p, TrustLevel level, String reason) {
        TrustEvent event = new TrustEvent();
        event.setLevel(level);
        event.setPersona(p);
        event.setReason(reason);
        core.getEventBus().publish(event);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        core = (Core) config.getServletContext().getAttribute("core");
        trustManager = (TrustManager) config.getServletContext().getAttribute("trustManager");
    }

    private static void TEtoXML(TrustEntry te, StringBuilder sb, TrustSubscriber trustSubscriber) {
        sb.append("<Persona>");
        sb.append("<User>").append(Util.escapeHTMLinXML(te.getPersona().getHumanReadableName())).append("</User>");
        sb.append("<UserB64>").append(te.getPersona().toBase64()).append("</UserB64>");
        String reason = "";
        if (te.getReason() != null)
            reason = te.getReason();
        sb.append("<Reason>").append(Util.escapeHTMLinXML(reason)).append("</Reason>");
        sb.append("<Subscribed>").append(trustSubscriber.isSubscribed(te.getPersona())).append("</Subscribed>");
        sb.append("</Persona>");
    }
    
    private static void TEtoXML(TrustEntry te, StringBuilder sb, TrustService trustService) {
        sb.append("<Persona>");
        sb.append("<User>").append(Util.escapeHTMLinXML(te.getPersona().getHumanReadableName())).append("</User>");
        sb.append("<UserB64>").append(te.getPersona().toBase64()).append("</UserB64>");
        String reason = "";
        if (te.getReason() != null)
            reason = te.getReason();
        sb.append("<Reason>").append(Util.escapeHTMLinXML(reason)).append("</Reason>");
        sb.append("<Status>").append(trustService.getLevel(te.getPersona().getDestination())).append("</Status>");
        sb.append("</Persona>");
    }
}
