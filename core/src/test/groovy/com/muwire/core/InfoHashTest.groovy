package com.muwire.core

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.Test

class InfoHashTest {

	@Test
	void testEmpty() {
		byte [] empty = new byte[0x1 << 6];
		def ih = InfoHash.fromHashList(empty)
		def ih2 = new InfoHash("6ws72qwrniqdaj4y55xngcmxtnbqapjdedm7b2hktay2sj2z7nfq");
		assertEquals(ih, ih2);
	}
}
