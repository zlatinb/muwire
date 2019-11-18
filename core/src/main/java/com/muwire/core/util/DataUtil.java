package com.muwire.core.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.muwire.core.Constants;

import net.i2p.crypto.DSAEngine;
import net.i2p.data.Base64;
import net.i2p.data.Signature;
import net.i2p.data.SigningPrivateKey;
import net.i2p.util.ConcurrentHashSet;

public class DataUtil {

    private final static int MAX_SHORT = (0x1 << 16) - 1;

    static void writeUnsignedShort(int value, OutputStream os) throws IOException {
        if (value > MAX_SHORT || value < 0)
            throw new IllegalArgumentException("$value invalid");

        byte lsb = (byte) (value & 0xFF);
        byte msb = (byte) (value >> 8);

        os.write(msb);
        os.write(lsb);
    }

    private final static int MAX_HEADER = 0x7FFFFF;

    static void packHeader(int length, byte [] header) {
        if (header.length != 3)
            throw new IllegalArgumentException("header length $header.length");
        if (length < 0 || length > MAX_HEADER)
            throw new IllegalArgumentException("length $length");

        header[2] = (byte) (length & 0xFF);
        header[1] = (byte) ((length >> 8) & 0xFF);
        header[0] = (byte) ((length >> 16) & 0x7F);
    }

    static int readLength(byte [] header) {
        if (header.length != 3)
            throw new IllegalArgumentException("header length $header.length");

        return (((int)(header[0] & 0x7F)) << 16) |
                (((int)(header[1] & 0xFF) << 8)) |
                ((int)header[2] & 0xFF);
    }

    static String readi18nString(byte [] encoded) {
        if (encoded.length < 2)
            throw new IllegalArgumentException("encoding too short $encoded.length");
        int length = ((encoded[0] & 0xFF) << 8) | (encoded[1] & 0xFF);
        if (encoded.length != length + 2)
            throw new IllegalArgumentException("encoding doesn't match length, expected $length found $encoded.length");
        byte [] string = new byte[length];
        System.arraycopy(encoded, 2, string, 0, length);
        return new String(string, StandardCharsets.UTF_8);
    }

    public static byte[] encodei18nString(String string) {
        byte [] utf8 = string.getBytes(StandardCharsets.UTF_8);
        if (utf8.length > Short.MAX_VALUE)
            throw new IllegalArgumentException("String in utf8 too long $utf8.length");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(baos);
        try {
            daos.writeShort((short) utf8.length);
            daos.write(utf8);
            daos.close();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
        return baos.toByteArray();
    }

    public static String readTillRN(InputStream is) throws IOException {
        final long start = System.currentTimeMillis();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(baos.size() < (Constants.MAX_HEADER_SIZE)) {
            int read = is.read();
            if (System.currentTimeMillis() - start > Constants.MAX_HEADER_TIME)
                throw new IOException("header taking too long");
            if (read == -1)
                throw new IOException();
            if (read == '\r') {
                if (is.read() != '\n')
                    throw new IOException("invalid header");
                break;
            }
            baos.write(read);
        }
        return new String(baos.toByteArray(), StandardCharsets.US_ASCII);
    }
    
    public static Map<String, String> readAllHeaders(InputStream is) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String header;
        while(!(header = readTillRN(is)).equals("") && headers.size() < Constants.MAX_HEADERS) {
            int colon = header.indexOf(':');
            if (colon == -1 || colon == header.length() - 1)
                throw new IOException("Invalid header "+ header);
            String key = header.substring(0, colon);
            String value = header.substring(colon + 1);
            headers.put(key, value.trim());
        }
        return headers;
    }

    public static String encodeXHave(List<Integer> pieces, int totalPieces) {
        int bytes = totalPieces / 8;
        if (totalPieces % 8 != 0)
            bytes++;
        byte[] raw = new byte[bytes];
        for (int it : pieces) {
            int byteIdx = it / 8;
            int offset = it % 8;
            int mask = 0x80 >>> offset;
            raw[byteIdx] |= mask;
        }
        return Base64.encode(raw);
    }

    public static List<Integer> decodeXHave(String xHave) {
        byte [] availablePieces = Base64.decode(xHave);
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < availablePieces.length; i ++) {
            byte b = availablePieces[i];
            for (int j = 0; j < 8 ; j++) {
                byte mask = (byte) (0x80 >>> j);
                if ((b & mask) == mask) {
                    available.add(i * 8 + j);
                }
            }
        }
        return available;
    }

    public static Throwable findRoot(Throwable e) {
        while(e.getCause() != null)
            e = e.getCause();
        return e;
    }

    public static void tryUnmap(ByteBuffer cb) {
        if (cb==null || !cb.isDirect()) return;
        // we could use this type cast and call functions without reflection code,
        // but static import from sun.* package is risky for non-SUN virtual machine.
        //try { ((sun.nio.ch.DirectBuffer)cb).cleaner().clean(); } catch (Exception ex) { }

        // JavaSpecVer: 1.6, 1.7, 1.8, 9, 10
        boolean isOldJDK = System.getProperty("java.specification.version","99").startsWith("1.");
        try {
            if (isOldJDK) {
                Method cleaner = cb.getClass().getMethod("cleaner");
                cleaner.setAccessible(true);
                Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
                clean.setAccessible(true);
                clean.invoke(cleaner.invoke(cb));
            } else {
                Class unsafeClass;
                try {
                    unsafeClass = Class.forName("sun.misc.Unsafe");
                } catch(Exception ex) {
                    // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
                    // but that method should be added if sun.misc.Unsafe is removed.
                    unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                }
                Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
                clean.setAccessible(true);
                Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Object theUnsafe = theUnsafeField.get(null);
                clean.invoke(theUnsafe, cb);
            }
        } catch(Exception ex) { }
        cb = null;
    }
    
    public static Set<String> readEncodedSet(Properties props, String property) {
        Set<String> rv = new ConcurrentHashSet<>();
        if (props.containsKey(property)) {
            String [] encoded = props.getProperty(property).split(",");
            for(String s : encoded)
                rv.add(readi18nString(Base64.decode(s)));
        }
        return rv;
    }
    
    public static void writeEncodedSet(Set<String> set, String property, Properties props) {
        if (set.isEmpty())
            return;
        String encoded = set.stream().map(s -> Base64.encode(encodei18nString(s)))
                .collect(Collectors.joining(","));
        props.setProperty(property, encoded);
    }
    
    public static byte[] signUUID(UUID uuid, long timestamp, SigningPrivateKey spk) {
        byte [] payload = (uuid.toString() + String.valueOf(timestamp)).getBytes(StandardCharsets.US_ASCII);
        Signature sig = DSAEngine.getInstance().sign(payload, spk);
        return sig.getData();
    }
}
