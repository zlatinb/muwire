package com.muwire.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * A name of persona, file or search term
 */
public class Name {
    final String name;

    public Name(String name) {
        this.name = name;
    }

    public Name(InputStream nameStream) throws IOException {
        DataInputStream dis = new DataInputStream(nameStream);
        int length = dis.readUnsignedShort();
        byte [] nameBytes = new byte[length];
        dis.readFully(nameBytes);
        this.name = new String(nameBytes, StandardCharsets.UTF_8);
    }

    public void write(OutputStream out) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        byte [] bytes = name.getBytes(StandardCharsets.UTF_8);
        dos.writeShort(bytes.length);
        dos.write(bytes);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Name))
            return false;
        Name other = (Name)o;
        return name.equals(other.name);
    }
}
