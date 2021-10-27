package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.collections.CollectionFetchStatus
import com.muwire.core.collections.CollectionFetchStatusEvent
import com.muwire.core.collections.CollectionFetchedEvent
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.UICollectionFetchEvent
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
import java.util.stream.Collectors

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
    
    private final Set<UIResultEvent> uniqueResults = new HashSet<>()
    List<UIResultEvent> resultsWithComments = []
    List<UIResultEvent> resultsWithCertificates = []
    List<UIResultEvent> resultsWithCollections = []
    
    
    Map<Persona, CertsModel> certificates
    Map<Persona, UUID> collectionFetches
    Map<UUID, CollectionsModel> collections
    
    private boolean registeredForCertificates, registeredForCollections
    
    void mvcGroupInit(Map<String,String> args) {
        key = fileName + Base64.encode(infoHash.getRoot())
        SharedFile[] locals = core.fileManager.getSharedFiles(infoHash.getRoot())
        if (locals == null)
            localFiles = Collections.emptyList()
        else
            localFiles = Arrays.asList(locals)
        
        certificates = new HashMap<>()
        collectionFetches = new HashMap<>()
        collections = new HashMap<>()
        
        uniqueResults.addAll(results)
        for (UIResultEvent event : results) {
            if (event.comment != null)
                resultsWithComments << event
            if (event.certificates > 0)
                resultsWithCertificates << event
            if (event.collections.size() > 0)
                resultsWithCollections << event
        }
    }
    
    void mvcGroupDestroy() {
        if (registeredForCertificates) {
            core.eventBus.unregister(CertificateFetchEvent.class, this)
            core.eventBus.unregister(CertificateFetchedEvent.class, this)
        }
        if (registeredForCollections) {
            core.eventBus.unregister(CollectionFetchedEvent.class, this)
            core.eventBus.unregister(CollectionFetchStatusEvent.class, this)
        }
    }
    
    void addResult(UIResultEvent event) {
        if (!uniqueResults.add(event))
            return
        results << event
        if (event.comment != null)
            resultsWithComments << event
        if (event.certificates > 0)
            resultsWithCertificates << event
        if (event.collections.size() > 0)
            resultsWithCollections << event
        view.refreshAll()
    }
    
    CollectionsModel registerForCollections(Persona sender) {
        if (collections.containsKey(sender))
            return null
        if (!registeredForCollections) {
            registeredForCollections = true
            core.eventBus.with {
                register(CollectionFetchStatusEvent.class, this)
                register(CollectionFetchedEvent.class, this)
            }
        }
        UUID uuid = UUID.randomUUID()
        collectionFetches.put(sender, uuid)
        def rv = new CollectionsModel()
        collections.put(uuid, rv)
        
        Set<InfoHash> infoHashes = results.stream().filter({it.sender == sender}).
            flatMap({it.collections.stream()}).collect(Collectors.toSet())
        UICollectionFetchEvent event = new UICollectionFetchEvent(uuid: uuid, host: sender, infoHashes: infoHashes)
        core.eventBus.publish(event)
        rv
    }
    
    void onCollectionFetchStatusEvent(CollectionFetchStatusEvent event) {
        runInsideUIAsync {
            def model = collections[event.uuid]
            if (model == null)
                return
            model.status = event.status
            if (event.status == CollectionFetchStatus.FETCHING)
                model.count = event.count
            view.refreshCollections()
        }
    }
    
    void onCollectionFetchedEvent(CollectionFetchedEvent event) {
        runInsideUIAsync {
            def model = collections[event.uuid]
            if (model == null)
                return
            model.collections << event.collection
            view.refreshCollections()
        }
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
    
    static class CollectionsModel {
        CollectionFetchStatus status
        int count
        final List<FileCollection> collections = new ArrayList<>()
    }
    
    boolean hasComments() {
        for (UIResultEvent event : results) {
            if (event.comment != null)
                return true
        }
        false
    }
}
