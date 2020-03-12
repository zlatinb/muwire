package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.filefeeds.Feed;
import com.muwire.core.filefeeds.FeedItem;
import com.muwire.core.filefeeds.UIDownloadFeedItemEvent;
import com.muwire.core.util.DataUtil;
import com.muwire.webui.FeedManager.RemoteFeed;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class FeedServlet extends HttpServlet {
    
    private FeedManager feedManager;
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
            List<WrappedFeed> feeds = feedManager.getRemoteFeeds().values().stream().
                    map(rf -> new WrappedFeed(rf, core.getFeedManager().getFeedItems(rf.getFeed().getPublisher()).size())).
                    collect(Collectors.toList());
            FEED_COMPARATORS.sort(feeds, req);
            sb.append("<Status>");
            feeds.forEach(f -> f.toXML(sb));
            sb.append("</Status>");
        } else if (section.equals("items")) {
            String publisherB64 = req.getParameter("publisher");
            if (publisherB64 == null) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            Persona publisher;
            try {
                publisher = new Persona(new ByteArrayInputStream(Base64.decode(publisherB64)));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            RemoteFeed feed = feedManager.getRemoteFeeds().get(publisher);
            if (feed == null)
                return; // hmm
            
            List<WrappedFeedItem> items = core.getFeedManager().getFeedItems(publisher).stream().
                    map(item -> {
                        ResultStatus resultStatus = ResultStatus.AVAILABLE;
                        if (core.getFileManager().isShared(item.getInfoHash()))
                            resultStatus = ResultStatus.SHARED;
                        else if (core.getDownloadManager().isDownloading(item.getInfoHash()))
                            resultStatus = ResultStatus.DOWNLOADING;
                        return new WrappedFeedItem(item, resultStatus);
                    }).collect(Collectors.toList());
            
            ITEM_COMPARATORS.sort(items, req);
            
            sb.append("<Items>");
            items.forEach(i -> i.toXML(sb));
            sb.append("</Items>");
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(403,"Bad param");
            return;
        }
        if (action.equals("subscribe")) {
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
            feedManager.subscribe(host);
            Util.pause();
            resp.sendRedirect("/MuWire/Feeds");
        } else if (action.equals("unsubscribe")) {
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
            feedManager.unsubscribe(host);
            Util.pause();
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
            
            Feed feed = core.getFeedManager().getFeed(host);
            if (feed == null)
                return;
            
            Optional<FeedItem> itemOptional = core.getFeedManager().getFeedItems(host).
                    stream().filter(item -> item.getInfoHash().equals(infoHash)).findFirst();
            if (!itemOptional.isPresent())
                return;
            FeedItem item = itemOptional.get();
            
            File target = new File(core.getMuOptions().getDownloadLocation(), item.getName());
            
            UIDownloadFeedItemEvent event = new UIDownloadFeedItemEvent();
            event.setItem(item);
            event.setSequential(feed.isSequential());
            event.setTarget(target);
            core.getEventBus().publish(event);
            Util.pause();
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        feedManager = (FeedManager) config.getServletContext().getAttribute("feedManager");
        core = (Core) config.getServletContext().getAttribute("core");
    }
    
    private static final Comparator<WrappedFeed> FEED_BY_PUBLISHER = (l, r) -> {
        return Collator.getInstance().compare(l.feed.getPublisher().getHumanReadableName(), r.feed.getPublisher().getHumanReadableName());
    };
    
    private static final Comparator<WrappedFeed> FEED_BY_FILES = (l, r) -> {
        return Integer.compare(l.files, r.files);
    };
    
    private static final Comparator<WrappedFeed> FEED_BY_LAST_UPDATED = (l, r) -> {
        return Long.compare(l.feed.getLastUpdated(), r.feed.getLastUpdated());
    };
    
    private static final Comparator<WrappedFeed> FEED_BY_STATUS = (l, r) -> {
        return Collator.getInstance().compare(l.feed.getStatus().toString(), r.feed.getStatus().toString());
    };
    
    private static final ColumnComparators<WrappedFeed> FEED_COMPARATORS = new ColumnComparators<>();
    static {
        FEED_COMPARATORS.add("publisher", FEED_BY_PUBLISHER);
        FEED_COMPARATORS.add("files", FEED_BY_FILES);
        FEED_COMPARATORS.add("status", FEED_BY_STATUS);
        FEED_COMPARATORS.add("lastUpdated", FEED_BY_LAST_UPDATED);
    }
    
    private static final Comparator<WrappedFeedItem> ITEM_BY_NAME = (l, r) -> {
        return Collator.getInstance().compare(l.feedItem.getName(), r.feedItem.getName());
    };
    
    private static final Comparator<WrappedFeedItem> ITEM_BY_SIZE = (l, r) -> {
        return Long.compare(l.feedItem.getSize(), r.feedItem.getSize());
    };
    
    private static final Comparator<WrappedFeedItem> ITEM_BY_STATUS = (l, r) -> {
        return Collator.getInstance().compare(l.resultStatus.toString(), r.resultStatus.toString());
    };
    
    private static final Comparator<WrappedFeedItem> ITEM_BY_TIMESTAMP = (l, r) -> {
        return Long.compare(l.feedItem.getTimestamp(), r.feedItem.getTimestamp());
    };
    
    private static final ColumnComparators<WrappedFeedItem> ITEM_COMPARATORS = new ColumnComparators<>();
    static {
        ITEM_COMPARATORS.add("name", ITEM_BY_NAME);
        ITEM_COMPARATORS.add("size", ITEM_BY_SIZE);
        ITEM_COMPARATORS.add("status", ITEM_BY_STATUS);
        ITEM_COMPARATORS.add("timestamp", ITEM_BY_TIMESTAMP);
    }

    private static class WrappedFeedItem {
        private final FeedItem feedItem;
        private final ResultStatus resultStatus;
        WrappedFeedItem(FeedItem feedItem, ResultStatus resultStatus) {
            this.feedItem = feedItem;
            this.resultStatus = resultStatus;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Item>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(feedItem.getName())).append("</Name>");
            sb.append("<ResultStatus>").append(resultStatus).append("</ResultStatus>");
            sb.append("<Size>").append(DataHelper.formatSize2Decimal(feedItem.getSize(), false)).append("</Size>");
            sb.append("<Timestamp>").append(DataHelper.formatTime(feedItem.getTimestamp())).append("</Timestamp>");
            sb.append("<InfoHash>").append(Base64.encode(feedItem.getInfoHash().getRoot())).append("</InfoHash>");
            sb.append("<Certificates>").append(feedItem.getCertificates()).append("</Certificates>");
            if (feedItem.getComment() != null)
                sb.append("<Comment>").append(Util.escapeHTMLinXML(DataUtil.readi18nString(Base64.decode(feedItem.getComment())))).append("</Comment>");
            sb.append("</Item>");
        }
    }
    
    private static class WrappedFeed {
        private final Feed feed;
        private final long revision;
        private final int files;
        WrappedFeed(RemoteFeed rf, int files) {
            this.feed = rf.getFeed();
            this.revision = rf.getRevision();
            this.files = files;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Feed>");
            sb.append("<Publisher>").append(Util.escapeHTMLinXML(feed.getPublisher().getHumanReadableName())).append("</Publisher>");
            sb.append("<PublisherB64>").append(feed.getPublisher().toBase64()).append("</PublisherB64>");
            sb.append("<Files>").append(files).append("</Files>");
            sb.append("<Revision>").append(revision).append("</Revision>");
            sb.append("<Status>").append(feed.getStatus().toString()).append("</Status>");
            sb.append("<Active>").append(feed.getStatus().isActive()).append("</Active>");
            sb.append("<LastUpdated>").append(DataHelper.formatTime(feed.getLastUpdated())).append("</LastUpdated>");
            sb.append("</Feed>");
        }
    }
}
