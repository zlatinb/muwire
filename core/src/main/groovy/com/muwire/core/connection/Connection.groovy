package com.muwire.core.connection

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import com.muwire.core.EventBus

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
abstract class Connection implements Closeable {

	final EventBus eventBus
	final Endpoint endpoint
	final boolean incoming
	
	private final AtomicBoolean running = new AtomicBoolean()
	private final BlockingQueue messages = new LinkedBlockingQueue()
	private final Thread reader, writer
	
	protected final String name
	
	long lastPingSentTime, lastPingReceivedTime
	
	Connection(EventBus eventBus, Endpoint endpoint, boolean incoming) {
		this.eventBus = eventBus
		this.incoming = incoming
		this.endpoint = endpoint
		
		this.name = endpoint.destination.toBase32().substring(0,8)
		
		this.reader = new Thread({readLoop()} as Runnable)
		this.reader.setName("reader-$name")
		this.reader.setDaemon(true)
		
		this.writer = new Thread({writeLoop()} as Runnable)
		this.writer.setName("writer-$name")
		this.writer.setDaemon(true)
	}
	
	/**
	 * starts the connection threads
	 */
	void start() {
		if (!running.compareAndSet(false, true)) {
			log.log(Level.WARNING,"$name already running", new Exception())
			return
		}
		reader.start()
		writer.start()
	}
	
	@Override
	public void close() {
		if (!running.compareAndSet(true, false)) {
			log.log(Level.WARNING, "$name already closed", new Exception() )
			return
		}
		reader.interrupt()
		writer.interrupt()
		reader.join()
		writer.join()
	}
	
	private void readLoop() {
		try {
			while(running.get()) {
				read()
			}
		} catch (Exception e) {
			if (running.get()) {
				log.log(Level.WARNING,"unhandled exception in reader",e)
				close()
			}
		}
	}
	
	protected abstract void read()
	
	private void writeLoop() {
		try {
			while(running.get()) {
				def message = messages.take()
				write(message)
			}
		} catch (Exception e) {
			if (running.get()) {
				log.log(Level.WARNING, "unhandled exception in writer",e)
				close()
			}
		}
	}
	
	protected abstract void write(def message);
}
