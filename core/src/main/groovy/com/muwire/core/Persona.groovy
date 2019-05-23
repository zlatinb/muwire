package com.muwire.core

import net.i2p.crypto.DSAEngine
import net.i2p.crypto.SigType
import net.i2p.data.Destination
import net.i2p.data.Signature
import net.i2p.data.SigningPublicKey

public class Persona {
    private static final int SIG_LEN = Constants.SIG_TYPE.getSigLen()
    
    private final byte version
    private final Name name
    private final Destination destination
    private final byte[] sig
    private volatile String humanReadableName
    private volatile byte[] payload
    
    public Persona(InputStream personaStream) throws IOException, InvalidSignatureException {
        version = (byte) (personaStream.read() & 0xFF)
        if (version != Constants.PERSONA_VERSION)
            throw new IOException("Unknown version "+version)
            
        name = new Name(personaStream)
        destination = Destination.create(personaStream)
        sig = new byte[SIG_LEN]
        DataInputStream dis = new DataInputStream(personaStream)
        dis.readFully(sig)
        if (!verify(version, name, destination, sig))
            throw new InvalidSignatureException(getHumanReadableName() + " didn't verify")
    }
    
    private static boolean verify(byte version, Name name, Destination destination, byte [] sig) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        baos.write(version)
        name.write(baos)
        destination.writeBytes(baos)
        byte[] payload = baos.toByteArray()
        SigningPublicKey spk = destination.getSigningPublicKey()
        Signature signature = new Signature(Constants.SIG_TYPE, sig)
        DSAEngine.getInstance().verifySignature(signature, payload, spk)
    }
    
    public void write(OutputStream out) throws IOException {
        if (payload == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            baos.write(version)
            name.write(baos)
            destination.writeBytes(baos)
            baos.write(sig)
            payload = baos.toByteArray()
        }
        out.write(payload)
    }
    
    public String getHumanReadableName() {
        if (humanReadableName == null) 
            humanReadableName = name.getName() + "@" + destination.toBase32().substring(0,32)
        humanReadableName
    }
    
    @Override
    public int hashCode() {
        name.hashCode() ^ destination.hashCode()
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Persona))
            return false
        Persona other = (Persona)o
        name.equals(other.name) && destination.equals(other.destination)
    }
}
