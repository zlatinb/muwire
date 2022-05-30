package com.muwire.core.profile;

import com.muwire.core.*;
import net.i2p.crypto.DSAEngine;
import net.i2p.data.*;

import java.io.*;

public class MWProfileHeader {
    
    private final byte version;
    private final Persona persona;
    private final byte[] thumbNail;
    private final Name title;
    private final byte[] sig;
    
    private volatile String base64;
    private volatile byte[] payload;
    
    public MWProfileHeader(InputStream inputStream) throws IOException, DataFormatException,
            InvalidSignatureException, InvalidNicknameException {
        version = (byte) (inputStream.read() & 0xFF);
        if (version != Constants.PROFILE_HEADER_VERSION)
            throw new IOException("unknown version " + version);
        
        persona = new Persona(inputStream);

        DataInputStream dis = new DataInputStream(inputStream);
        int thumbnailLength = dis.readUnsignedShort();
        thumbNail = new byte[thumbnailLength];
        dis.readFully(thumbNail);
        
        title = new Name(dis);
        if (title.getName().length() > Constants.MAX_PROFILE_TITLE_LENGTH)
            throw new IOException("Profile title too long " + title.getName().length());
        
        sig = new byte[Constants.SIG_TYPE.getSigLen()];
        dis.readFully(sig);
        
        if (!verify())
            throw new InvalidSignatureException("Profile header for " + persona.getHumanReadableName() + " did not verify");
    }
    
    public MWProfileHeader(Persona persona, byte [] thumbNail, String title, SigningPrivateKey spk) 
            throws IOException, DataFormatException {
        this.version = Constants.PROFILE_HEADER_VERSION;
        this.persona = persona;
        this.thumbNail = thumbNail;
        this.title = new Name(title);
        
        byte [] signablePayload = signablePayload();
        Signature signature = DSAEngine.getInstance().sign(signablePayload, spk);
        this.sig = signature.getData();
    }
    
    private byte[] signablePayload() throws IOException, DataFormatException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(baos);
        
        daos.write(version);
        persona.write(daos);
        daos.writeShort((short) thumbNail.length);
        daos.write(thumbNail);
        title.write(daos);
        daos.close();
        return baos.toByteArray();
    }
    
    private boolean verify() throws IOException, DataFormatException {
        byte [] payload = signablePayload();
        SigningPublicKey spk = persona.getDestination().getSigningPublicKey();
        Signature signature = new Signature(spk.getType(), sig);
        return DSAEngine.getInstance().verifySignature(signature, payload, spk);
    }
    
    public void write(OutputStream outputStream) throws IOException, DataFormatException {
        if (payload == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream daos = new DataOutputStream(baos);
            daos.write(signablePayload());
            daos.write(sig);
            daos.close();
            payload = baos.toByteArray();
        }
        outputStream.write(payload);
    }
    
    public String toBase64() {
        if (base64 == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                write(baos);
            } catch (Exception impossible) {
                throw new RuntimeException(impossible);
            }
            base64 = Base64.encode(baos.toByteArray());
        }
        return base64;
    }
    
    public Persona getPersona() {
        return persona;
    }
    
    public byte[] getThumbNail() {
        return thumbNail;
    }
    
    public String getTitle() {
        return title.getName();
    }
}
