package com.muwire.core.upload


import java.nio.charset.StandardCharsets

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.InfoHash
import com.muwire.core.connection.Endpoint

class UploaderTest {
    
    Endpoint endpoint
    File file
    Thread uploadThread
    
    InputStream is
    OutputStream os
    
    Request request
    Uploader uploader
    
    byte[] inFile
    
    @Before
    public void setup() {
        file?.delete()
        file = File.createTempFile("uploadTest", "dat")
        file.deleteOnExit()
        is = new PipedInputStream(0x1 << 14)
        os = new PipedOutputStream(is)
        endpoint = new Endpoint(null, is, os, null)
    }
    
    @After
    public void teardown() {
        file?.delete()
        uploadThread?.interrupt()
        Thread.sleep(50)
    }
    
    private void fillFile(int length) {
        byte [] data = new byte[length]
        def random = new Random()
        random.nextBytes(data)
        def fos = new FileOutputStream(file)
        fos.write(data)
        fos.close()
        inFile = data
    }
    
    private void startUpload() {
        uploader = new Uploader(file, request, endpoint)
        uploadThread = new Thread(uploader.respond() as Runnable)
        uploadThread.setDaemon(true)
        uploadThread.start()
    }
    
    private String readUntilRN() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        while(true) {
            byte read = is.read()
            if (read == -1)
                throw new IOException()
            if (read != '\r') {
                baos.write(read)
                continue
            }
            assert is.read() == '\n'
            break
        }
        new String(baos.toByteArray(), StandardCharsets.US_ASCII)
    }
    
    @Test
    public void testSmallFile() {
        fillFile(20)
        request = new Request(range : new Range(0,19))
        startUpload()
        assert "200 OK" == readUntilRN()
        assert "Content-Range: 0-19" == readUntilRN()
        assert "" == readUntilRN()
        
        byte [] data = new byte[20]
        DataInputStream dis = new DataInputStream(is)
        dis.readFully(data)
        assert inFile == data
    }
    
    @Test
    public void testRequestMiddle() {
        fillFile(20)
        request = new Request(range : new Range(5,15))
        startUpload()
        assert "200 OK" == readUntilRN()
        assert "Content-Range: 5-15" == readUntilRN()
        assert "" == readUntilRN()
        
        byte [] data = new byte[11]
        DataInputStream dis = new DataInputStream(is)
        dis.readFully(data)
        for (int i = 0; i < data.length; i++)
            assert inFile[i+5] == data[i]
    }
    
    @Test
    public void testOutOfRange() {
        fillFile(20)
        request = new Request(range : new Range(0,20))
        startUpload()
        assert "416 Range Not Satisfiable" == readUntilRN()
        assert "" == readUntilRN()
    }
    
}
