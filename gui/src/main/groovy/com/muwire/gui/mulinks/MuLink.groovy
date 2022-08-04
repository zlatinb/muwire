package com.muwire.gui.mulinks

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import net.i2p.crypto.DSAEngine
import net.i2p.data.Base64
import net.i2p.data.Signature
import net.i2p.data.SigningPublicKey

import java.nio.charset.StandardCharsets
import java.nio.file.Path

abstract class MuLink {
    
    private static final String SCHEME = "muwire"
    private static final int PORT = -1

    static enum LinkType {
        FILE,
        COLLECTION
    }
    
    final String name
    final Persona host
    final InfoHash infoHash
    final LinkType linkType
    
    private final byte[] sig
    
    protected MuLink(Persona host, InfoHash infoHash, String name, byte[] sig, LinkType linkType) {
        this.host = host
        this.infoHash = infoHash
        this.name = name
        this.sig = sig
        this.linkType = linkType
    }
    
    
    private boolean verify() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        
        baos.write(infoHash.getRoot())
        baos.write(name.getBytes(StandardCharsets.UTF_8))
        baos.write(linkType.ordinal())
        
        appendSignedElements(baos)
        
        byte[] payload = baos.toByteArray()

        SigningPublicKey spk = host.destination.getSigningPublicKey()
        Signature signature = new Signature(spk.getType(), sig)
        DSAEngine.getInstance().verifySignature(signature, payload, spk)
    }
    
    protected abstract void appendSignedElements(ByteArrayOutputStream baos)
    
    String toLink() {
        def query = [:]
        query.name = URLEncoder.encode(name, StandardCharsets.UTF_8)
        query.sig = Base64.encode(sig)
        addQueryElements(query)
        
        def kvs = []
        query.each { k, v ->
            kvs << "$k=$v"
        }
        String queryStr = kvs.join("&")
        
        URI uri = new URI(SCHEME,
                host.toBase64(), 
                "/${linkType.name()}/" + Base64.encode(infoHash.getRoot()),
                queryStr,
                null)
        uri.toASCIIString()
    }
    
    protected abstract void addQueryElements(Map<String,String> query)
 
    static MuLink parse(String url) throws InvalidMuLinkException {
        try {
            URI uri = new URI(url)
            if (uri.getScheme() != SCHEME)
                throw new InvalidMuLinkException("Unsupported scheme ${uri.getScheme()}")


            if (uri.getAuthority() == null)
                throw new InvalidMuLinkException("no persona")
            Persona p = new Persona(new ByteArrayInputStream(Base64.decode(uri.getAuthority())))

            if (uri.getPath() == null)
                throw new InvalidMuLinkException("no path")
            String [] pathElements = uri.getPath().split("/")
            if (pathElements.length < 3)
                throw new InvalidMuLinkException("path elements ${pathElements.length}")
            
            LinkType linkType = LinkType.valueOf(pathElements[1])
            
            InfoHash ih = new InfoHash(Base64.decode(pathElements[2]))
            
            Map<String,String> query = parseQuery(uri.getQuery())
            
            if(query.name == null)
                throw new InvalidMuLinkException("name missing")
            String n = URLDecoder.decode(query.name, StandardCharsets.UTF_8)
            
            if(query.sig == null)
                throw new InvalidMuLinkException("no signature")
            byte[] sigBytes = Base64.decode(query.sig)
            if (sigBytes.length != Constants.SIG_TYPE.getSigLen())
                throw new InvalidMuLinkException("invalid sig key")
            
            MuLink rv
            if (linkType == LinkType.FILE)
                rv = new FileMuLink(p, ih, n, sigBytes, query)
            else if (linkType == LinkType.COLLECTION)
                rv = new CollectionMuLink(p, ih, n, sigBytes, query)
            else 
                throw new InvalidMuLinkException("unknown type $linkType")
            
            if (!rv.verify())
                throw new InvalidMuLinkException("failed verification")
            return rv
        } catch (InvalidMuLinkException e) {
            throw e
        } catch (Exception e) {
            throw new InvalidMuLinkException(e)
        }
    }
    
    private static Map<String, String> parseQuery(String query) throws InvalidMuLinkException {
        def rv = [:]
        String[] split = query.split("&")
        for(String kv : split) {
            int equals = kv.indexOf("=")
            if (equals < 0 )
                throw new InvalidMuLinkException("invalid kv $kv")
            
            String k = kv.substring(0, equals)
            String v = kv.substring(equals + 1, kv.length())
            
            if (rv.containsKey(k))
                throw new InvalidMuLinkException("duplicate key $k")
            rv[k] = v
        }
        rv
    }
}
