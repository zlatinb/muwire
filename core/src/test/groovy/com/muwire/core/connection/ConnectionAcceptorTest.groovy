package com.muwire.core.connection

import java.util.concurrent.CopyOnWriteArrayList

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.Destinations
import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService

import groovy.mock.interceptor.MockFor

class ConnectionAcceptorTest {

	EventBus eventBus
	final Destinations destinations = new Destinations()
	def settings
	
	def connectionManagerMock
	UltrapeerConnectionManager connectionManager
	
	def i2pAcceptorMock
	I2PAcceptor i2pAcceptor
	
	def hostCacheMock
	HostCache hostCache
	
	def trustServiceMock
	TrustService trustService
	
	ConnectionAcceptor acceptor
	List<ConnectionEvent> connectionEvents
	InputStream inputStream
	OutputStream outputStream
	
	@Before
	void before() {
		connectionManagerMock = new MockFor(UltrapeerConnectionManager.class)
		i2pAcceptorMock = new MockFor(I2PAcceptor.class)
		hostCacheMock = new MockFor(HostCache.class)
		trustServiceMock = new MockFor(TrustService.class)
	}
	
	@After
	void after() {
		acceptor?.stop()
		connectionManagerMock.verify connectionManager
		i2pAcceptorMock.verify i2pAcceptor
		hostCacheMock.verify hostCache
		trustServiceMock.verify trustService
		Thread.sleep(100)
	}
	
	private void initMocks() {
		connectionEvents = new CopyOnWriteArrayList()
		eventBus = new EventBus()
		def listener = new Object() {
			void onConnectionEvent(ConnectionEvent e) {
				connectionEvents.add e
			}
		}
		eventBus.register(ConnectionEvent.class, listener)
		
		connectionManager = connectionManagerMock.proxyInstance()
		i2pAcceptor = i2pAcceptorMock.proxyInstance()
		hostCache = hostCacheMock.proxyInstance()
		trustService = trustServiceMock.proxyInstance()
		
		acceptor = new ConnectionAcceptor(eventBus, connectionManager, settings, i2pAcceptor, hostCache, trustService)
		acceptor.start()
		Thread.sleep(100)
	}
	
	@Test
	void testSuccessfulLeaf() {
		settings = new MuWireSettings() {
			boolean isLeaf() {
				false
			}
		}
		i2pAcceptorMock.demand.accept {
			def is = new PipedInputStream()
			outputStream = new PipedOutputStream(is)
			def os = new PipedOutputStream()
			inputStream = new PipedInputStream(os)
			new Endpoint(destinations.dest1, is, os)
		}
		i2pAcceptorMock.demand.accept { Thread.sleep(Integer.MAX_VALUE) }
		connectionManagerMock.demand.hasLeafSlots() { true }
		trustServiceMock.demand.getLevel { dest ->
			assert dest == destinations.dest1
			TrustLevel.TRUSTED
		}
		
		initMocks()
		
		outputStream.write("MuWire leaf".bytes)
		byte [] OK = new byte[2]
		def dis = new DataInputStream(inputStream)
		dis.readFully(OK)
		assert OK == "OK".bytes
		
		Thread.sleep(50)
		assert connectionEvents.size() == 1
		def event = connectionEvents[0]
		assert event.endpoint.destination == destinations.dest1
		assert event.status == ConnectionAttemptStatus.SUCCESSFUL
	}
}
