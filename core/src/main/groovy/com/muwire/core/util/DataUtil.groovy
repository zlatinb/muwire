package com.muwire.core.util

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
				((int)(header[1] & 0xFF << 8)) |
				((int)header[2] & 0xFF)
	}
}
