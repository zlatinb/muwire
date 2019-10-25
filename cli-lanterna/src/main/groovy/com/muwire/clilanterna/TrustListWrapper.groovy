package com.muwire.clilanterna

import com.muwire.core.trust.RemoteTrustList

class TrustListWrapper {
    private final RemoteTrustList trustList
    TrustListWrapper(RemoteTrustList trustList) {
        this.trustList = trustList
    }
    
    @Override
    public String toString() {
        trustList.persona.getHumanReadableName()
    }
}
