package com.muwire.webui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.search.UIResultEvent;
import com.muwire.core.trust.TrustLevel;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class SearchServlet extends HttpServlet {
    
    private Core core;
    private SearchManager searchManager;
    private ConnectionCounter connectionCounter;
    private DownloadManager downloadManager;
    private BrowseManager browseManager;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (searchManager == null) {
            resp.sendError(403, "Not initialized");
            return;
        }
        String search = req.getParameter("search");
        searchManager.newSearch(search);
        resp.sendRedirect("/MuWire/Home");
    }
    
    

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String section = req.getParameter("section");
        if (section == null) {
            resp.sendError(403, "Bad section param");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        if (section.equals("status")) {
            if (searchManager == null || downloadManager == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
            
            String key = req.getParameter("key");
            String order = req.getParameter("order");
            Comparator<SearchResults> comparator = SEARCH_COMPARATORS.get(key, order);
            
            List<SearchResults> searchResults = new ArrayList<>(searchManager.getResults().values());
            if (comparator != null)
                Collections.sort(searchResults, comparator);
            
            sb.append("<Searches>");
            for (SearchResults results : searchResults) {
                sb.append("<Search>");
                sb.append("<Revision>").append(results.getRevision()).append("</Revision>");
                sb.append("<Query>").append(Util.escapeHTMLinXML(results.getSearch())).append("</Query>");
                sb.append("<uuid>").append(results.getUUID().toString()).append("</uuid>");
                sb.append("<Senders>").append(results.getSenderCount()).append("</Senders>");
                sb.append("<Results>").append(results.totalResults()).append("</Results>");
                sb.append("</Search>");
            }
            sb.append("</Searches>");
        } else if (section.equals("senders")) {
            if (searchManager == null || downloadManager == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
            
            String uuidString = req.getParameter("uuid");
            if (uuidString == null) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            UUID uuid = UUID.fromString(uuidString);
            
            SearchResults results = searchManager.getResults().get(uuid);
            if (results == null)
                return;
            
            sb.append("<Senders>");
            results.getBySender().forEach( (persona, resultsFromSender) -> {
                Sender sender = new Sender(persona,
                        core.getTrustService().getLevel(persona.getDestination()),
                        resultsFromSender.iterator().next().getBrowse(),
                        browseManager.isBrowsing(persona),
                        resultsFromSender.size());
                sender.toXML(sb);
            });
            sb.append("</Senders>");
        } else if (section.equals("groupBySender")) {
            if (searchManager == null || downloadManager == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
            
            String uuidString = req.getParameter("uuid");
            if (uuidString == null) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            UUID uuid = UUID.fromString(uuidString);
            
            SearchResults results = searchManager.getResults().get(uuid);
            if (results == null)
                return;
            
            Map<Persona, Set<UIResultEvent>> bySender = results.getBySender();
            sb.append("<ResultsBySender>");
            bySender.forEach((sender, resultsFromSender) -> {
                sb.append("<ResultsFromSender>");
                sb.append("<Sender>");
                sb.append(Util.escapeHTMLinXML(sender.getHumanReadableName()));
                sb.append("</Sender>");
                sb.append("<SenderB64>").append(sender.toBase64()).append("</SenderB64>");
                sb.append("<Browse>").append(resultsFromSender.iterator().next().getBrowse()).append("</Browse>");
                sb.append("<Browsing>").append(browseManager.isBrowsing(sender)).append("</Browsing>");
                sb.append("<Trust>").append(core.getTrustService().getLevel(sender.getDestination())).append("</Trust>");
                resultsFromSender.forEach(result -> {
                    sb.append("<Result>");
                    sb.append("<Name>");
                    sb.append(Util.escapeHTMLinXML(result.getName()));
                    sb.append("</Name>");
                    sb.append("<Size>");
                    sb.append(DataHelper.formatSize2Decimal(result.getSize(), false)).append("B");
                    sb.append("</Size>");
                    String infohash = Base64.encode(result.getInfohash().getRoot());
                    sb.append("<InfoHash>");
                    sb.append(infohash);
                    sb.append("</InfoHash>");
                    sb.append("<Downloading>").append(downloadManager.isDownloading(result.getInfohash())).append("</Downloading>");
                    if (result.getComment() != null) {
                        sb.append("<Comment>")
                        .append(Util.escapeHTMLinXML(result.getComment()))
                        .append("</Comment>");
                    }
                    sb.append("<Certificates>").append(result.getCertificates()).append("</Certificates>");
                    sb.append("</Result>");
                });
                sb.append("</ResultsFromSender>");
            });
            sb.append("</ResultsBySender>");
        } else if (section.equals("groupByFile")) {
            if (searchManager == null || downloadManager == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
            
            String uuidString = req.getParameter("uuid");
            if (uuidString == null) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            UUID uuid = UUID.fromString(uuidString);
            
            SearchResults results = searchManager.getResults().get(uuid);
            if (results == null)
                return;

            Map<InfoHash, Set<UIResultEvent>> byInfohash = results.getByInfoHash();
            sb.append("<ResultsByFile>");
            byInfohash.forEach((infoHash, resultSet) -> {
                sb.append("<ResultsForFile>");
                UIResultEvent first = resultSet.iterator().next();
                sb.append("<InfoHash>").append(Base64.encode(infoHash.getRoot())).append("</InfoHash>");
                sb.append("<Downloading>").append(downloadManager.isDownloading(infoHash)).append("</Downloading>");
                sb.append("<Name>").append(Util.escapeHTMLinXML(first.getName())).append("</Name>");
                sb.append("<Size>").append(DataHelper.formatSize2Decimal(first.getSize(), false)).append("B").append("</Size>");
                resultSet.forEach(result -> {
                    sb.append("<Result>");
                    sb.append("<Sender>").append(Util.escapeHTMLinXML(result.getSender().getHumanReadableName())).append("</Sender>");
                    sb.append("<SenderB64>").append(result.getSender().toBase64()).append("</SenderB64>");
                    sb.append("<Browse>").append(result.getBrowse()).append("</Browse>");
                    sb.append("<Browsing>").append(browseManager.isBrowsing(result.getSender())).append("</Browsing>");
                    sb.append("<Trust>").append(core.getTrustService().getLevel(result.getSender().getDestination())).append("</Trust>");
                    if (result.getComment() != null) {
                        sb.append("<Comment>")
                        .append(Util.escapeHTMLinXML(result.getComment()))
                        .append("</Comment>");
                    }
                    sb.append("<Certificates>").append(result.getCertificates()).append("</Certificates>");
                    sb.append("</Result>");
                });
                sb.append("</ResultsForFile>");
            });
            sb.append("</ResultsByFile>");
        } else if (section.equals("connectionsCount")) {
            if (connectionCounter == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
            sb.append("<Connections>");
            sb.append(connectionCounter.getConnections());
            sb.append("</Connections>");
        } else {
            resp.sendError(403, "Bad section param");
            return;
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
    public void init(ServletConfig config) throws ServletException {
        searchManager = (SearchManager) config.getServletContext().getAttribute("searchManager");
        connectionCounter = (ConnectionCounter) config.getServletContext().getAttribute("connectionCounter");
        downloadManager = (DownloadManager) config.getServletContext().getAttribute("downloadManager");
        browseManager = (BrowseManager) config.getServletContext().getAttribute("browseManager");
        core = (Core) config.getServletContext().getAttribute("core");
    }
    
    private class Sender {
        private final Persona persona;
        private final TrustLevel trustLevel;
        private final boolean browse;
        private final boolean browsing;
        private final int results;
        
        Sender(Persona persona, TrustLevel trustLevel, boolean browse, boolean browsing, int results) {
            this.persona = persona;
            this.trustLevel = trustLevel;
            this.browse = browse;
            this.browsing = browsing;
            this.results = results;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Sender>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(persona.getHumanReadableName())).append("</Name>");
            sb.append("<B64>").append(persona.toBase64()).append("</B64>");
            sb.append("<Trust>").append(trustLevel.toString()).append("</Trust>");
            sb.append("<Browse>").append(browse).append("</Browse>");
            sb.append("<Browsing>").append(browsing).append("</Browsing>");
            sb.append("<Results>").append(results).append("</Results>");
            sb.append("</Sender>");
        }
    }
    
    private static final Comparator<SearchResults> SEARCH_BY_NAME = (k, v) -> {
        return k.getSearch().compareTo(v.getSearch());
    };
    
    private static final Comparator<SearchResults> SEARCH_BY_SENDERS = (k, v) -> {
        return Integer.compare(k.getSenderCount(), v.getSenderCount());
    };
    
    private static final Comparator<SearchResults> SEARCH_BY_RESULTS = (k, v) -> {
        return Integer.compare(k.getResultCount(), v.getResultCount());
    };
    
    private static final ColumnComparators<SearchResults> SEARCH_COMPARATORS = new ColumnComparators<>();
    static {
        SEARCH_COMPARATORS.add("Query", SEARCH_BY_NAME);
        SEARCH_COMPARATORS.add("Senders", SEARCH_BY_SENDERS);
        SEARCH_COMPARATORS.add("Results", SEARCH_BY_RESULTS);
    }
    
    private static final Comparator<Sender> SENDER_BY_NAME = (k, v) -> {
        return k.persona.getHumanReadableName().compareTo(v.persona.getHumanReadableName());
    };
    
    private static final Comparator<Sender> SENDER_BY_TRUST = (k, v) -> {
        return k.trustLevel.toString().compareTo(v.trustLevel.toString());
    };
    
    private static final Comparator<Sender> SENDER_BY_RESULTS = (k, v) -> {
        return Integer.compare(k.results, v.results);
    };
    
    private static final ColumnComparators<Sender> SENDER_COMPARATORS = new ColumnComparators<>();
    static {
        SENDER_COMPARATORS.add("Sender", SENDER_BY_NAME);
        SENDER_COMPARATORS.add("Trust", SENDER_BY_TRUST);
        SENDER_COMPARATORS.add("Results", SENDER_BY_RESULTS);
    }

}
