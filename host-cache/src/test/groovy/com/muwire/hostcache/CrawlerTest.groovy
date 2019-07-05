package com.muwire.hostcache

import org.junit.After
import org.junit.Before
import org.junit.Test

import groovy.mock.interceptor.MockFor
import groovy.mock.interceptor.StubFor
import net.i2p.data.Destination

class CrawlerTest {

    def pingerMock
    def pinger

    def hostPoolMock
    def hostPool

    def crawler

    Destinations destinations = new Destinations()
    final Host host = new Host(destination: new Destination())
    final Host host1 = new Host(destination: destinations.dest1)
    final Host host2 = new Host(destination: destinations.dest2)

    final int parallel = 5

    @Before
    void before() {
        pingerMock = new MockFor(Pinger)
        hostPoolMock = new MockFor(HostPool)
    }

    @After
    void after() {
        hostPoolMock.verify hostPool
        pingerMock.verify pinger
    }

    private def initCrawler() {
        pinger = pingerMock.proxyInstance()
        hostPool = hostPoolMock.proxyInstance()
        crawler = new Crawler(pinger, hostPool, parallel)

    }

    @Test
    void testBadJson() {
        initCrawler()
        def unpingedHost = new Host(destination : new Destination())
        crawler.handleCrawlerPong(null, new Destination())
    }

    @Test
    void testStartCrawl() {
        hostPoolMock.demand.getUnverified { n ->
            assert n == parallel
            [host]
        }
        pingerMock.demand.ping { h,uuid -> assert h == host }

        initCrawler()
        crawler.startCrawl()

    }

    @Test
    void testFailsUnanswered() {
        hostPoolMock.demand.getUnverified {n -> [host]}
        hostPoolMock.demand.fail { h -> assert h == host }
        hostPoolMock.demand.getUnverified {n -> [:]}
        pingerMock.demand.ping {h,uuid -> }
        initCrawler()

        crawler.startCrawl()
        crawler.startCrawl()
    }

    @Test
    void testVerifiesAnswered() {
        def currentUUID
        hostPoolMock.demand.getUnverified { n -> [host1] }
        hostPoolMock.demand.verify { h -> assert h == host1 }
        pingerMock.demand.ping { h, uuid -> currentUUID = uuid }

        initCrawler()

        crawler.startCrawl()

        def pong = [uuid : currentUUID.toString(), leafSlots : "false", peerSlots: "false", peers: [] ]
        crawler.handleCrawlerPong(pong, host1.destination)
    }

    @Test
    void testWrongSourceIgnored() {
        def currentUUID
        hostPoolMock.demand.getUnverified { n -> [host1] }
        pingerMock.demand.ping { h, uuid -> currentUUID = uuid }

        initCrawler()

        crawler.startCrawl()

        def pong = [uuid : currentUUID.toString(), leafSlots : "false", peerSlots: "false", peers: [] ]
        crawler.handleCrawlerPong(pong, host2.destination)
    }

    @Test
    void testHost1CarriesHost2() {
        def currentUUID
        hostPoolMock.demand.getUnverified { n -> [host1] }
        hostPoolMock.demand.addUnverified { h -> assert h == host2 }
        hostPoolMock.demand.verify { h -> assert h == host1 }
        pingerMock.demand.ping { h, uuid -> currentUUID = uuid }

        initCrawler()

        crawler.startCrawl()

        def pong = [uuid : currentUUID.toString(), leafSlots : "false", peerSlots: "false", peers: [destinations.dest2.toBase64()] ]
        crawler.handleCrawlerPong(pong, host1.destination)
    }

    @Test
    void testWrongUUID() {
        def currentUUID
        hostPoolMock.demand.getUnverified { n -> [host1] }
        hostPoolMock.demand.fail { h -> assert h == host1 }
        pingerMock.demand.ping { h, uuid -> currentUUID = uuid }

        initCrawler()

        crawler.startCrawl()

        def pong = [uuid : UUID.randomUUID().toString(), leafSlots : "false", peerSlots: "false", peers: [] ]
        crawler.handleCrawlerPong(pong, host1.destination)
    }
}
