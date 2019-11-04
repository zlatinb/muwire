package com.muwire.core.filecert

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.InvalidSignatureException
import com.muwire.core.Name
import com.muwire.core.Persona

import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.SigningPrivateKey

@Log
class CertificateManager {
    
    private final EventBus eventBus
    private final File certDir
    private final Persona me
    private final SigningPrivateKey spk
    
    final Map<InfoHash, Set<Certificate>> byInfoHash = new ConcurrentHashMap()
    final Map<Persona, Set<Certificate>> byIssuer = new ConcurrentHashMap()
    
    CertificateManager(EventBus eventBus, File home, Persona me, SigningPrivateKey spk) {
        this.eventBus = eventBus
        this.me = me
        this.spk = spk
        this.certDir = new File(home, "filecerts")
        if (!certDir.exists())
            certDir.mkdirs()
        else
            loadCertificates()
    }
    
    private void loadCertificates() {
        certDir.listFiles({ dir, name ->
            name.endsWith("mwcert")
        } as FilenameFilter).each { certFile ->
            Certificate cert = null
            try {
                certFile.withInputStream {
                    cert = new Certificate(it)
                }
            } catch (IOException | InvalidSignatureException ignore) {
                log.log(Level.WARNING, "Certificate failed to load from $certFile", ignore)
                return
            }
            
            Set<Certificate> existing = byInfoHash.get(cert.infoHash)
            if (existing == null) {
                existing = new HashSet<>()
                byInfoHash.put(cert.infoHash, existing)
            }
            existing.add(cert)
            
            existing = byIssuer.get(cert.issuer)
            if (existing == null) {
                existing = new HashSet<>()
                byIssuer.put(cert.issuer, existing)
            }
            existing.add(cert)
            
            eventBus.publish(new CertificateCreatedEvent(certificate : cert))
        }
    }
    
    void onUICreateCertificateEvent(UICreateCertificateEvent e) {
        InfoHash infoHash = e.sharedFile.getInfoHash()
        String name = e.sharedFile.getFile().getName()
        long timestamp = System.currentTimeMillis()
        Certificate cert = new Certificate(infoHash, name, timestamp, me, spk)
        
        boolean added = true
        
        Set<Certificate> existing = byInfoHash.get(cert.infoHash)
        if (existing == null) {
            existing = new HashSet<>()
            byInfoHash.put(cert.infoHash, existing)
        }
        added &= existing.add(cert)
        
        existing = byIssuer.get(cert.issuer)
        if (existing == null) {
            existing = new HashSet<>()
            byIssuer.put(cert.issuer, existing)
        }
        added &= existing.add(cert)
        
        if (added) {
            String infoHashString = Base64.encode(infoHash.getRoot())
            File certFile = new File(certDir, "${infoHashString}${name}.mwcert")
            certFile.withOutputStream { cert.write(it) }
            eventBus.publish(new CertificateCreatedEvent(certificate : cert))
        }
    }

    boolean hasLocalCertificate(InfoHash infoHash) {
        if (!byInfoHash.containsKey(infoHash))
            return false
        Set<Certificate> set = byInfoHash.get(infoHash)
        for (Certificate cert : set) {
            if (cert.issuer == me)
                return true
        }
        return false
    }    
}
