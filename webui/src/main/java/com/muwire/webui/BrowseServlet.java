package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.download.UIDownloadEvent;
import com.muwire.core.search.UIResultEvent;
import com.muwire.webui.BrowseManager.Browse;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;
import net.i2p.data.Destination;

public class BrowseServlet extends HttpServlet {
    
    private BrowseManager browseManager;
    private DownloadManager downloadManager;
    private Core core;

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
            
            List<Browse> browses = new ArrayList<>(browseManager.getBrowses().values());
            BROWSE_COMPARATORS.sort(browses, req);
            
            sb.append("<Status>");
            browses.forEach( browse -> {
                sb.append("<Browse>");
                sb.append("<Host>").append(Util.escapeHTMLinXML(browse.getHost().getHumanReadableName())).append("</Host>");
                sb.append("<HostB64>").append(browse.getHost().toBase64()).append("</HostB64>");
                sb.append("<BrowseStatus>").append(browse.getStatus()).append("</BrowseStatus>");
                sb.append("<TotalResults>").append(browse.getTotalResults()).append("</TotalResults>");
                sb.append("<ResultsCount>").append(browse.getResults().size()).append("</ResultsCount>");
                sb.append("<Revision>").append(browse.getRevision()).append("</Revision>");
                sb.append("</Browse>");
            });
            sb.append("</Status>");
        } else if (section.equals("results")) {
            String hostB64 = req.getParameter("host");
            if (hostB64 == null) {
                resp.sendError(403,"Bad param");
                return;
            }
            Persona host;
            try {
                host = new Persona(new ByteArrayInputStream(Base64.decode(hostB64)));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            Browse browse = browseManager.getBrowses().get(host);
            if (browse == null)
                return; // hmm
            
            List<Result> wrapped = browse.getResults().stream().map(event -> {
                ResultStatus resultStatus = ResultStatus.AVAILABLE;
                if (core.getFileManager().getRootToFiles().containsKey(event.getInfohash()))
                    resultStatus = ResultStatus.SHARED;
                else if (downloadManager.isDownloading(event.getInfohash()))
                    resultStatus = ResultStatus.DOWNLOADING;
                return new Result(event, resultStatus);
            }).collect(Collectors.toList());

            RESULT_COMPARATORS.sort(wrapped, req);
            
            sb.append("<Results>");
            wrapped.stream().forEach(resultWrapper -> {
                UIResultEvent result = resultWrapper.getEvent();
                sb.append("<Result>");
                sb.append("<Name>").append(Util.escapeHTMLinXML(result.getName())).append("</Name>");
                sb.append("<ResultStatus>").append(resultWrapper.resultStatus).append("</ResultStatus>");
                sb.append("<Size>").append(DataHelper.formatSize2Decimal(result.getSize(), false)).append("B").append("</Size>");
                sb.append("<InfoHash>").append(Base64.encode(result.getInfohash().getRoot())).append("</InfoHash>");
                if (result.getComment() != null) {
                    sb.append("<Comment>").append(Util.escapeHTMLinXML(result.getComment())).append("</Comment>");
                }
                sb.append("<Certificates>").append(result.getCertificates()).append("</Certificates>");
                // TODO: add more fields
                sb.append("</Result>");
            });
            sb.append("</Results>");
        } else {
            resp.sendError(403, "Bad param");
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(403,"Bad param");
            return;
        }
        if (action.equals("browse")) {
            String personaB64 = req.getParameter("host");
            if (personaB64 == null) {
                resp.sendError(403,Util._t("Please enter a full MuWire id"));
                return;
            }
            Persona host;
            try {
                host = new Persona(new ByteArrayInputStream(Base64.decode(personaB64)));
            } catch (Exception bad) {
                resp.sendError(403,Util._t("Please enter a full MuWire id"));
                return;
            }
            browseManager.browse(host);
            pause();
            resp.sendRedirect("/MuWire/BrowseHost");
        } else if (action.equals("download")) {
            if (core == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
            String personaB64 = req.getParameter("host");
            if (personaB64 == null) {
                resp.sendError(403,"Bad param");
                return;
            }
            Persona host;
            try {
                host = new Persona(new ByteArrayInputStream(Base64.decode(personaB64)));
            } catch (Exception bad) {
                resp.sendError(403,"Bad param");
                return;
            }
            String infoHashB64 = req.getParameter("infoHash");
            if (infoHashB64 == null) {
                resp.sendError(403, "Bad param");
                return;
            }
            final InfoHash infoHash;
            try {
                infoHash = new InfoHash(Base64.decode(infoHashB64));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            Browse browse = browseManager.getBrowses().get(host);
            if (browse == null)
                return;
            
            Set<UIResultEvent> results = browse.getResults().stream().filter(e -> e.getInfohash().equals(infoHash)).
                    collect(Collectors.toSet());
            
            if (results.isEmpty())
                return;
            
            UIDownloadEvent event = new UIDownloadEvent();
            UIResultEvent[] resultsArray = results.toArray(new UIResultEvent[0]);
            event.setResult(resultsArray);
            // TODO: sequential
            Set<Destination> possibleSources = results.stream().flatMap(e -> e.getSources().stream()).collect(Collectors.toSet());
            event.setSources(possibleSources);
            event.setTarget(new File(core.getMuOptions().getDownloadLocation(), resultsArray[0].getName()));
            core.getEventBus().publish(event);
            pause();
        } else if (action.equals("close")) {
            String personaB64 = req.getParameter("host");
            if (personaB64 == null) {
                resp.sendError(403,"Bad param");
                return;
            }
            Persona host;
            try {
                host = new Persona(new ByteArrayInputStream(Base64.decode(personaB64)));
            } catch (Exception bad) {
                resp.sendError(403,"Bad param");
                return;
            }
            browseManager.getBrowses().remove(host);
        }
    }
    
    private static void pause() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {}
    }

    @Override
    public void init(ServletConfig cfg) throws ServletException {
        browseManager = (BrowseManager) cfg.getServletContext().getAttribute("browseManager");
        downloadManager = (DownloadManager) cfg.getServletContext().getAttribute("downloadManager");
        core = (Core) cfg.getServletContext().getAttribute("core");
    }
    
    private static class Result {
        private final UIResultEvent event;
        private final ResultStatus resultStatus;
        Result(UIResultEvent event, ResultStatus resultStatus) {
            this.event = event;
            this.resultStatus = resultStatus;
        }
        
        UIResultEvent getEvent() {
            return event;
        }
    }
    
    private static final Comparator<Result> BY_NAME = (k, v) -> {
        return Collator.getInstance().compare(k.event.getName(), v.event.getName());
    };
    
    private static final Comparator<Result> BY_SIZE = (k, v) -> {
        return Long.compare(k.event.getSize(), v.event.getSize());  
    };

    private static final Comparator<Result> BY_RESULT_STATUS = (k, v) -> {
        return Collator.getInstance().compare(k.resultStatus.toString(), v.resultStatus.toString());
    };
    
    private static final ColumnComparators<Result> RESULT_COMPARATORS = new ColumnComparators<>();
    static {
        RESULT_COMPARATORS.add("Name", BY_NAME);
        RESULT_COMPARATORS.add("Size", BY_SIZE);
        RESULT_COMPARATORS.add("Download", BY_RESULT_STATUS);
    }
    
    private static final Comparator<Browse> BY_HOST = (k, v) -> {
        return Collator.getInstance().compare(k.getHost().getHumanReadableName(), v.getHost().getHumanReadableName());
    };
    
    private static final Comparator<Browse> BY_STATUS = (k, v) -> {
        return Collator.getInstance().compare(k.getStatus().toString(), v.getStatus().toString());
    };
    
    private static final Comparator<Browse> BY_RESULTS = (k, v) -> {
        return Integer.compare(k.getResults().size(), v.getResults().size());
    };
    
    private static final ColumnComparators<Browse> BROWSE_COMPARATORS = new ColumnComparators<>(); 
    static {
        BROWSE_COMPARATORS.add("Host", BY_HOST);
        BROWSE_COMPARATORS.add("Status", BY_STATUS);
        BROWSE_COMPARATORS.add("Results", BY_RESULTS);
    }
}
