package com.muwire.core.upload

import java.nio.charset.StandardCharsets

import org.junit.Before
import org.junit.Test

import com.muwire.core.InfoHash

class RequestParsingTest {

    ContentRequest request

    private void fromString(String requestString) {
        def is = new ByteArrayInputStream(requestString.getBytes(StandardCharsets.US_ASCII))
        request = Request.parseContentRequest(new InfoHash(new byte[InfoHash.SIZE]), is)
    }


    private static void failed(String requestString) {
        try {
            def is = new ByteArrayInputStream(requestString.getBytes(StandardCharsets.US_ASCII))
            Request.parseContentRequest(new InfoHash(new byte[InfoHash.SIZE]), is)
            assert false
        } catch (IOException expected) {}
    }

    @Before
    public void setup() {
        request = null
    }

    @Test
    public void testSuccessful() {
        fromString("Range: 1-2\r\n\r\n")
        assert request != null
        assert request.getRange().start == 1
        assert request.getRange().end == 2
    }

    @Test
    public void testRNMissing() {
        failed("Range: 1-2")
    }

    @Test
    public void testRNMissing2() {
        failed("Range: 1-2\r\n")
    }

    @Test
    public void testRR() {
        failed("Range: 1-2\r\r")
    }

    @Test
    public void testNR() {
        failed("Range: 1-2\n\r")
    }

    @Test
    public void testR() {
        failed("Range: 1-2\r")
    }

    @Test
    public void testRX() {
        failed("Range: 1-2\rx")
    }

    @Test
    public void testTwoHeaders() {
        fromString("Range: 1-2\r\nA:B\r\n\r\n")
        assert request != null
        assert request.getRange().start == 1
        assert request.getRange().end == 2
        assert request.getHeaders().size() == 2
        assert request.getHeaders().get("Range").trim() == "1-2"
        assert request.getHeaders().get("A").trim() == "B"
    }

    @Test
    public void testRangeMissing() {
        failed("A:B\r\n")
    }

    @Test
    public void testNoHeaders() {
        failed("\r\n")
    }

    @Test
    public void testInvalidRange() {
        failed("Range 1-2\r\n\r\n")
        failed("Range 2-1\r\n\r\n")
        failed("Range:\r\n\r\n")
        failed("Range: -1-2\r\n\r\n")
        failed("Range: 1-x\r\n\r\n")
        failed("Range: x")
    }

    @Test
    public void testHeaderTooLong() {
        StringBuilder sb = new StringBuilder()
        for (int i = 0; i < (0x1 << 14) + 1; i++)
            sb.append("x")
        failed(sb.toString())
    }
}
