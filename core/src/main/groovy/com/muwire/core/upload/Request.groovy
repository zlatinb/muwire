package com.muwire.core.upload

import java.nio.charset.StandardCharsets

import com.muwire.core.InfoHash

import groovy.util.logging.Log
import net.i2p.data.Base64

@Log
class Request {
    
    private static final byte R = "\r".getBytes(StandardCharsets.US_ASCII)[0]
    private static final byte N = "\n".getBytes(StandardCharsets.US_ASCII)[0]
    
    InfoHash infoHash
    Range range
    Map<String, String> headers
    
    static Request parse(InfoHash infoHash, InputStream is) throws IOException {
        Map<String,String> headers = new HashMap<>()
        byte [] tmp = new byte[0x1 << 14]
        while(true) {
            boolean r = false
            boolean n = false
            int idx = 0
            while (true) {
                byte read = is.read()
                if (read == -1)
                    throw new IOException("Stream closed")
                    
                if (!r && read == N)
                    throw new IOException("Received N before R")
                if (read == R) {
                    if (r) 
                        throw new IOException("double R")
                    r = true
                    continue
                }
                
                if (r && !n) {
                    if (read != N) 
                        throw new IOException("R not followed by N")
                    n = true
                    break
                }
                if (idx == 0x1 << 14)
                    throw new IOException("Header too long")    
                tmp[idx++] = read
            }
            
            if (idx == 0)
                break
                
            String header = new String(tmp, 0, idx, StandardCharsets.US_ASCII)
            log.fine("Read header $header")
            
            int keyIdx = header.indexOf(":")
            if (keyIdx < 1)
                throw new IOException("Header key not found")
            if (keyIdx == header.length())
                throw new IOException("Header value not found")
            String key = header.substring(0, keyIdx)
            String value = header.substring(keyIdx + 1)
            headers.put(key, value)
        }
        
        if (!headers.containsKey("Range"))
            throw new IOException("Range header not found")
            
        String range = headers.get("Range").trim()
        String[] split = range.split("-")
        if (split.length != 2)
            throw new IOException("Invalid range header $range")
        long start
        long end
        try {
            start = Long.parseLong(split[0])
            end = Long.parseLong(split[1])
        } catch (NumberFormatException nfe) {
            throw new IOException(nfe)
        }
        
        if (start < 0 || end < start)
            throw new IOException("Invalid range $start - $end")
        
        new Request( infoHash : infoHash, range : new Range(start, end), headers : headers)
    }
    
}
