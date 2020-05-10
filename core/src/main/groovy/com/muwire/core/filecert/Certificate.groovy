package com.muwire.core.filecert

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.InvalidSignatureException
import com.muwire.core.Name
import com.muwire.core.Persona

import net.i2p.crypto.DSAEngine
import net.i2p.data.Signature
import net.i2p.data.SigningPrivateKey
import net.i2p.data.SigningPublicKey
import net.i2p.data.Base64

class Certificate {
    private final byte version
    private final InfoHash infoHash
    final Name name, comment
    final long timestamp
    final Persona issuer
    private final byte[] sig
    
    private volatile byte [] payload
    
    private String base64;
    
    Certificate(InputStream is) {
        version = (byte) (is.read() & 0xFF)
        if (version > Constants.FILE_CERT_VERSION)
            throw new IOException("Unknown version $version")
        
        DataInputStream dis = new DataInputStream(is)
        timestamp = dis.readLong()
        
        byte [] root = new byte[InfoHash.SIZE]
        dis.readFully(root)
        infoHash = new InfoHash(root)
        
        name = new Name(dis)
        
        issuer = new Persona(dis)
        if (version == 2) {
            byte present = (byte)(dis.read() & 0xFF)
            if (present != 0) {
                comment = new Name(dis)
            }
        }
        
        sig = new byte[Constants.SIG_TYPE.getSigLen()]
        dis.readFully(sig)
        
        if (!verify(version, infoHash, name, timestamp, issuer, comment, sig))
            throw new InvalidSignatureException("certificate for $name.name from ${issuer.getHumanReadableName()} didn't verify")
    }
    
    Certificate(InfoHash infoHash, String name, long timestamp, Persona issuer, String comment, SigningPrivateKey spk) {
        this.version = Constants.FILE_CERT_VERSION
        this.infoHash = infoHash
        this.name = new Name(name)
        if (comment != null)
            this.comment = new Name(comment)
        else
            this.comment = null
        this.timestamp = timestamp
        this.issuer = issuer
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        DataOutputStream daos = new DataOutputStream(baos)
        
        daos.write(version)
        daos.writeLong(timestamp)
        daos.write(infoHash.getRoot())
        this.name.write(daos)
        issuer.write(daos)
        if (this.comment == null) {
            daos.write((byte) 0)
        } else {
            daos.write((byte) 1)
            this.comment.write(daos)
        }
        daos.close()
        
        byte[] payload = baos.toByteArray()
        Signature signature = DSAEngine.getInstance().sign(payload, spk)
        this.sig = signature.getData()
    }
    
    private static boolean verify(byte version, InfoHash infoHash, Name name, long timestamp, Persona issuer, Name comment, byte[] sig) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        DataOutputStream daos = new DataOutputStream(baos)
        daos.write(version)
        daos.writeLong(timestamp)
        daos.write(infoHash.getRoot())
        name.write(daos)
        issuer.write(daos)
        if (version == 2) {
            if (comment == null) {
                daos.write((byte)0)
            } else {
                daos.write((byte)1)
                comment.write(daos)
            }
        }
        daos.close()
        
        byte [] payload = baos.toByteArray()
        SigningPublicKey spk = issuer.destination.getSigningPublicKey()
        Signature signature = new Signature(spk.getType(), sig)
        DSAEngine.getInstance().verifySignature(signature, payload, spk)
    }
    
    public void write(OutputStream os) {
        if (payload == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            DataOutputStream daos = new DataOutputStream(baos)
            daos.write(version)
            daos.writeLong(timestamp)
            daos.write(infoHash.getRoot())
            name.write(daos)
            issuer.write(daos)
            if (version == 2) {
                if (comment == null)
                    daos.write((byte) 0)
                else {
                    daos.write((byte) 1)
                    comment.write(daos)
                }
            }
            daos.write(sig)
            daos.close()
            
            payload = baos.toByteArray()
        }
        os.write(payload)
    }
    
    public String toBase64() {
        if (base64 == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            write(baos)
            base64 = Base64.encode(baos.toByteArray())
        }
        return base64;
    }
    
    @Override
    public int hashCode() {
        version.hashCode() ^ infoHash.hashCode() ^ timestamp.hashCode() ^ name.hashCode() ^ issuer.hashCode() ^ Objects.hashCode(comment)
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Certificate))
            return false
        Certificate other = (Certificate)o
        
        version == other.version &&
            infoHash == other.infoHash &&
            timestamp == other.timestamp &&
            name == other.name &&
            issuer == other.issuer &&
            comment == other.comment
    }
}
