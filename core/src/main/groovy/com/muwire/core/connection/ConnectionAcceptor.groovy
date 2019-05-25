package com.muwire.core.connection

import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.hostcache.HostCache
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService
import com.muwire.core.search.InvalidSearchResultException
import com.muwire.core.search.ResultsParser
import com.muwire.core.search.SearchManager
import com.muwire.core.search.UnexpectedResultsException

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

@Log
class ConnectionAcceptor {

	final EventBus eventBus
	final UltrapeerConnectionManager manager
	final MuWireSettings settings
	final I2PAcceptor acceptor
	final HostCache hostCache
	final TrustService trustService
    final SearchManager searchManager 
	
	final ExecutorService acceptorThread
	final ExecutorService handshakerThreads
	
	ConnectionAcceptor(EventBus eventBus, UltrapeerConnectionManager manager,
		MuWireSettings settings, I2PAcceptor acceptor, HostCache hostCache,
		TrustService trustService, searchManager) {
		this.eventBus = eventBus
		this.manager = manager
		this.settings = settings
		this.acceptor = acceptor
		this.hostCache = hostCache
		this.trustService = trustService
        this.searchManager = searchManager
		
		acceptorThread = Executors.newSingleThreadExecutor { r -> 
			def rv = new Thread(r)
			rv.setDaemon(true)
			rv.setName("acceptor")
			rv
		}
		
		handshakerThreads = Executors.newCachedThreadPool { r ->
			def rv = new Thread(r)
			rv.setDaemon(true)
			rv.setName("acceptor-processor-${System.currentTimeMillis()}")
			rv
		}
	}
	
	void start() {
		acceptorThread.execute({acceptLoop()} as Runnable)
	}
	
	void stop() {
		acceptorThread.shutdownNow()
		handshakerThreads.shutdownNow()
	}
	
	private void acceptLoop() {
		while(true) {
			def incoming = acceptor.accept()
			log.info("accepted connection from ${incoming.destination.toBase32()}")
			switch(trustService.getLevel(incoming.destination)) {
				case TrustLevel.TRUSTED : break
				case TrustLevel.NEUTRAL :
					if (settings.allowUntrusted())
						break
				case TrustLevel.DISTRUSTED :
					log.info("Disallowing distrusted connection")
					incoming.close()
					continue
			}
			handshakerThreads.execute({processIncoming(incoming)} as Runnable)
		}
	}
	
	private void processIncoming(Endpoint e) {
		InputStream is = e.inputStream
		try {
			int read = is.read()
			switch(read) {
				case (byte)'M':
                    if (settings.isLeaf())
                        throw new IOException("Incoming connection as leaf")
					processMuWire(e)
					break
				case (byte)'G':
					processGET(e)
					break
                case (byte)'P':
                    processPOST(e)
                    break
				default:
					throw new Exception("Invalid read $read")
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "incoming connection failed",ex)
			e.close()
			eventBus.publish new ConnectionEvent(endpoint: e, incoming: true, leaf: null, status: ConnectionAttemptStatus.FAILED)
		}
	}
	
	private void processMuWire(Endpoint e) {
		byte[] uWire = "uWire ".bytes
		for (int i = 0; i < uWire.length; i++) {
			int read = e.inputStream.read()
			if (read != uWire[i]) {
				throw new IOException("unexpected value $read at position $i")
			}
		}
        
		byte[] type = new byte[4]
		DataInputStream dis = new DataInputStream(e.inputStream)
		dis.readFully(type)
                
        if (type == "leaf".bytes)
            handleIncoming(e, true)
        else if (type == "peer".bytes)
            handleIncoming(e, false)
        else 
            throw new IOException("unknown connection type $type")
    }

	private void handleIncoming(Endpoint e, boolean leaf) {
		boolean accept = leaf ? manager.hasLeafSlots() : manager.hasPeerSlots()
		if (accept) {
			log.info("accepting connection, leaf:$leaf")
			e.outputStream.write("OK".bytes)
			e.outputStream.flush()
			def wrapped = new Endpoint(e.destination, new InflaterInputStream(e.inputStream), new DeflaterOutputStream(e.outputStream, true))
			eventBus.publish(new ConnectionEvent(endpoint: wrapped, incoming: true, leaf: leaf, status: ConnectionAttemptStatus.SUCCESSFUL))
		} else {
			log.info("rejecting connection, leaf:$leaf")
			e.outputStream.write("REJECT".bytes)
			def hosts = hostCache.getGoodHosts(10)
			if (!hosts.isEmpty()) {
				def json = [:]
				json.tryHosts = hosts.collect { d -> d.toBase64() }
				json = JsonOutput.toJson(json)
				def os = new DataOutputStream(e.outputStream)
				os.writeShort(json.bytes.length)
				os.write(json.bytes)
			}
			e.outputStream.flush()
			e.close()
			eventBus.publish(new ConnectionEvent(endpoint: e, incoming: true, leaf: leaf, status: ConnectionAttemptStatus.REJECTED))
		}
	}
	
	
	
	private void processGET(Endpoint e) {
		// TODO: implement
	}
    
    private void processPOST(final Endpoint e) throws IOException {
        byte [] ost = new byte[4]
        final DataInputStream dis = new DataInputStream(e.getInputStream())
        dis.readFully(ost)
        if (ost != "OST ".getBytes(StandardCharsets.US_ASCII))
            throw new IOException("Invalid POST connection")
        JsonSlurper slurper = new JsonSlurper()
        try {
            byte[] uuid = new byte[36]
            dis.readFully(uuid)
            UUID resultsUUID = UUID.fromString(new String(uuid, StandardCharsets.US_ASCII))
            if (!searchManager.hasLocalSearch(resultsUUID))
                throw new UnexpectedResultsException(resultsUUID.toString())

            byte[] rn = new byte[4]
            dis.readFully(rn)
            if (rn != "\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                throw new IOException("invalid request header")

            Persona sender = new Persona(dis)
            if (sender.destination != e.getDestination())
                throw new IOException("Sender destination mismatch expected $e.getDestination(), got $sender.destination")
            int nResults = dis.readUnsignedShort()
            for (int i = 0; i < nResults; i++) {
                int jsonSize = dis.readUnsignedShort()
                byte [] payload = new byte[jsonSize]
                dis.readFully(payload)
                def json = slurper.parse(payload)
                eventBus.publish(ResultsParser.parse(sender, json))
            }
        } catch (IOException | UnexpectedResultsException | InvalidSearchResultException bad) {
            log.log(Level.WARNING, "failed to process POST", bad)
        } finally {
            e.close()
        }
    }
	
}
