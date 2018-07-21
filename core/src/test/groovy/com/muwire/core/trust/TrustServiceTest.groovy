package com.muwire.core.trust

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.Destinations

import net.i2p.data.Destination

class TrustServiceTest {

	TrustService service
	File persistGood, persistBad
	Destinations dests = new Destinations()
	
	@Before
	void before() {
		persistGood = new File("good.trust")
		persistBad = new File("bad.trust")
		persistGood.delete()
		persistBad.delete()
		persistGood.deleteOnExit()
		persistBad.deleteOnExit()
		service = new TrustService(persistGood, persistBad, 100)
		service.start()
	}
	
	@After
	void after() {
		service.stop()
	}
	
	@Test
	void testEmpty() {
		assert TrustLevel.NEUTRAL == service.getLevel(dests.dest1)
		assert TrustLevel.NEUTRAL == service.getLevel(dests.dest2)
	}
	
	@Test
	void testOnEvent() {
		service.onTrustEvent new TrustEvent(level: TrustLevel.TRUSTED, destination: dests.dest1)
		service.onTrustEvent new TrustEvent(level: TrustLevel.DISTRUSTED, destination: dests.dest2)
		
		assert TrustLevel.TRUSTED == service.getLevel(dests.dest1)
		assert TrustLevel.DISTRUSTED == service.getLevel(dests.dest2)
	}
	
	@Test 
	void testPersist() {
		service.onTrustEvent new TrustEvent(level: TrustLevel.TRUSTED, destination: dests.dest1)
		service.onTrustEvent new TrustEvent(level: TrustLevel.DISTRUSTED, destination: dests.dest2)
		
		Thread.sleep(250)
		def trusted = new HashSet<>()
		persistGood.eachLine {
			trusted.add(new Destination(it))
		}
		def distrusted = new HashSet<>()
		persistBad.eachLine {
			distrusted.add(new Destination(it))
		}
		
		assert trusted.size() == 1
		assert trusted.contains(dests.dest1)
		assert distrusted.size() == 1
		assert distrusted.contains(dests.dest2)
	}
	
	@Test
	void testLoad() {
		service.stop()
		persistGood.append("${dests.dest1.toBase64()}\n")
		persistBad.append("${dests.dest2.toBase64()}\n")
		service = new TrustService(persistGood, persistBad, 100)
		service.start()
		Thread.sleep(10)
		
		assert TrustLevel.TRUSTED == service.getLevel(dests.dest1)
		assert TrustLevel.DISTRUSTED == service.getLevel(dests.dest2)

	}
}
