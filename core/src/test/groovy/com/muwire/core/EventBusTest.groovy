package com.muwire.core

import org.junit.Test

class EventBusTest {

	class FakeEvent extends Event {}
	
	class FakeEventHandler {
		def onFakeEvent(FakeEvent e) {
			assert e == fakeEvent
		}
	}
	
	FakeEvent fakeEvent = new FakeEvent()
	
	EventBus bus = new EventBus()
	def handler = new FakeEventHandler()
	
	@Test
	void testDynamicEvent() {
		bus.register(FakeEvent.class, handler)
		bus.publish(fakeEvent)
	}

}
