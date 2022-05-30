package com.muwire.core.profile;

import com.muwire.core.Constants;
import com.muwire.core.InvalidNicknameException;
import com.muwire.core.InvalidSignatureException;
import com.muwire.core.Name;
import net.i2p.crypto.DSAEngine;
import net.i2p.data.*;

import java.io.*;

public class MWProfile {
    
    private final byte version;
    private final MWProfileHeader header;
    private final byte[] image;
    private final MWProfileImageFormat format;
    private final Name body;
    private final byte [] sig;
    
    private volatile byte[] payload;
    private volatile String base64;
    
    public MWProfile(InputStream inputStream) throws IOException, DataFormatException,
            InvalidSignatureException, InvalidNicknameException {
        version = (byte) (inputStream.read() & 0xFF);
        if (version != Constants.PROFILE_VERSION)
            throw new IOException("unknown version " + version);
        
        header = new MWProfileHeader(inputStream);

        DataInputStream dais = new DataInputStream(inputStream);
        
        byte imageFormat = dais.readByte();
        if (imageFormat == 0)
            format = MWProfileImageFormat.PNG;
        else if (imageFormat == 1)
            format = MWProfileImageFormat.JPG;
        else
            throw new IOException("unknown image format for " + header.getPersona().getHumanReadableName() + " " + imageFormat);
        
        int imageLength = dais.readInt();
        if (imageLength > Constants.MAX_PROFILE_IMAGE_LENGTH)
            throw new IOException("image too long for " + header.getPersona().getHumanReadableName() + " " + imageLength);
        image = new byte[imageLength];
        dais.readFully(image);
        
        body = new Name(dais);
        if (body.getName().length() > Constants.MAX_COMMENT_LENGTH)
            throw new IOException("body too long for " + header.getPersona().getHumanReadableName() + " " + body.getName().length());
        
        sig = new byte[Constants.SIG_TYPE.getSigLen()];
        dais.readFully(sig);
        
        if (!verify())
            throw new InvalidSignatureException("Profile for " + header.getPersona().getHumanReadableName() + " did not verify");
    }
    
    public MWProfile(MWProfileHeader header, byte[] image, MWProfileImageFormat format, String body, SigningPrivateKey spk)
        throws IOException, DataFormatException {
        this.version = Constants.PROFILE_VERSION;
        this.header = header;
        this.format = format;
        this.image = image;
        this.body = new Name(body);
        
        byte [] signablePayload = signablePayload();
        Signature signature = DSAEngine.getInstance().sign(signablePayload, spk);
        this.sig = signature.getData();
    }
    
    private byte[] signablePayload() throws IOException, DataFormatException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(baos);
        
        daos.write(version);
        header.write(daos);
        
        daos.write((byte) format.ordinal());
        
        daos.writeInt(image.length);
        daos.write(image);
        
        body.write(daos);
        
        daos.close();
        return baos.toByteArray();
    }
    
    private boolean verify() throws IOException, DataFormatException {
        byte [] payload = signablePayload();
        SigningPublicKey spk = header.getPersona().getDestination().getSigningPublicKey();
        Signature signature = new Signature(spk.getType(), sig);
        return DSAEngine.getInstance().verifySignature(signature, payload, spk);
    }
    
    public void write(OutputStream outputStream) throws IOException, DataFormatException {
        if (payload == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(signablePayload());
            baos.write(sig);
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
    
    public MWProfileHeader getHeader() {
        return header;
    }
    
    public MWProfileImageFormat getFormat() {
        return format;
    }
    
    public byte[] getImage() {
        return image;
    }
    
    public String getBody() {
        return body.getName();
    }
}
