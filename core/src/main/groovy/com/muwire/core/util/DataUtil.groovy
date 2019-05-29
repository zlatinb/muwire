package com.muwire.core.util

import java.nio.charset.StandardCharsets

import com.muwire.core.Constants

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
}
