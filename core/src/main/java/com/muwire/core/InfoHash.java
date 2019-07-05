package com.muwire.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.i2p.data.Base32;
import net.i2p.data.Base64;

public class InfoHash {

    public static final int SIZE = 0x1 << 5;
    
    private final byte[] root;
    private final byte[] hashList;
    
    private final int hashCode;
    
    public InfoHash(byte[] root, byte[] hashList) {
        if (root.length != SIZE)
            throw new IllegalArgumentException("invalid root size "+root.length);
        if (hashList != null && hashList.length % SIZE != 0) 
            throw new IllegalArgumentException("invalid hashList size " + hashList.length);
        this.root = root;
        this.hashList = hashList;
        hashCode = root[0] << 24 |
                root[1] << 16 |
                root[2] << 8 |
                root[3];
    }
    
    public InfoHash(byte[] root) {
        this(root, null);
    }
    
    public InfoHash(String base32) {
        this(Base32.decode(base32));
    }
    
    public static InfoHash fromHashList(byte []hashList) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] root = sha256.digest(hashList);
            return new InfoHash(root, hashList);
        } catch (NoSuchAlgorithmException impossible) {
            impossible.printStackTrace();
            System.exit(1);
        }
        return null;
    }
    
    public byte[] getRoot() {
        return root;
    }
    
    public byte[] getHashList() {
        return hashList;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InfoHash)) {
            return false;
        }
        InfoHash other = (InfoHash) o;
        return Arrays.equals(root, other.root);
    }
    
    public String toString() {
        String rv = "InfoHash[root:"+Base64.encode(root) + " hashList:";
        List<String> b64HashList = new ArrayList<>();
        if (hashList != null) {       
            byte [] tmp = new byte[SIZE];
            for (int i = 0; i < hashList.length / SIZE; i++) {
                System.arraycopy(hashList, SIZE * i, tmp, 0, SIZE);
                b64HashList.add(Base64.encode(tmp));
            }
        }
        rv += b64HashList.toString();
        rv += "]";
        return rv;
    }
}
