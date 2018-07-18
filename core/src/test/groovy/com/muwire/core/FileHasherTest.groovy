package com.muwire.core

import static org.junit.jupiter.api.Assertions.assertEquals

import org.junit.Test

class FileHasherTest extends GroovyTestCase {

	@Test
	void testPieceSize() {
		assert 18 == FileHasher.getPieceSize(1000000)
		assert 20 == FileHasher.getPieceSize(100000000)
		assert 30 == FileHasher.getPieceSize(FileHasher.MAX_SIZE)
		shouldFail IllegalArgumentException, {
			FileHasher.getPieceSize(Long.MAX_VALUE)
		}
		
	}
}
