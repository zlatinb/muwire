package com.muwire.core.connection

import java.util.concurrent.CopyOnWriteArrayList

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.Destinations
import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.hostcache.HostCache
import com.muwire.core.hostcache.HostDiscoveredEvent

import groovy.json.JsonOutput
import groovy.mock.interceptor.MockFor

class ConnectionEstablisherTest {

    EventBus eventBus
    final Destinations destinations = new Destinations()
    List<ConnectionEvent> connectionEvents
    List<HostDiscoveredEvent> discoveredEvents
    DataInputStream inputStream
    DataOutputStream outputStream
    
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
        discoveredEvents = new CopyOnWriteArrayList()
        def listener = new Object() {
            void onConnectionEvent(ConnectionEvent e) {
                connectionEvents.add(e)
            }
            void onHostDiscoveredEvent(HostDiscoveredEvent e) {
                discoveredEvents.add e
            }
        }
        eventBus = new EventBus()
        eventBus.register(ConnectionEvent.class, listener)
        eventBus.register(HostDiscoveredEvent.class, listener)
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
        Thread.sleep(100)
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
        settings = new MuWireSettings()
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
    
    @Test
    void testConnectionSucceedsPeer() {
        settings = new MuWireSettings() {
            boolean isLeaf() {false}
        }
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
            PipedOutputStream os = new PipedOutputStream()
            inputStream = new DataInputStream(new PipedInputStream(os))
            PipedInputStream is = new PipedInputStream()
            outputStream = new DataOutputStream(new PipedOutputStream(is))
            new Endpoint(dest, is, os, null)
        }
        
        initMocks()
        
        byte [] header = new byte[11]
        inputStream.readFully(header)
        assert header == "MuWire peer".bytes
        
        outputStream.write("OK".bytes)
        outputStream.flush()
        
        Thread.sleep(100)
        
        assert connectionEvents.size() == 1
        def event = connectionEvents[0]
        assert event.endpoint.destination == destinations.dest1
        assert event.incoming == false
        assert event.status == ConnectionAttemptStatus.SUCCESSFUL
        
    }
    
    @Test
    void testConnectionSucceedsLeaf() {
        settings = new MuWireSettings() {
            boolean isLeaf() {true}
        }
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
            PipedOutputStream os = new PipedOutputStream()
            inputStream = new DataInputStream(new PipedInputStream(os))
            PipedInputStream is = new PipedInputStream()
            outputStream = new DataOutputStream(new PipedOutputStream(is))
            new Endpoint(dest, is, os, null)
        }
        
        initMocks()
        
        byte [] header = new byte[11]
        inputStream.readFully(header)
        assert header == "MuWire leaf".bytes
        
        outputStream.write("OK".bytes)
        outputStream.flush()
        
        Thread.sleep(100)
        
        assert connectionEvents.size() == 1
        def event = connectionEvents[0]
        assert event.endpoint.destination == destinations.dest1
        assert event.incoming == false
        assert event.status == ConnectionAttemptStatus.SUCCESSFUL
    }
    
    @Test
    void testConnectionRejected() {
        settings = new MuWireSettings() {
            boolean isLeaf() {false}
        }
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
            PipedOutputStream os = new PipedOutputStream()
            inputStream = new DataInputStream(new PipedInputStream(os))
            PipedInputStream is = new PipedInputStream()
            outputStream = new DataOutputStream(new PipedOutputStream(is))
            new Endpoint(dest, is, os, null)
        }
        
        initMocks()
        
        byte [] header = new byte[11]
        inputStream.readFully(header)
        assert header == "MuWire peer".bytes
        
        outputStream.write("REJECT".bytes)
        outputStream.flush()
        
        Thread.sleep(100)
        
        assert connectionEvents.size() == 1
        def event = connectionEvents[0]
        assert event.endpoint.destination == destinations.dest1
        assert event.incoming == false
        assert event.status == ConnectionAttemptStatus.REJECTED
        assert discoveredEvents.isEmpty()
    }
    
    @Test
    void testConnectionRejectedSuggestions() {
        settings = new MuWireSettings() {
            boolean isLeaf() {false}
        }
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
            PipedOutputStream os = new PipedOutputStream()
            inputStream = new DataInputStream(new PipedInputStream(os))
            PipedInputStream is = new PipedInputStream()
            outputStream = new DataOutputStream(new PipedOutputStream(is))
            new Endpoint(dest, is, os, null)
        }
        
        initMocks()
        
        byte [] header = new byte[11]
        inputStream.readFully(header)
        assert header == "MuWire peer".bytes
        
        outputStream.write("REJECT".bytes)
        outputStream.flush()
        
        def json = [:]
        json.tryHosts = [destinations.dest2.toBase64()]
        json = JsonOutput.toJson(json)
        outputStream.writeShort(json.bytes.length)
        outputStream.write(json.bytes)
        outputStream.flush()
        Thread.sleep(100)
        
        assert connectionEvents.size() == 1
        def event = connectionEvents[0]
        assert event.endpoint.destination == destinations.dest1
        assert event.incoming == false
        assert event.status == ConnectionAttemptStatus.REJECTED
        
        assert discoveredEvents.size() == 1
        event = discoveredEvents[0]
        assert event.destination == destinations.dest2
    }
}
