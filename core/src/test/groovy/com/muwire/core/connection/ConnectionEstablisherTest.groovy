package com.muwire.core.connection

import java.util.concurrent.CopyOnWriteArrayList

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.Destinations
import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache

import groovy.mock.interceptor.MockFor

class ConnectionEstablisherTest {

	EventBus eventBus
	final Destinations destinations = new Destinations()
	List<ConnectionEvent> connectionEvents
	
	def i2pConnectorMock
	I2PConnector i2pConnector
	
	MuWireSettings settings
	
	def connectionManagerMock
	ConnectionManager connectionManager
	
	def hostCacheMock
	HostCache hostCache
	
	ConnectionEstablisher establisher
	
	
	@Before
	void before() {
		connectionEvents = new CopyOnWriteArrayList()
		def listener = new Object() {
			void onConnectionEvent(ConnectionEvent e) {
				connectionEvents.add(e)
			}
		}
		eventBus = new EventBus()
		eventBus.register(ConnectionEvent.class, listener)
		i2pConnectorMock = new MockFor(I2PConnector.class)
		connectionManagerMock = new MockFor(ConnectionManager.class)
		hostCacheMock = new MockFor(HostCache.class)
	}
	
	@After
	void after() {
		establisher?.stop()
		i2pConnectorMock.verify i2pConnector
		connectionManagerMock.verify connectionManager
		hostCacheMock.verify hostCache
	}
	
	private void initMocks() {
		i2pConnector = i2pConnectorMock.proxyInstance()
		connectionManager = connectionManagerMock.proxyInstance()
		hostCache = hostCacheMock.proxyInstance()
		establisher = new ConnectionEstablisher(eventBus, i2pConnector, settings, connectionManager, hostCache)
		establisher.start()
		Thread.sleep(250)
	}
	
	
	@Test
	void testConnectFails() {
		connectionManagerMock.ignore.needsConnections {
			true 
		}
		hostCacheMock.ignore.getHosts { num ->
			assert num == 1
			[destinations.dest1]
		}
		connectionManagerMock.ignore.isConnected { dest ->
			assert dest == destinations.dest1
			false
		}
		i2pConnectorMock.demand.connect { dest ->
			assert dest == destinations.dest1
			throw new IOException()
		}
		
		initMocks()
		
		assert connectionEvents.size() == 1
		def event = connectionEvents[0]
		assert event.endpoint.destination == destinations.dest1
		assert event.incoming == false
		assert event.status == ConnectionAttemptStatus.FAILED
	}
}
