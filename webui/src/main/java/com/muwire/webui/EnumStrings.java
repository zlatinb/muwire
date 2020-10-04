package com.muwire.webui;

import java.util.EnumMap;
import java.util.Map;

import static com.muwire.webui.Util._x;
import com.muwire.core.download.Downloader;
import com.muwire.core.filecert.CertificateFetchStatus;
import com.muwire.core.filefeeds.FeedFetchStatus;
import com.muwire.core.search.BrowseStatus;
import com.muwire.core.trust.RemoteTrustList;
import com.muwire.core.trust.TrustLevel;

public class EnumStrings {

    private EnumStrings() {}
    
    public static final Map<Downloader.DownloadState, String> DOWNLOAD_STATES =
            new EnumMap<>(Downloader.DownloadState.class);
    static {
        DOWNLOAD_STATES.put(Downloader.DownloadState.CANCELLED, _x("Cancelled"));
        DOWNLOAD_STATES.put(Downloader.DownloadState.CONNECTING, _x("Connecting"));
        DOWNLOAD_STATES.put(Downloader.DownloadState.DOWNLOADING, _x("Downloading"));
        DOWNLOAD_STATES.put(Downloader.DownloadState.FAILED, _x("Failed"));
        DOWNLOAD_STATES.put(Downloader.DownloadState.FINISHED, _x("Finished"));
        DOWNLOAD_STATES.put(Downloader.DownloadState.HASHLIST, _x("Hash List"));
        DOWNLOAD_STATES.put(Downloader.DownloadState.HOPELESS, _x("Hopeless"));
        DOWNLOAD_STATES.put(Downloader.DownloadState.PAUSED, _x("Paused"));
    }
    
    public static final Map<BrowseStatus, String> BROWSE_STATES =
            new EnumMap<>(BrowseStatus.class);
    static {
        BROWSE_STATES.put(BrowseStatus.CONNECTING, _x("Connecting"));
        BROWSE_STATES.put(BrowseStatus.FAILED, _x("Failed"));
        BROWSE_STATES.put(BrowseStatus.FETCHING, _x("Fetching"));
        BROWSE_STATES.put(BrowseStatus.FINISHED, _x("Finished"));
    }
    
    public static final Map<ResultStatus, String> RESULT_STATES =
            new EnumMap<>(ResultStatus.class);
    static {
        RESULT_STATES.put(ResultStatus.AVAILABLE, _x("Available"));
        RESULT_STATES.put(ResultStatus.DOWNLOADING, _x("Downloading"));
        RESULT_STATES.put(ResultStatus.SHARED, _x("Shared"));
    }
    
    public static final Map<FeedFetchStatus, String> FEED_STATES =
            new EnumMap<>(FeedFetchStatus.class);
    static {
        FEED_STATES.put(FeedFetchStatus.CONNECTING, _x("Connecting"));
        FEED_STATES.put(FeedFetchStatus.FAILED, _x("Failed"));
        FEED_STATES.put(FeedFetchStatus.FETCHING, _x("Fetching"));
        FEED_STATES.put(FeedFetchStatus.FINISHED, _x("Finished"));
        FEED_STATES.put(FeedFetchStatus.IDLE, _x("Idle"));
    }
    
    public static final Map<CertificateFetchStatus, String> CERTIFICATE_STATES =
            new EnumMap<>(CertificateFetchStatus.class);
    static {
        CERTIFICATE_STATES.put(CertificateFetchStatus.CONNECTING, _x("Connecting"));
        CERTIFICATE_STATES.put(CertificateFetchStatus.DONE, _x("Done"));
        CERTIFICATE_STATES.put(CertificateFetchStatus.FAILED, _x("Failed"));
        CERTIFICATE_STATES.put(CertificateFetchStatus.FETCHING, _x("FETCHING"));
    }
    
    public static final Map<RemoteTrustList.Status, String> TRUST_LIST_STATES = 
            new EnumMap<>(RemoteTrustList.Status.class);
    static {
        TRUST_LIST_STATES.put(RemoteTrustList.Status.NEW, _x("New"));
        TRUST_LIST_STATES.put(RemoteTrustList.Status.UPDATE_FAILED, _x("Update Failed"));
        TRUST_LIST_STATES.put(RemoteTrustList.Status.UPDATED, _x("Updated"));
        TRUST_LIST_STATES.put(RemoteTrustList.Status.UPDATING, _x("Updating"));
    }
    
    public static final Map<TrustLevel, String> TRUST_LEVELS =
            new EnumMap<>(TrustLevel.class);
    static {
        TRUST_LEVELS.put(TrustLevel.TRUSTED, _x("Trusted"));
        TRUST_LEVELS.put(TrustLevel.NEUTRAL, _x("Neutral"));
        TRUST_LEVELS.put(TrustLevel.DISTRUSTED, _x("Distrusted"));
    }

}
