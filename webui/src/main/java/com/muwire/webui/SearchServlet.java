package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Collator;
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
        String action = req.getParameter("action");
        
        if (action.equals("start")) {
            String search = req.getParameter("search");
            UUID newUUID = searchManager.newSearch(search);
            if (newUUID != null)
                resp.sendRedirect("/MuWire/Home?uuid=" + newUUID.toString());
            else
                resp.sendError(403, Util._t("Please enter a search keyword or file hash"));
        } else if (action.equals("stop")) {
            String uuidString = req.getParameter("uuid");
            UUID uuid = UUID.fromString(uuidString);
            searchManager.stopSearch(uuid);
        }
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
            
            List<SearchResults> searchResults = new ArrayList<>(searchManager.getResults().values());
            SEARCH_COMPARATORS.sort(searchResults, req);
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
            
            List<Sender> senders = new ArrayList<>();
            results.getBySender().forEach( (persona, resultsFromSender) -> {
                UIResultEvent first = resultsFromSender.iterator().next();
                Sender sender = new Sender(persona,
                        core.getTrustService().getLevel(persona.getDestination()),
                        first.getBrowse(),
                        browseManager.isBrowsing(persona),
                        resultsFromSender.size(),
                        first.getFeed(),
                        core.getFeedManager().getFeed(persona) != null);
                senders.add(sender);
            });
            
            SENDER_COMPARATORS.sort(senders, req);
            
            sb.append("<Senders>");
            senders.forEach(sender -> sender.toXML(sb));
            sb.append("</Senders>");
        } else if (section.equals("resultsFromSender")) {
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
            
            String senderB64 = req.getParameter("sender");
            Persona sender;
            try {
                sender = new Persona(new ByteArrayInputStream(Base64.decode(senderB64)));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            SearchResults searchResults = searchManager.getResults().get(uuid);
            Set<UIResultEvent> results = searchResults.getBySender().get(sender);
            List<ResultFromSender> resultsFromSender = new ArrayList<>();
            results.forEach(result -> {
                ResultStatus resultStatus = ResultStatus.AVAILABLE;
                if (core.getFileManager().getRootToFiles().containsKey(result.getInfohash()))
                    resultStatus = ResultStatus.SHARED;
                else if (downloadManager.isDownloading(result.getInfohash()))
                    resultStatus = ResultStatus.DOWNLOADING;
                ResultFromSender resultFromSender = new ResultFromSender(result,
                        resultStatus);
                resultsFromSender.add(resultFromSender);
            });
            
            RESULT_FROM_SENDER_COMPARATORS.sort(resultsFromSender, req);
            
            sb.append("<ResultsFromSender>");
            resultsFromSender.forEach(result -> result.toXML(sb));
            sb.append("</ResultsFromSender>");
        } else if (section.equals("results")) {
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
            SearchResults searchResults = searchManager.getResults().get(uuid);
            if (searchResults == null)
                return;
            Map<InfoHash, Set<UIResultEvent>> byInfohash = searchResults.getByInfoHash();
            
            List<Result> results = new ArrayList<>();
            byInfohash.forEach( (infoHash, resultSet) -> {
                UIResultEvent event = resultSet.iterator().next();
                ResultStatus resultStatus = ResultStatus.AVAILABLE;
                if (core.getFileManager().getRootToFiles().containsKey(infoHash))
                    resultStatus = ResultStatus.SHARED;
                else if (downloadManager.isDownloading(infoHash))
                    resultStatus = ResultStatus.DOWNLOADING;
                Result result = new Result(event.getName(),
                        event.getSize(),
                        resultStatus,
                        resultSet.size(),
                        infoHash);
                results.add(result);
            });
            
            RESULT_COMPARATORS.sort(results, req);
            
            sb.append("<Results>");
            results.forEach(result -> result.toXML(sb));
            sb.append("</Results>");
        } else if (section.equals("sendersForResult")) {
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
            SearchResults searchResults = searchManager.getResults().get(uuid);
            if (searchResults == null)
                return;
            
            String infoHashB64 = req.getParameter("infoHash");
            InfoHash infoHash;
            try {
                infoHash = new InfoHash(Base64.decode(infoHashB64));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            Set<UIResultEvent> resultSet = searchResults.getByInfoHash(infoHash);
            if (resultSet == null) 
                return;
            
            List<SenderForResult> sendersForResult = new ArrayList<>();
            resultSet.forEach(event -> {
                SenderForResult senderForResult = new SenderForResult(event.getSender(),
                        event.getBrowse(),
                        browseManager.isBrowsing(event.getSender()),
                        event.getComment(),
                        event.getCertificates(),
                        core.getTrustService().getLevel(event.getSender().getDestination()),
                        event.getFeed(),
                        core.getFeedManager().getFeed(event.getSender()) != null);
                sendersForResult.add(senderForResult);
            });
            
            SENDER_FOR_RESULT_COMPARATORS.sort(sendersForResult, req);
            
            sb.append("<Senders>");
            sendersForResult.forEach(sender -> sender.toXML(sb));
            sb.append("</Senders>");
            
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
    
    private static class Sender {
        private final Persona persona;
        private final TrustLevel trustLevel;
        private final boolean browse;
        private final boolean browsing;
        private final int results;
        private final boolean feed;
        private final boolean subscribed;
        
        Sender(Persona persona, TrustLevel trustLevel, boolean browse, boolean browsing, int results,
                boolean feed, boolean subscribed) {
            this.persona = persona;
            this.trustLevel = trustLevel;
            this.browse = browse;
            this.browsing = browsing;
            this.results = results;
            this.feed = feed;
            this.subscribed = subscribed;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Sender>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(persona.getHumanReadableName())).append("</Name>");
            sb.append("<B64>").append(persona.toBase64()).append("</B64>");
            sb.append("<Trust>").append(trustLevel.toString()).append("</Trust>");
            sb.append("<TrustString>").append(Util._t(EnumStrings.TRUST_LEVELS.get(trustLevel))).append("</TrustString>");
            sb.append("<Browse>").append(browse).append("</Browse>");
            sb.append("<Browsing>").append(browsing).append("</Browsing>");
            sb.append("<Results>").append(results).append("</Results>");
            sb.append("<Feed>").append(feed).append("</Feed>");
            sb.append("<Subscribed>").append(subscribed).append("</Subscribed>");
            sb.append("</Sender>");
        }
    }
    
    private static class ResultFromSender {
        private final String name;
        private final long size;
        private final InfoHash infoHash;
        private final ResultStatus resultStatus;
        private final String comment;
        private final int certificates;
        
        ResultFromSender(UIResultEvent e, ResultStatus resultStatus) {
            this.name = e.getName();
            this.size = e.getSize();
            this.infoHash = e.getInfohash();
            this.resultStatus = resultStatus;
            this.comment = e.getComment();
            this.certificates = e.getCertificates();
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Result>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(name)).append("</Name>");
            sb.append("<Size>").append(DataHelper.formatSize2Decimal(size, false)).append("B").append("</Size>");
            sb.append("<InfoHash>").append(Base64.encode(infoHash.getRoot())).append("</InfoHash>");
            sb.append("<ResultStatus>").append(resultStatus).append("</ResultStatus>");
            sb.append("<ResultStatusString>").append(Util._t(EnumStrings.RESULT_STATES.get(resultStatus))).append("</ResultStatusString>");
            if (comment != null)
                sb.append("<Comment>").append(Util.escapeHTMLinXML(comment)).append("</Comment>");
            sb.append("<Certificates>").append(certificates).append("</Certificates>");
            sb.append("</Result>");
        }
    }
    
    private static class Result {
        private final String name;
        private final long size;
        private final ResultStatus resultStatus;
        private final int sources;
        private final InfoHash infoHash;
        
        Result(String name, long size, ResultStatus resultStatus, int sources, InfoHash infoHash) {
            this.name = name;
            this.size = size;
            this.resultStatus = resultStatus;
            this.infoHash = infoHash;
            this.sources = sources;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Result>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(name)).append("</Name>");
            sb.append("<Size>").append(DataHelper.formatSize2Decimal(size, false)).append("B").append("</Size>");
            sb.append("<InfoHash>").append(Base64.encode(infoHash.getRoot())).append("</InfoHash>");
            sb.append("<ResultStatus>").append(resultStatus).append("</ResultStatus>");
            sb.append("<ResultStatusString>").append(Util._t(EnumStrings.RESULT_STATES.get(resultStatus))).append("</ResultStatusString>");
            sb.append("<Sources>").append(sources).append("</Sources>");
            sb.append("</Result>");
        }
    }
    
    
    private static class SenderForResult {
        private final Persona sender;
        private final boolean browse;
        private final boolean browsing;
        private final String comment;
        private final int certificates;
        private final TrustLevel trustLevel;
        private final boolean feed;
        private final boolean subscribed;
        
        SenderForResult(Persona sender, boolean browse, boolean browsing, String comment, int certificates, TrustLevel trustLevel,
                boolean feed, boolean subscribed) {
            this.sender = sender;
            this.browse = browse;
            this.trustLevel = trustLevel;
            this.browsing = browsing;
            this.comment = comment;
            this.certificates = certificates;
            this.feed = feed;
            this.subscribed = subscribed;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Sender>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(sender.getHumanReadableName())).append("</Name>");
            sb.append("<B64>").append(sender.toBase64()).append("</B64>");
            sb.append("<Browse>").append(browse).append("</Browse>");
            sb.append("<Trust>").append(trustLevel.toString()).append("</Trust>");
            sb.append("<TrustString>").append(Util._t(EnumStrings.TRUST_LEVELS.get(trustLevel))).append("</TrustString>");
            sb.append("<Browsing>").append(browsing).append("</Browsing>");
            if (comment != null)
                sb.append("<Comment>").append(Util.escapeHTMLinXML(comment)).append("</Comment>");
            sb.append("<Certificates>").append(certificates).append("</Certificates>");
            sb.append("<Feed>").append(feed).append("</Feed>");
            sb.append("<Subscribed>").append(subscribed).append("</Subscribed>");
            sb.append("</Sender>");
        }
        
    }
    
    private static final Comparator<SearchResults> SEARCH_BY_NAME = (k, v) -> {
        return Collator.getInstance().compare(k.getSearch(), v.getSearch());
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
        return Collator.getInstance().compare(k.persona.getHumanReadableName(), v.persona.getHumanReadableName());
    };
    
    private static final Comparator<Sender> SENDER_BY_TRUST = (k, v) -> {
        return Collator.getInstance().compare(k.trustLevel.toString(), v.trustLevel.toString());
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
    
    private static final Comparator<ResultFromSender> RESULT_FROM_SENDER_BY_NAME = (k, v) -> {
        return Collator.getInstance().compare(k.name, v.name);
    };
    
    private static final Comparator<ResultFromSender> RESULT_FROM_SENDER_BY_SIZE = (k, v) -> {
        return Long.compare(k.size, v.size);
    };
    
    private static final Comparator<ResultFromSender> RESULT_FROM_SENDER_BY_STATUS = (k, v) -> {
        return Collator.getInstance().compare(k.resultStatus.toString(), v.resultStatus.toString());
    };
    
    private static final ColumnComparators<ResultFromSender> RESULT_FROM_SENDER_COMPARATORS = new ColumnComparators<>();
    static {
        RESULT_FROM_SENDER_COMPARATORS.add("Name", RESULT_FROM_SENDER_BY_NAME);
        RESULT_FROM_SENDER_COMPARATORS.add("Size", RESULT_FROM_SENDER_BY_SIZE);
        RESULT_FROM_SENDER_COMPARATORS.add("Download", RESULT_FROM_SENDER_BY_STATUS);
    }
    
    private static final Comparator<Result> RESULT_BY_NAME = (k, v) -> {
        return Collator.getInstance().compare(k.name, v.name);
    };
    
    private static final Comparator<Result> RESULT_BY_SIZE = (k, v) -> {
        return Long.compare(k.size, v.size);
    };
    
    private static final Comparator<Result> RESULT_BY_STATUS = (k, v) -> {
        return Collator.getInstance().compare(k.resultStatus.toString(), v.resultStatus.toString());
    };
    
    private static final Comparator<Result> RESULT_BY_SOURCES = (l, r) -> {
        return Integer.compare(l.sources, r.sources);
    };
    
    private static final ColumnComparators<Result> RESULT_COMPARATORS = new ColumnComparators<>();
    static {
        RESULT_COMPARATORS.add("Name", RESULT_BY_NAME);
        RESULT_COMPARATORS.add("Size", RESULT_BY_SIZE);
        RESULT_COMPARATORS.add("Download", RESULT_BY_STATUS);
        RESULT_COMPARATORS.add("Sources", RESULT_BY_SOURCES);
    }

    private static final Comparator<SenderForResult> SENDER_FOR_RESULT_BY_SENDER = (k, v) -> {
        return Collator.getInstance().compare(k.sender.getHumanReadableName(), v.sender.getHumanReadableName());
    };
    
    private static final Comparator<SenderForResult> SENDER_FOR_RESULT_BY_BROWSING = (k, v) -> {
        return Boolean.compare(k.browsing, v.browsing);
    };
    
    private static final Comparator<SenderForResult> SENDER_FOR_RESULT_BY_TRUST = (k, v) -> {
        return Collator.getInstance().compare(k.trustLevel.toString(), v.trustLevel.toString());
    };
    
    private static final ColumnComparators<SenderForResult> SENDER_FOR_RESULT_COMPARATORS = new ColumnComparators<>();
    static {
        SENDER_FOR_RESULT_COMPARATORS.add("Sender", SENDER_FOR_RESULT_BY_SENDER);
        SENDER_FOR_RESULT_COMPARATORS.add("Browse", SENDER_FOR_RESULT_BY_BROWSING);
        SENDER_FOR_RESULT_COMPARATORS.add("Trust", SENDER_FOR_RESULT_BY_TRUST);
    }
}
