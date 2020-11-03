package com.muwire.core.messenger

import java.security.MessageDigest

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.Name
import com.muwire.core.Persona

import groovy.transform.CompileStatic
import net.i2p.crypto.DSAEngine
import net.i2p.data.Signature
import net.i2p.data.SigningPrivateKey

class MWMessage {

    final Persona sender
    final Set<Persona> recipients = new LinkedHashSet<>()
    final String subject
    final long timestamp
    final String body
    final Set<MWMessageAttachment> attachments = new LinkedHashSet<>()
    
    private final byte[] sig
    private final int hashCode
    private volatile byte[] payload
    private volatile InfoHash infoHash
    
    MWMessage(Persona sender, Set<Persona> recipients, String subject, long timestamp, String body, 
        Set<MWMessageAttachment> attachments, SigningPrivateKey spk) {
        this.sender = sender
        this.subject = subject
        this.timestamp = timestamp
        this.attachments = attachments
        this.recipients = recipients
        this.body = body
        
        byte [] signablePayload = signablePayload()
        Signature signature = DSAEngine.getInstance().sign(signablePayload, spk)
        this.sig = signature.getData()
        
        this.hashCode = Objects.hash(sender, recipients, subject, timestamp, body, attachments)
    }
    
    MWMessage(InputStream is) {
        DataInputStream dis = new DataInputStream(is)
        byte version = dis.readByte()
        if (version != Constants.MESSENGER_MESSAGE_VERSION)
            throw new InvalidMessageException("unknown version $version")
        
        sender = new Persona(dis)
        
        int nRecipients = dis.readByte()
        nRecipients.times { 
            recipients.add(new Persona(dis))
        }
        
        timestamp = dis.readLong()
        def n = new Name(dis)
        subject = n.name
        n = new Name(dis)
        body = n.name
        
        int nAttachments = dis.readByte()
        nAttachments.times { 
            attachments.add(new MWMessageAttachment(dis))
        }
        
        sig = new byte[Constants.SIG_TYPE.getSigLen()]
        dis.readFully(sig)
        
        if (!verify())
            throw new InvalidMessageException("verification failed")
            
        this.hashCode = Objects.hash(sender, recipients, subject, timestamp, body, attachments)
    }
    
    private byte[] signablePayload() {
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        
        daos.writeByte(Constants.MESSENGER_MESSAGE_VERSION)
        sender.write(daos)
        
        daos.writeByte(recipients.size())
        recipients.each { 
            it.write(daos)
        }
        
        
        daos.writeLong(timestamp)
        
        def n = new Name(subject)
        n.write(daos)
        n = new Name(body)
        n.write(daos)
        
        daos.writeByte(attachments.size())
        attachments.each { 
            it.write(daos)
        }
        
        daos.close()
        baos.toByteArray()
    }
    
    private boolean verify() {
        byte [] signable = signablePayload()
        def spk = sender.destination.getSigningPublicKey()
        def signature = new Signature(spk.getType(), sig)
        DSAEngine.getInstance().verifySignature(signature, signable, spk)
    }
    
    private byte [] getPayload() {
        if (payload == null) {
            def baos = new ByteArrayOutputStream()
            baos.write(signablePayload())
            baos.write(sig)
            payload = baos.toByteArray()
        }
        payload
    }
    
    public InfoHash getInfoHash() {
        if (infoHash == null) {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            digester.update(getPayload())
            infoHash = new InfoHash(digester.digest())
        }
        infoHash
    }
    
    public void write(OutputStream os) {
        os.write(getPayload())
    }
    
    public int hashCode() {
        hashCode
    }
    
    public boolean equals(Object o) {
        MWMessage other = (MWMessage) o
        return Objects.equals(sender, other.sender) &&
                timestamp == other.timestamp &&
                Objects.equals(subject, other.subject) &&
                Objects.equals(body, other.body) &&
                Objects.equals(attachments, other.attachments) &&
                Objects.equals(recipients, other.recipients)
    }
}
