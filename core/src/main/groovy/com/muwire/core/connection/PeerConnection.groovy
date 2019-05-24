package com.muwire.core.connection

import java.io.InputStream
import java.io.OutputStream

import com.muwire.core.EventBus
import com.muwire.core.hostcache.HostCache
import com.muwire.core.util.DataUtil

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Destination

/**
 * This side is an ultrapeer and the remote is an ultrapeer too
 * @author zab
 */
@Log
class PeerConnection extends Connection {
	
	private final DataInputStream dis
	private final DataOutputStream dos
	
	private final byte[] readHeader = new byte[3]
	private final byte[] writeHeader = new byte[3]
	
	private final JsonSlurper slurper = new JsonSlurper()

	public PeerConnection(EventBus eventBus, Endpoint endpoint,
			boolean incoming, HostCache hostCache) {
		super(eventBus, endpoint, incoming, hostCache)
		this.dis = new DataInputStream(endpoint.inputStream)
		this.dos = new DataOutputStream(endpoint.outputStream)
	}

	@Override
	protected void read() {
		dis.readFully(readHeader)
		int length = DataUtil.readLength(readHeader)
		log.fine("$name read length $length")
		
		byte[] payload = new byte[length]
		dis.readFully(payload)
		
		if ((readHeader[0] & (byte)0x80) == 0x80) {
			// TODO process binary
		} else {
			def json = slurper.parse(payload)
			if (json.type == null)
				throw new Exception("missing json type")
			switch(json.type) {
				case "Ping" : handlePing(); break;
				case "Pong" : handlePong(json); break;
                case "Search": handleSearch(json); break
				default :
					throw new Exception("unknown json type ${json.type}")
			}
		}
	}

	@Override
	protected void write(Object message) {
		byte[] payload
		if (message instanceof Map) {
			payload = JsonOutput.toJson(message).bytes
			DataUtil.packHeader(payload.length, writeHeader)
			log.fine "$name writing message type ${message.type} length $payload.length"
			writeHeader[0] &= (byte)0x7F
		} else {
			// TODO: write binary
		}
		
		dos.write(writeHeader)
		dos.write(payload)
		dos.flush()
	}

}
