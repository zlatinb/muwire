package com.muwire.core.connection

import static org.junit.Assert.fail

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
import com.muwire.core.upload.UploadManager
import com.muwire.core.search.SearchManager

import groovy.json.JsonSlurper
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
    
    def searchManagerMock
    SearchManager searchManager
    
    def uploadManagerMock
    UploadManager uploadManager
    
    def connectionEstablisherMock
    ConnectionEstablisher connectionEstablisher
	
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
        searchManagerMock = new MockFor(SearchManager.class)
        uploadManagerMock = new MockFor(UploadManager.class)
        connectionEstablisherMock = new MockFor(ConnectionEstablisher.class)
	}
	
	@After
	void after() {
		acceptor?.stop()
		connectionManagerMock.verify connectionManager
		i2pAcceptorMock.verify i2pAcceptor
		hostCacheMock.verify hostCache
		trustServiceMock.verify trustService
        searchManagerMock.verify searchManager
        uploadManagerMock.verify uploadManager
        connectionEstablisherMock.verify connectionEstablisher
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
        searchManager = searchManagerMock.proxyInstance()
        uploadManager = uploadManagerMock.proxyInstance()
        connectionEstablisher = connectionEstablisherMock.proxyInstance()
		
		acceptor = new ConnectionAcceptor(eventBus, connectionManager, settings, i2pAcceptor, 
            hostCache, trustService, searchManager, uploadManager, connectionEstablisher)
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
			new Endpoint(destinations.dest1, is, os, null)
		}
		i2pAcceptorMock.demand.accept { Thread.sleep(Integer.MAX_VALUE) }
        connectionEstablisherMock.demand.isInProgress(destinations.dest1) { false }
        connectionManagerMock.demand.isConnected { dest ->
            assert dest == destinations.dest1
            false
        }
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
		assert event.incoming == true
		assert event.leaf == true
	}
	
	@Test
	void testSuccessfulPeer() {
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
			new Endpoint(destinations.dest1, is, os, null)
		}
		i2pAcceptorMock.demand.accept { Thread.sleep(Integer.MAX_VALUE) }
        connectionEstablisherMock.demand.isInProgress(destinations.dest1) { false }
        connectionManagerMock.demand.isConnected { dest ->
            assert dest == destinations.dest1
            false
        }
        connectionManagerMock.demand.hasPeerSlots() { true }
		trustServiceMock.demand.getLevel { dest ->
			assert dest == destinations.dest1
			TrustLevel.TRUSTED
		}
		
		initMocks()
		
		outputStream.write("MuWire peer".bytes)
		byte [] OK = new byte[2]
		def dis = new DataInputStream(inputStream)
		dis.readFully(OK)
		assert OK == "OK".bytes
		
		Thread.sleep(50)
		assert connectionEvents.size() == 1
		def event = connectionEvents[0]
		assert event.endpoint.destination == destinations.dest1
		assert event.status == ConnectionAttemptStatus.SUCCESSFUL
		assert event.incoming == true
		assert event.leaf == false
	}
	
	@Test
	void testLeafRejectsLeaf() {
		settings = new MuWireSettings() {
			boolean isLeaf() {
				true
			}
		}
		i2pAcceptorMock.demand.accept {
			def is = new PipedInputStream()
			outputStream = new PipedOutputStream(is)
			def os = new PipedOutputStream()
			inputStream = new PipedInputStream(os)
			new Endpoint(destinations.dest1, is, os, null)
		}
		i2pAcceptorMock.demand.accept { Thread.sleep(Integer.MAX_VALUE) }
		trustServiceMock.demand.getLevel { dest ->
			assert dest == destinations.dest1
			TrustLevel.TRUSTED
		}
		
		initMocks()
		
		outputStream.write("MuWire leaf".bytes)
		outputStream.flush()
		Thread.sleep(50)
		assert inputStream.read() == -1
		
		Thread.sleep(50)
		assert connectionEvents.size() == 1
		def event = connectionEvents[0]
		assert event.endpoint.destination == destinations.dest1
		assert event.status == ConnectionAttemptStatus.FAILED
		assert event.incoming == true
		assert event.leaf == null
	}
	
	@Test
	void testLeafRejectsPeer() {
		settings = new MuWireSettings() {
			boolean isLeaf() {
				true
			}
		}
		i2pAcceptorMock.demand.accept {
			def is = new PipedInputStream()
			outputStream = new PipedOutputStream(is)
			def os = new PipedOutputStream()
			inputStream = new PipedInputStream(os)
			new Endpoint(destinations.dest1, is, os, null)
		}
		i2pAcceptorMock.demand.accept { Thread.sleep(Integer.MAX_VALUE) }
		trustServiceMock.demand.getLevel { dest ->
			assert dest == destinations.dest1
			TrustLevel.TRUSTED
		}
		
		initMocks()
		
		outputStream.write("MuWire peer".bytes)
		outputStream.flush()
		Thread.sleep(50)
		assert inputStream.read() == -1
		
		Thread.sleep(50)
		assert connectionEvents.size() == 1
		def event = connectionEvents[0]
		assert event.endpoint.destination == destinations.dest1
		assert event.status == ConnectionAttemptStatus.FAILED
		assert event.incoming == true
		assert event.leaf == null
	}
	
	@Test
	void testPeerRejectsPeerSlots() {
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
			new Endpoint(destinations.dest1, is, os, null)
		}
		i2pAcceptorMock.demand.accept { Thread.sleep(Integer.MAX_VALUE) }
        connectionEstablisherMock.demand.isInProgress(destinations.dest1) { false }
        connectionManagerMock.demand.isConnected { dest ->
            assert dest == destinations.dest1
            false
        }
        connectionManagerMock.demand.hasPeerSlots() { false }
		trustServiceMock.demand.getLevel { dest ->
			assert dest == destinations.dest1
			TrustLevel.TRUSTED
		}
		hostCacheMock.ignore.getGoodHosts { n -> [] }
		
		initMocks()
		
		outputStream.write("MuWire peer".bytes)
		byte [] OK = new byte[6]
		def dis = new DataInputStream(inputStream)
		dis.readFully(OK)
		assert OK == "REJECT".bytes
		
		Thread.sleep(50)
		assert dis.read() == -1
		
		Thread.sleep(50)
		assert connectionEvents.size() == 1
		def event = connectionEvents[0]
		assert event.endpoint.destination == destinations.dest1
		assert event.status == ConnectionAttemptStatus.REJECTED
		assert event.incoming == true
		assert event.leaf == false
	}
	
	@Test
	void testPeerRejectsLeafSlots() {
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
			new Endpoint(destinations.dest1, is, os, null)
		}
		i2pAcceptorMock.demand.accept { Thread.sleep(Integer.MAX_VALUE) }
        connectionEstablisherMock.demand.isInProgress(destinations.dest1) { false }
        connectionManagerMock.demand.isConnected { dest ->
            assert dest == destinations.dest1
            false
        }
        connectionManagerMock.demand.hasLeafSlots() { false }
		trustServiceMock.demand.getLevel { dest ->
			assert dest == destinations.dest1
			TrustLevel.TRUSTED
		}
		hostCacheMock.ignore.getGoodHosts { n -> [] }
		
		initMocks()
		
		outputStream.write("MuWire leaf".bytes)
		byte [] OK = new byte[6]
		def dis = new DataInputStream(inputStream)
		dis.readFully(OK)
		assert OK == "REJECT".bytes
		
		Thread.sleep(50)
		assert dis.read() == -1
		
		Thread.sleep(50)
		assert connectionEvents.size() == 1
		def event = connectionEvents[0]
		assert event.endpoint.destination == destinations.dest1
		assert event.status == ConnectionAttemptStatus.REJECTED
		assert event.incoming == true
		assert event.leaf == true
	}
	
	@Test
	void testPeerRejectsPeerSuggests() {
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
			new Endpoint(destinations.dest1, is, os, null)
		}
		i2pAcceptorMock.demand.accept { Thread.sleep(Integer.MAX_VALUE) }
        connectionEstablisherMock.demand.isInProgress(destinations.dest1) { false }
        connectionManagerMock.demand.isConnected { dest ->
            assert dest == destinations.dest1
            false
        }
        connectionManagerMock.demand.hasPeerSlots() { false }
		trustServiceMock.demand.getLevel { dest ->
			assert dest == destinations.dest1
			TrustLevel.TRUSTED
		}
		hostCacheMock.ignore.getGoodHosts { n -> [destinations.dest2] }
		
		initMocks()
		
		outputStream.write("MuWire peer".bytes)
		byte [] OK = new byte[6]
		def dis = new DataInputStream(inputStream)
		dis.readFully(OK)
		assert OK == "REJECT".bytes
		
		short payloadSize = dis.readUnsignedShort()
		byte[] payload = new byte[payloadSize]
		dis.readFully(payload)
		assert dis.read() == -1
		
		def json = new JsonSlurper()
		json = json.parse(payload)
		assert json.tryHosts != null
		assert json.tryHosts.size() == 1
		assert json.tryHosts.contains(destinations.dest2.toBase64())
		
		Thread.sleep(50)
		assert connectionEvents.size() == 1
		def event = connectionEvents[0]
		assert event.endpoint.destination == destinations.dest1
		assert event.status == ConnectionAttemptStatus.REJECTED
	}
}
