package com.muwire.hostcache

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
	
	@Before
	void before() {
		pingerMock = new MockFor(Pinger)
		pinger = pingerMock.proxyInstance()
		
		hostPoolMock = new MockFor(HostPool)
		hostPool = hostPoolMock.proxyInstance()
		
		crawler = new Crawler(pinger, hostPool)
	}
	
	@Test
	void testBadJson() {
		def unpingedHost = new Host(destination : new Destination())
		crawler.handleCrawlerPong(null, new Destination())
		hostPoolMock.verify hostPool
	}
}
