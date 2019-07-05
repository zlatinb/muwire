package com.muwire.core.util

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import com.muwire.core.Constants

import net.i2p.data.Base64

class DataUtil {

    private final static int MAX_SHORT = (0x1 << 16) - 1

    static void writeUnsignedShort(int value, OutputStream os) {
        if (value > MAX_SHORT || value < 0)
            throw new IllegalArgumentException("$value invalid")

        byte lsb = (byte) (value & 0xFF)
        byte msb = (byte) (value >> 8)

        os.write(msb)
        os.write(lsb)
    }

    private final static int MAX_HEADER = 0x7FFFFF

    static void packHeader(int length, byte [] header) {
        if (header.length != 3)
            throw new IllegalArgumentException("header length $header.length")
        if (length < 0 || length > MAX_HEADER)
            throw new IllegalArgumentException("length $length")

        header[2] = (byte) (length & 0xFF)
        header[1] = (byte) ((length >> 8) & 0xFF)
        header[0] = (byte) ((length >> 16) & 0x7F)
    }

    static int readLength(byte [] header) {
        if (header.length != 3)
            throw new IllegalArgumentException("header length $header.length")

        return (((int)(header[0] & 0x7F)) << 16) |
                (((int)(header[1] & 0xFF) << 8)) |
                ((int)header[2] & 0xFF)
    }

    static String readi18nString(byte [] encoded) {
        if (encoded.length < 2)
            throw new IllegalArgumentException("encoding too short $encoded.length")
        int length = ((encoded[0] & 0xFF) << 8) | (encoded[1] & 0xFF)
        if (encoded.length != length + 2)
            throw new IllegalArgumentException("encoding doesn't match length, expected $length found $encoded.length")
        byte [] string = new byte[length]
        System.arraycopy(encoded, 2, string, 0, length)
        new String(string, StandardCharsets.UTF_8)
    }

    static byte[] encodei18nString(String string) {
        byte [] utf8 = string.getBytes(StandardCharsets.UTF_8)
        if (utf8.length > Short.MAX_VALUE)
            throw new IllegalArgumentException("String in utf8 too long $utf8.length")
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        daos.writeShort((short) utf8.length)
        daos.write(utf8)
        daos.close()
        baos.toByteArray()
    }

    public static String readTillRN(InputStream is) {
        def baos = new ByteArrayOutputStream()
        while(baos.size() < (Constants.MAX_HEADER_SIZE)) {
            byte read = is.read()
            if (read == -1)
                throw new IOException()
            if (read == '\r') {
                if (is.read() != '\n')
                    throw new IOException("invalid header")
                break
            }
            baos.write(read)
        }
        new String(baos.toByteArray(), StandardCharsets.US_ASCII)
    }

    public static String encodeXHave(List<Integer> pieces, int totalPieces) {
        int bytes = totalPieces / 8
        if (totalPieces % 8 != 0)
            bytes++
        byte[] raw = new byte[bytes]
        pieces.each {
            int byteIdx = it / 8
            int offset = it % 8
            int mask = 0x80 >>> offset
            raw[byteIdx] |= mask
        }
        Base64.encode(raw)
    }

    public static List<Integer> decodeXHave(String xHave) {
        byte [] availablePieces = Base64.decode(xHave)
        List<Integer> available = new ArrayList<>()
        availablePieces.eachWithIndex {b, i ->
            for (int j = 0; j < 8 ; j++) {
                byte mask = 0x80 >>> j
                if ((b & mask) == mask) {
                    available.add(i * 8 + j)
                }
            }
        }
        available
    }

    public static Exception findRoot(Exception e) {
        while(e.getCause() != null)
            e = e.getCause()
        e
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
}
