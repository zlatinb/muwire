package net.metanotionz.io

import org.junit.After
import org.junit.Before
import org.junit.Test

class RAIFileTest {
    
    private RAIFile rf
    
    @Before
    void setup() {
        rf = new RAIFile(new File("."),"test")
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
        rf.seek(zeroes)
        rf.writeUTF(longString)
        rf.seek(zeroes)
        assert longString == rf.readUTF()
    }
    
    @Test
    void writeLongAtBoundary() {
        final int zeroes = RAIFile.MAX_SIZE - 4
        rf.seek(zeroes)
        rf.writeLong(Long.MAX_VALUE)
        rf.seek(zeroes)
        assert Long.MAX_VALUE == rf.readLong()
    }
    
    @Test
    void writeHugeArray() {
        byte [] huge = new byte[RAIFile.MAX_SIZE * 3]
        Arrays.fill(huge, (byte)1)
        rf.write(huge)
        assert RAIFile.MAX_SIZE * 3 == rf.getFilePointer()
        byte [] huge2 = new byte[huge.length]
        rf.seek(0)
        rf.read(huge2)
        assert huge == huge2
    }
}
