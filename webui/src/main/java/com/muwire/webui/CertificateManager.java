package com.muwire.webui;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.filecert.Certificate;
import com.muwire.core.filecert.CertificateFetchEvent;
import com.muwire.core.filecert.CertificateFetchStatus;
import com.muwire.core.filecert.CertificateFetchedEvent;
import com.muwire.core.filecert.UIFetchCertificatesEvent;
import com.muwire.core.filecert.UIImportCertificateEvent;

public class CertificateManager {
    private final Core core;
    
    
    private final Map<Persona, Map<InfoHash,CertificateRequest>> requests = new ConcurrentHashMap<>();
    
    public CertificateManager(Core core) {
        this.core = core;
    }
    
    public void onCertificateFetchEvent(CertificateFetchEvent e) {
        Map<InfoHash, CertificateRequest> map = requests.get(e.getUser());
        if (map == null)
            return;
        CertificateRequest request = map.get(e.getInfoHash());
        if (request == null)
            return;
        request.status = e.getStatus();
        if (request.status == CertificateFetchStatus.FETCHING)
            request.totalCertificates = e.getCount();
    }
    
    public void onCertificateFetchedEvent(CertificateFetchedEvent e) {
        Map<InfoHash, CertificateRequest> map = requests.get(e.getUser());
        if (map == null)
            return;
        CertificateRequest request = map.get(e.getInfoHash());
        if (request == null)
            return;
        request.certificates.add(e.getCertificate());
    }
    
    void request(Persona user, InfoHash infoHash) {
        CertificateRequest request = new CertificateRequest(user, infoHash);
        Map<InfoHash, CertificateRequest> requestsFromUser = requests.get(user);
        if (requestsFromUser == null) {
            requestsFromUser = new ConcurrentHashMap<>();
            requests.put(user, requestsFromUser);
        }
        requestsFromUser.put(infoHash, request);
        
        UIFetchCertificatesEvent event = new UIFetchCertificatesEvent();
        event.setHost(user);
        event.setInfoHash(infoHash);
        core.getEventBus().publish(event);
    }
    
    CertificateRequest get(Persona user, InfoHash infoHash) {
        Map<InfoHash, CertificateRequest> map = requests.get(user);
        if (map == null)
            return null;
        return map.get(infoHash);
    }
    
    void importCertificate(Certificate certificate) {
        UIImportCertificateEvent event = new UIImportCertificateEvent();
        event.setCertificate(certificate);
        core.getEventBus().publish(event);
    }
    
    static class CertificateRequest {
        private final Persona user;
        private final InfoHash infoHash;
        private volatile CertificateFetchStatus status;
        private volatile int totalCertificates;
        private Set<Certificate> certificates;
        
        CertificateRequest(Persona user, InfoHash infoHash) {
            this.user = user;
            this.infoHash = infoHash;
        }
        
        CertificateFetchStatus getStatus() {
            return status;
        }
        
        int totalCertificates() {
            return totalCertificates;
        }
        
        Set<Certificate> getCertificates() {
            return certificates;
        }
    }
}
