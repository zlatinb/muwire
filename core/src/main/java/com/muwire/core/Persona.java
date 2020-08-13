package com.muwire.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.muwire.core.util.DataUtil;

import net.i2p.crypto.DSAEngine;
import net.i2p.data.Base64;
import net.i2p.data.DataFormatException;
import net.i2p.data.Destination;
import net.i2p.data.Signature;
import net.i2p.data.SigningPublicKey;

public class Persona {
    private static final int SIG_LEN = Constants.SIG_TYPE.getSigLen();

    private final byte version;
    private final Name name;
    private final Destination destination;
    private final byte[] sig;
    private volatile String humanReadableName;
    private volatile String base64;
    private volatile byte[] payload;

    public Persona(InputStream personaStream) throws IOException, DataFormatException, InvalidSignatureException, InvalidNicknameException {
        version = (byte) (personaStream.read() & 0xFF);
        if (version != Constants.PERSONA_VERSION)
            throw new IOException("Unknown version "+version);

        name = new Name(personaStream);
        if (!DataUtil.isValidName(name.name))
            throw new InvalidNicknameException(name.name + " is not a valid nickname");
        
        destination = Destination.create(personaStream);
        sig = new byte[SIG_LEN];
        DataInputStream dis = new DataInputStream(personaStream);
        dis.readFully(sig);
        if (!verify(version, name, destination, sig))
            throw new InvalidSignatureException(getHumanReadableName() + " didn't verify");
    }
    
    private static boolean verify(byte version, Name name, Destination destination, byte [] sig) 
    throws IOException, DataFormatException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(version);
        name.write(baos);
        destination.writeBytes(baos);
        byte[] payload = baos.toByteArray();
        SigningPublicKey spk = destination.getSigningPublicKey();
        Signature signature = new Signature(spk.getType(), sig);
        return DSAEngine.getInstance().verifySignature(signature, payload, spk);
    }

    public void write(OutputStream out) throws IOException, DataFormatException {
        if (payload == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(version);
            name.write(baos);
            destination.writeBytes(baos);
            baos.write(sig);
            payload = baos.toByteArray();
        }
        out.write(payload);
    }

    public String getHumanReadableName() {
        if (humanReadableName == null)
            humanReadableName = name.getName() + "@" + destination.toBase32().substring(0,32);
        return humanReadableName;
    }
    
    public Destination getDestination() {
        return destination;
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

    @Override
    public int hashCode() {
        return name.hashCode() ^ destination.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Persona))
            return false;
        Persona other = (Persona)o;
        return name.equals(other.name) && destination.equals(other.destination);
    }

    public static void main(String []args) throws Exception {
        if (args.length != 1) {
            System.out.println("This utility decodes a bas64-encoded persona");
            System.exit(1);
        }
        Persona p = new Persona(new ByteArrayInputStream(Base64.decode(args[0])));
        System.out.println(p.getHumanReadableName());
    }
}
