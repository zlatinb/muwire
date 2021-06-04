package net.metanotion.io

import org.junit.After
import org.junit.Before
import org.junit.Test

class RAIFileTest {
    
    private RAIFile rf
    
    @Before
    void setup() {
        rf = new RAIFile("test")
    }
    
    @After
    void tearDown() {
        rf.close()
    }
    
    @Test
    void testManySmallWrites() {
        assert 0 == rf.getFilePointer()
        assert 0 == rf.length()
        
        RAIFile.MAX_SIZE.times {rf.writeByte((byte)1)}
        
        assert RAIFile.MAX_SIZE == rf.getFilePointer()
        assert RAIFile.MAX_SIZE == rf.length()
        
        // this should now cause second chunk
        rf.writeByte((byte)1)

        assert (RAIFile.MAX_SIZE + 1) == rf.getFilePointer()
        assert (RAIFile.MAX_SIZE + 1) == rf.length()
    }
    
    @Test
    void writeUTF8AtBoundary() {
        final int zeroes = RAIFile.MAX_SIZE - 8
        final String longString = "long long string"
        zeroes.times {rf.writeByte(0)}
        rf.writeUTF(longString)
        rf.seek(zeroes)
        assert longString == rf.readUTF()
    }
    
    @Test
    void writeLongAtBoundary() {
        final int zeroes = RAIFile.MAX_SIZE - 4
        zeroes.times {rf.writeByte(0)}
        rf.writeLong(Long.MAX_VALUE)
        rf.seek(zeroes)
        assert Long.MAX_VALUE == rf.readLong()
    }
}
