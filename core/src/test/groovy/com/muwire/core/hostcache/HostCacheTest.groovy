package com.muwire.core.hostcache

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.Destinations
import com.muwire.core.MuWireSettings
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.Endpoint
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.mock.interceptor.MockFor
import groovy.mock.interceptor.StubFor
import net.i2p.data.Destination

class HostCacheTest {


    File persist
    HostCache cache

    def trustMock
    TrustService trust

    def settingsMock
    MuWireSettings settings

    Destinations destinations = new Destinations()

    @Before
    void before() {
        persist = new File("hostPersist")
        persist.delete()
        persist.deleteOnExit()

        trustMock = new MockFor(TrustService.class)
        settingsMock = new MockFor(MuWireSettings.class)
    }

    @After
    void after() {
        cache?.stop()
        trustMock.verify trust
        settingsMock.verify settings
        Thread.sleep(150)
    }

    private void initMocks() {
        trust = trustMock.proxyInstance()
        settings = settingsMock.proxyInstance()
        cache = new SimpleHostCache(trust, persist, 100, settings, new Destination())
        cache.start()
        Thread.sleep(150)
    }

    @Test
    void testEmpty() {
        initMocks()
        assert cache.getHosts(5).size() == 0
        assert cache.getGoodHosts(5).size() == 0
    }

    @Test
    void testOnDiscoveredEvent() {
        trustMock.ignore.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.NEUTRAL
        }
        settingsMock.ignore.allowUntrusted { true }
        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }

        initMocks()

        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))

        def rv = cache.getHosts(5)
        assert rv.size() == 1
        assert rv.contains(destinations.dest1)

        assert cache.getGoodHosts(5).size() == 0
    }

    @Test
    void testOnDiscoveredUntrustedHost() {
        trustMock.demand.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.DISTRUSTED
        }

        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }
        
        initMocks()

        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))
        assert cache.getHosts(5).size() == 0
    }

    @Test
    void testOnDiscoverNeutralHostsProhibited() {
        trustMock.ignore.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.NEUTRAL
        }
        settingsMock.ignore.allowUntrusted { false }
        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }

        initMocks()

        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))
        assert cache.getHosts(5).size() == 0
    }

    @Test
    void test2DiscoveredGoodHosts() {
        trustMock.demand.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }
        trustMock.demand.getLevel { d ->
            assert d == destinations.dest2
            TrustLevel.TRUSTED
        }
        trustMock.demand.getLevel{ d -> TrustLevel.TRUSTED }
        trustMock.demand.getLevel{ d -> TrustLevel.TRUSTED }
        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }

        initMocks()
        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))
        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest2))

        def rv = cache.getHosts(1)
        assert rv.size() == 1
        assert rv.contains(destinations.dest1) || rv.contains(destinations.dest2)
    }

    @Test
    void testHostFailed() {
        trustMock.demand.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }
        trustMock.demand.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }

        settingsMock.ignore.getHostClearInterval { 100 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }
        
        initMocks()
        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))

        def endpoint = new Endpoint(destinations.dest1, null, null, null)
        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.FAILED))
        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.FAILED))
        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.FAILED))

        assert cache.getHosts(5).size() == 0
    }

    @Test
    void testFailedHostSuceeds() {
        trustMock.ignore.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }

        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }
        
        initMocks()
        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))

        def endpoint = new Endpoint(destinations.dest1, null, null, null)
        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.FAILED))
        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.FAILED))
        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.FAILED))
        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.SUCCESSFUL))

        def rv = cache.getHosts(5)
        assert rv.size() == 1
        assert rv.contains(destinations.dest1)

        rv = cache.getGoodHosts(5)
        assert rv.size() == 1
        assert rv.contains(destinations.dest1)
    }

    @Test
    void testFailedOnceNoLongerGood() {
        trustMock.ignore.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }

        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }
        
        initMocks()
        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))

        def endpoint = new Endpoint(destinations.dest1, null, null, null)
        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.SUCCESSFUL))

        def rv = cache.getHosts(5)
        def rv2 = cache.getGoodHosts(5)
        assert rv.size() == 1
        assert rv.contains(destinations.dest1)
        assert rv == rv2

        cache.onConnectionEvent( new ConnectionEvent(endpoint: endpoint, status: ConnectionAttemptStatus.FAILED))

        rv = cache.getHosts(5)
        assert rv.size() == 1
        assert rv.contains(destinations.dest1)
        assert cache.getGoodHosts(5).size() == 0
    }

    @Test
    void testDuplicateHostDiscovered() {
        trustMock.demand.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }
        trustMock.demand.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }

        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }
        
        initMocks()
        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))
        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))

        def rv = cache.getHosts(5)
        assert rv.size() == 1
        assert rv.contains(destinations.dest1)
    }

    @Test
    void testSaving() {
        trustMock.ignore.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }
        
        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }
        
        initMocks()
        cache.onHostDiscoveredEvent(new HostDiscoveredEvent(destination: destinations.dest1))
        Thread.sleep(150)

        assert persist.exists()
        int lines = 0
        persist.eachLine {
            lines++
            JsonSlurper slurper = new JsonSlurper()
            def json = slurper.parseText(it)
            assert json.destination == destinations.dest1.toBase64()
            assert json.failures == 0
            assert json.successes == 0
        }
        assert lines == 1
    }

    @Test
    void testLoading() {
        def json = [:]
        json.destination = destinations.dest1.toBase64()
        json.failures = 0
        json.successes = 1
        json = JsonOutput.toJson(json)
        persist.append("${json}\n")

        trustMock.ignore.getLevel { d ->
            assert d == destinations.dest1
            TrustLevel.TRUSTED
        }

        settingsMock.ignore.getHostClearInterval { 0 }
        settingsMock.ignore.getHostHopelessInterval { 0 }
        settingsMock.ignore.getHostRejectInterval { 0 }
        settingsMock.ignore.getHostHopelessPurgeInterval { 0 }
        
        initMocks()
        def rv = cache.getHosts(5)
        assert rv.size() == 1
        assert rv.contains(destinations.dest1)

        assert cache.getGoodHosts(5) == rv
    }
}
