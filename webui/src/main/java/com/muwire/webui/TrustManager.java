package com.muwire.webui;

import com.muwire.core.trust.TrustEvent;

public class TrustManager {

    private volatile long revision;
    
    public long getRevision() {
        return revision;
    }
    
    public void onTrustEvent(TrustEvent e) {
        revision++;
    }
}
