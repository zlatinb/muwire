package com.muwire.gui.resultdetails

import com.muwire.core.Core
import com.muwire.core.filecert.Certificate
import com.muwire.core.filecert.CertificateFetchEvent
import com.muwire.core.filecert.CertificateFetchStatus
import com.muwire.core.filecert.CertificateFetchedEvent
import com.muwire.core.filecert.UIFetchCertificatesEvent
import com.muwire.core.search.UIResultEvent
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import griffon.transform.Observable

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class CertificateTabModel {
    
    @MVCMember @Nonnull
    CertificateTabView view
    
    Core core
    UIResultEvent resultEvent
    
    
    final List<Certificate> certificates = new ArrayList<>()
    
    @Observable boolean importActionEnabled
    @Observable boolean viewCommentActionEnabled
    @Observable CertificateFetchStatus status
    @Observable int count
    @Observable int fetched
    
    private boolean registered
    
    void register() {
        registered = true
        core.eventBus.with {
            register(CertificateFetchEvent.class, this)
            register(CertificateFetchedEvent.class, this)
            publish(new UIFetchCertificatesEvent(host: resultEvent.sender, infoHash: resultEvent.infohash))
        }
    }
    
    void mvcGroupDestroy() {
        if (registered) {
            core.eventBus.with {
                unregister(CertificateFetchEvent.class, this)
                unregister(CertificateFetchedEvent.class, this)
            }
        }
    }

    void onCertificateFetchEvent(CertificateFetchEvent event) {
        if (event.infoHash != resultEvent.infohash || event.user != resultEvent.sender)
            return
        runInsideUIAsync {
            status = event.status
            if (event.status == CertificateFetchStatus.FETCHING)
                count = event.count
        }
    }

    void onCertificateFetchedEvent(CertificateFetchedEvent event) {
        if (event.infoHash != resultEvent.infohash || event.user != resultEvent.sender)
            return
        runInsideUIAsync {
            certificates << event.certificate
            fetched++
            view.refresh()
        }
    }
}
