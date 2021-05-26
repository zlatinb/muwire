package com.muwire.core.connection

import net.i2p.data.Destination

class MessageUtil {
    
    private static final byte PONG = (byte)1
    
    static byte [] createPongV2(UUID uuid, List<Destination> destinations) {
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        daos.writeByte(PONG)
        daos.writeByte((byte)2)
        daos.writeLong(uuid.mostSignificantBits)
        daos.writeLong(uuid.leastSignificantBits)
        daos.writeByte((byte) destinations.size())
        destinations.each {
            it.writeBytes(daos)
        }
        daos.close()
        baos.toByteArray()
    }
    
    static def parseBinaryMessage(byte [] payload) {
        def bais = new ByteArrayInputStream(payload)
        byte type = (byte)(bais.read() & 0xFF)
        switch(type) {
            case PONG:
                return parsePong(bais)
            default:
                throw new Exception("unknown binary message type ${type}")
        }
    }
    
    private static def parsePong(InputStream is) {
        byte version = (byte)(is.read() & 0xFF)
        if (version == (byte)2)
            return parsePongV2(is)
        throw new Exception("Unknown pong version ${version}")
    }
    
    private static def parsePongV2(InputStream is) {
        def rv = [:]
        def dis = new DataInputStream(is)
        
        long msb = dis.readLong()
        long lsb = dis.readLong()
        UUID uuid = new UUID(msb, lsb)
        rv.uuid = uuid.toString()
        
        byte count = dis.readByte()
        List<Destination> destinations = new ArrayList<>(count)
        count.times {
            destinations << Destination.create(dis).toBase64()
        }
        rv.pongs = destinations
        
        rv.type = "Pong"
        rv.version = 2
        
        rv
    }
}
