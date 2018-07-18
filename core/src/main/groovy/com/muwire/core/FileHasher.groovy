package com.muwire.core

class FileHasher {

	/** max size of shared file is 128 GB */
	public static final long MAX_SIZE = 0x1 << 37
	
	/**
	 * @param size of the file to be shared
	 * @return the size of each piece in power of 2
	 */
	static int getPieceSize(long size) {
		if (size <= 0x1 << 25)
			return 18
		
		for (int i = 26; i <= 37; i++) {
			if (size <= 0x1 << i) {
				return i-7
			}
		}
		
		throw new IllegalArgumentException("File too large $size")
	}
}
