package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollection
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
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.table.DefaultTableModel

@ArtifactProviderFor(GriffonModel)
class ResultDetailsModel {
    
    @MVCMember @Nonnull
    ResultDetailsView view
    
    @Observable boolean browseActionEnabled
    @Observable boolean copyIdActionEnabled

    Core core
    String fileName
    InfoHash infoHash
    List<UIResultEvent> results

    String key
    List<SharedFile> localFiles
    
    Map<Persona, CertsModel> certificates
    Map<Persona, List<FileCollection>> collections
    
    private boolean registeredForCertificates, registeredForCollections
    
    void mvcGroupInit(Map<String,String> args) {
        key = fileName + Base64.encode(infoHash.getRoot())
        SharedFile[] locals = core.fileManager.getSharedFiles(infoHash.getRoot())
        if (locals == null)
            localFiles = Collections.emptyList()
        else
            localFiles = Arrays.asList(locals)
        
        certificates = new HashMap<>()
        collections = new HashMap<>()
    }
    
    void mvcGroupDestroy() {
        if (registeredForCertificates) {
            core.eventBus.unregister(CertificateFetchEvent.class, this)
            core.eventBus.unregister(CertificateFetchedEvent.class, this)
        }
    }
    
    void registerForCollections(Persona sender) {
        if (registeredForCollections)
            return
        registeredForCollections = true
        // TODO: finish
    }
    
    CertsModel registerForCertificates(Persona persona) {
        if (certificates.containsKey(persona))
            return null
        if (!registeredForCertificates) {
            registeredForCertificates = true
            core.eventBus.with {
                register(CertificateFetchEvent.class, this)
                register(CertificateFetchedEvent.class, this)
            }
        }
        def rv = new CertsModel()
        certificates.put(persona, rv)
        core.eventBus.publish(new UIFetchCertificatesEvent(host: persona, infoHash: infoHash))
        return rv
    }
    
    void onCertificateFetchEvent(CertificateFetchEvent event) {
        if (event.infoHash != infoHash)
            return
        runInsideUIAsync {
            CertsModel model = certificates.get(event.user)
            if (model == null)
                return
            model.status = event.status
            if (event.status == CertificateFetchStatus.FETCHING)
                model.count = event.count
            view.refreshCertificates()
        }
    }
    
    void onCertificateFetchedEvent(CertificateFetchedEvent event) {
        if (event.infoHash != infoHash)
            return
        runInsideUIAsync {
            CertsModel model = certificates.get(event.user)
            if (model == null)
                return
            model.certificates << event.certificate
            view.refreshCertificates()
        }
    }
    
    static class CertsModel {
        CertificateFetchStatus status
        int count
        final List<Certificate> certificates = new ArrayList<>()
    }
}
