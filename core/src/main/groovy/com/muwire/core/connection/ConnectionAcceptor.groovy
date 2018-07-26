package com.muwire.core.connection

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService

import groovy.util.logging.Log

@Log
class ConnectionAcceptor {

	final EventBus eventBus
	final UltrapeerConnectionManager manager
	final MuWireSettings settings
	final I2PAcceptor acceptor
	final HostCache hostCache
	final TrustService trustService
	
	final ExecutorService acceptorThread
	final ExecutorService handshakerThreads
	
	ConnectionAcceptor(EventBus eventBus, UltrapeerConnectionManager manager,
		MuWireSettings settings, I2PAcceptor acceptor, HostCache hostCache,
		TrustService trustService) {
		this.eventBus = eventBus
		this.manager = manager
		this.settings = settings
		this.acceptor = acceptor
		this.hostCache = hostCache
		this.trustService = trustService
		
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
			log.info("accepted connection from ${incoming.destination}")
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
					processMuWire(e)
					break
				case (byte)'G':
					processGET(e)
					break
				default:
					throw new Exception("Invalid read $read")
			}
		} catch (Exception ex) {
			log.log(Level.WARNING, "incoming connection failed",ex)
			e.close()
			eventBus.publish new ConnectionEvent(endpoint: e, incoming: true, status: ConnectionAttemptStatus.FAILED)
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
		
		if (settings.isLeaf()) {
			if (type != "resu".bytes) {
				throw new IOException("Received incoming non-results connection as leaf")
			}
			byte [] lts = new byte[3]
			dis.readFully(lts)
			if (lts != "lts".bytes)
				throw new IOException("malformed results connection")
			// TODO: hand-off results connection
		} else {
			if (type == "leaf".bytes)
				handleIncoming(e, true)
			else if (type == "peer".bytes)
				handleIncoming(e, false)
			else
				throw new IOException("unknown connection type $type")
		}
	}
	
	private void handleIncoming(Endpoint e, boolean leaf) {
		boolean accept = leaf ? manager.hasLeafSlots() : manager.hasPeerSlots()
		if (accept) {
			log.info("accepting connection, leaf:$leaf")
			e.outputStream.write("OK".bytes)
			e.outputStream.flush()
			def wrapped = new Endpoint(e.destination, new InflaterInputStream(e.inputStream), new DeflaterOutputStream(e.outputStream))
			eventBus.publish(new ConnectionEvent(endpoint: wrapped, incoming: true, status: ConnectionAttemptStatus.SUCCESSFUL))
		} else {
			log.info("rejecting connection, leaf:$leaf")
			e.outputStream.write("REJECT".bytes)
			// TODO: suggest peers
		}
	}
	
	
	
	private void processGET(Endpoint e) {
		// TODO: implement
	}
	
}
