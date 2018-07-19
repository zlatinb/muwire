package com.muwire.core.files

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.After
import org.junit.Before
import org.junit.Test

class FileHasherTest extends GroovyTestCase {

	def hasher = new FileHasher()
	File tmp
	
	@Before
	void setUp() {
		tmp = File.createTempFile("testFile", "test")
		tmp.deleteOnExit()
	}
	
	@After
	void tearDown() {
		tmp?.delete()
	}
	
	@Test
	void testPieceSize() {
		assert 18 == FileHasher.getPieceSize(1000000)
		assert 20 == FileHasher.getPieceSize(100000000)
		assert 30 == FileHasher.getPieceSize(FileHasher.MAX_SIZE)
		shouldFail IllegalArgumentException, {
			FileHasher.getPieceSize(Long.MAX_VALUE)
		}
	}
	
	@Test
	void testHash1Byte() {
		def fos = new FileOutputStream(tmp)
		fos.write(0)
		fos.close()
		def ih = hasher.hashFile(tmp)
		assert ih.getHashList().length == 32
	}
	
	@Test
	void testHash1PieceExact() {
		def fos = new FileOutputStream(tmp)
		byte [] b = new byte[ 0x1 << 18]
		fos.write b
		fos.close()
		def ih = hasher.hashFile tmp
		assert ih.getHashList().length == 32
	}
	
	@Test
	void testHash1Piece1Byte() {
		def fos = new FileOutputStream(tmp)
		byte [] b = new byte[ (0x1 << 18) + 1]
		fos.write b
		fos.close()
		def ih = hasher.hashFile tmp
		assert ih.getHashList().length == 64
	}
	
	@Test
	void testHash2Pieces() {
		def fos = new FileOutputStream(tmp)
		byte [] b = new byte[ (0x1 << 19)]
		fos.write b
		fos.close()
		def ih = hasher.hashFile tmp
		assert ih.getHashList().length == 64
	}
	
	@Test
	void testHash2Pieces2Bytes() {
		def fos = new FileOutputStream(tmp)
		byte [] b = new byte[ (0x1 << 19) + 2]
		fos.write b
		fos.close()
		def ih = hasher.hashFile tmp
		assert ih.getHashList().length == 32 * 3
	}
}
