package com.muwire.core.util

import static org.junit.Assert.fail

import org.junit.Test

class DataUtilTest {

	
	private static void usVal(int value) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		DataUtil.writeUnsignedShort(value, baos)
		def is = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))
		assert is.readUnsignedShort() == value
	}
	@Test
	void testUnsignedShort() {
		usVal(0)
		usVal(20)
		usVal(Short.MAX_VALUE)
		usVal(Short.MAX_VALUE + 1)
		usVal(0xFFFF)
		
		try {
			usVal(0xFFFF + 1)
			fail()
		} catch (IllegalArgumentException expected) {}
	}
}
