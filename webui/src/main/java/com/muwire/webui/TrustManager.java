package com.muwire.webui;

import com.muwire.core.trust.TrustEvent;
import com.muwire.core.trust.TrustSubscriptionUpdatedEvent;

public class TrustManager {

    private volatile long revision;
    
    public long getRevision() {
        return revision;
    }
    
    public void onTrustEvent(TrustEvent e) {
        revision++;
    }
    
    public void onTrustSubscriptionUpdatedEvent(TrustSubscriptionUpdatedEvent e) {
        revision++;
    }
}
