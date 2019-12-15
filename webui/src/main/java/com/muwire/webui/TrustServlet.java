package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        } else if (section.equals("trustedUsers")) {
            List<TrustUser> list = new ArrayList<>();
            for(TrustEntry te : core.getTrustService().getGood().values()) {
                TrustUser trustUser = new TrustUser(
                        te.getPersona(),
                        te.getReason(),
                        core.getTrustSubscriber().isSubscribed(te.getPersona()));
                list.add(trustUser);
            }
            
            USER_COMPARATORS.sort(list, req);
            
            sb.append("<Users>");
            list.forEach(u -> u.toXML(sb));
            sb.append("</Users>");
        } else if (section.equals("distrustedUsers")) {
            List<TrustUser> list = new ArrayList<>();
            for(TrustEntry te : core.getTrustService().getBad().values()) {
                TrustUser trustUser = new TrustUser(
                        te.getPersona(),
                        te.getReason(),
                        false);
                list.add(trustUser);
            }
            
            USER_COMPARATORS.sort(list, req);
            
            sb.append("<Users>");
            list.forEach(l -> l.toXML(sb));
            sb.append("</Users>");
        } else if (section.equals("subscriptions")) {
            
            List<Subscription> subs = new ArrayList<>();
            
            for (RemoteTrustList list : core.getTrustSubscriber().getRemoteTrustLists().values()) {
                Subscription sub = new Subscription(list.getPersona(),
                        list.getStatus(),
                        list.getTimestamp(),
                        list.getGood().size(),
                        list.getBad().size());
                subs.add(sub);
            }
            
            SUBSCRIPTION_COMPARATORS.sort(subs, req);
            
            sb.append("<Subscriptions>");
            subs.forEach(sub -> sub.toXML(sb));
            sb.append("</Subscriptions>");
            
        } else if (section.equals("listTrusted")) {
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
            
            List<TrustListEntry> entries = new ArrayList<>();
            list.getGood().forEach(good -> {
                String reason = good.getReason() == null ? "" : good.getReason();
                TrustListEntry entry = new TrustListEntry(
                        good.getPersona(),
                        reason,
                        core.getTrustService().getLevel(good.getPersona().getDestination()));
                entries.add(entry);
            });
            
            TRUST_LIST_ENTRY_COMPARATORS.sort(entries, req);
            
            sb.append("<List>");
            entries.forEach(entry -> entry.toXML(sb));
            sb.append("</List>");
        } else if (section.equals("listDistrusted")) {
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
            
            List<TrustListEntry> entries = new ArrayList<>();
            list.getBad().forEach(bad -> {
                String reason = bad.getReason() == null ? "" : bad.getReason();
                TrustListEntry entry = new TrustListEntry(
                        bad.getPersona(),
                        reason,
                        core.getTrustService().getLevel(bad.getPersona().getDestination()));
                entries.add(entry);
            });
            
            TRUST_LIST_ENTRY_COMPARATORS.sort(entries, req);
            
            sb.append("<List>");
            entries.forEach(entry -> entry.toXML(sb));
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
            Util.pause();
        } else if (action.equals("unsubscribe")) {
            core.getMuOptions().getTrustSubscriptions().remove(p);
            TrustSubscriptionEvent event = new TrustSubscriptionEvent();
            event.setPersona(p);
            event.setSubscribe(false);
            core.getEventBus().publish(event);
            Util.pause();
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
        Util.pause();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        core = (Core) config.getServletContext().getAttribute("core");
        trustManager = (TrustManager) config.getServletContext().getAttribute("trustManager");
    }

    private static class TrustUser {
        private final Persona persona;
        private final String reason;
        private final boolean subscribed;
        TrustUser(Persona persona, String reason, boolean subscribed) {
            this.persona = persona;
            this.reason = reason;
            this.subscribed = subscribed;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Persona>");
            sb.append("<User>").append(Util.escapeHTMLinXML(persona.getHumanReadableName())).append("</User>");
            sb.append("<UserB64>").append(persona.toBase64()).append("</UserB64>");
            sb.append("<Reason>").append(Util.escapeHTMLinXML(reason)).append("</Reason>");
            sb.append("<Subscribed>").append(subscribed).append("</Subscribed>");
            sb.append("</Persona>");
        }
    }

    private static final Comparator<TrustUser> USER_BY_USER = (l, r) -> {
        return Collator.getInstance().compare(l.persona.getHumanReadableName(), r.persona.getHumanReadableName());
    };
    
    private static final Comparator<TrustUser> USER_BY_REASON = (l, r) -> {
        return Collator.getInstance().compare(l.reason, r.reason);
    };
    
    private static final Comparator<TrustUser> USER_BY_SUBSCRIBED = (l, r) -> {
        return Boolean.compare(l.subscribed, r.subscribed);
    };
    
    private static final ColumnComparators<TrustUser> USER_COMPARATORS = new ColumnComparators<>();
    static {
        USER_COMPARATORS.add("User", USER_BY_USER);
        USER_COMPARATORS.add("Reason", USER_BY_REASON);
        USER_COMPARATORS.add("Subscribe", USER_BY_SUBSCRIBED);
    }
    
    private static class Subscription {
        private final Persona persona;
        private final RemoteTrustList.Status status;
        private final long timestamp;
        private final int trusted, distrusted;
        
        Subscription(Persona persona, RemoteTrustList.Status status, long timestamp,
                int trusted, int distrusted) {
            this.persona = persona;
            this.status = status;
            this.timestamp = timestamp;
            this.trusted = trusted;
            this.distrusted = distrusted;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Subscription>");
            sb.append("<User>").append(Util.escapeHTMLinXML(persona.getHumanReadableName())).append("</User>");
            sb.append("<UserB64>").append(persona.toBase64()).append("</UserB64>");
            sb.append("<Status>").append(status).append("</Status>");
            String timestampString = Util._t("Never");
            if (timestamp > 0)
                timestampString = DataHelper.formatTime(timestamp);
            sb.append("<Timestamp>").append(timestampString).append("</Timestamp>");
            sb.append("<Trusted>").append(trusted).append("</Trusted>");
            sb.append("<Distrusted>").append(distrusted).append("</Distrusted>");
            sb.append("</Subscription>");
        }
    }
    
    private static final Comparator<Subscription> SUBSCRIPTION_BY_USER = (l, r) -> {
        return Collator.getInstance().compare(l.persona.getHumanReadableName(), r.persona.getHumanReadableName());
    };
    
    private static final Comparator<Subscription> SUBSCRIPTION_BY_STATUS = (l, r) -> {
        return Collator.getInstance().compare(l.status.toString(), r.status.toString());
    };
    
    private static final Comparator<Subscription> SUBSCRIPTION_BY_TIMESTAMP = (l, r) -> {
        return Long.compare(l.timestamp, r.timestamp);
    };
    
    private static final Comparator<Subscription> SUBSCRIPTION_BY_TRUSTED = (l, r) -> {
        return Integer.compare(l.trusted, r.trusted);
    };
    
    private static final Comparator<Subscription> SUBSCRIPTION_BY_DISTRUSTED = (l, r) -> {
        return Integer.compare(l.distrusted, r.distrusted);
    };
    
    private static final ColumnComparators<Subscription> SUBSCRIPTION_COMPARATORS = new ColumnComparators<>();
    static {
        SUBSCRIPTION_COMPARATORS.add("Name", SUBSCRIPTION_BY_USER);
        SUBSCRIPTION_COMPARATORS.add("Status", SUBSCRIPTION_BY_STATUS);
        SUBSCRIPTION_COMPARATORS.add("Last Updated", SUBSCRIPTION_BY_TIMESTAMP);
        SUBSCRIPTION_COMPARATORS.add("Trusted", SUBSCRIPTION_BY_TRUSTED);
        SUBSCRIPTION_COMPARATORS.add("Distrusted", SUBSCRIPTION_BY_DISTRUSTED);
    }
    
    private static class TrustListEntry {
        private final Persona persona;
        private final String reason;
        private final TrustLevel status;
        
        TrustListEntry(Persona persona, String reason, TrustLevel status) {
            this.persona = persona;
            this.reason = reason;
            this.status = status;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Persona>");
            sb.append("<User>").append(Util.escapeHTMLinXML(persona.getHumanReadableName())).append("</User>");
            sb.append("<UserB64>").append(persona.toBase64()).append("</UserB64>");
            sb.append("<Reason>").append(Util.escapeHTMLinXML(reason)).append("</Reason>");
            sb.append("<Status>").append(status).append("</Status>");
            sb.append("</Persona>");
        }
    }
    
    private static final Comparator<TrustListEntry> TRUST_LIST_ENTRY_BY_USER = (l, r) -> {
        return Collator.getInstance().compare(l.persona.getHumanReadableName(), r.persona.getHumanReadableName());
    };
    
    private static final Comparator<TrustListEntry> TRUST_LIST_ENTRY_BY_REASON = (l, r) -> {
        return Collator.getInstance().compare(l.reason, r.reason);
    };
    
    private static final Comparator<TrustListEntry> TRUST_LIST_ENTRY_BY_STATUS = (l, r) -> {
        return Collator.getInstance().compare(l.status.toString(), r.status.toString());
    };
    
    private static final ColumnComparators<TrustListEntry> TRUST_LIST_ENTRY_COMPARATORS = new ColumnComparators<>();
    static {
        TRUST_LIST_ENTRY_COMPARATORS.add("User", TRUST_LIST_ENTRY_BY_USER);
        TRUST_LIST_ENTRY_COMPARATORS.add("Reason", TRUST_LIST_ENTRY_BY_REASON);
        TRUST_LIST_ENTRY_COMPARATORS.add("Your Trust", TRUST_LIST_ENTRY_BY_STATUS);
    }
    
}
