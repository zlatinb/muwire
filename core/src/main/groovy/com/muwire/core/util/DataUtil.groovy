package com.muwire.core.util

class DataUtil {
	
	private static int MAX_SHORT = (0x1 << 16) - 1

	static writeUnsignedShort(int value, OutputStream os) {
		if (value > MAX_SHORT || value < 0)
			throw new IllegalArgumentException("$value invalid")
		
		byte lsb = (byte) (value & 0xFF)
		byte msb = (byte) (value >> 8)
		
		os.write(msb)
		os.write(lsb)	
	}
}
