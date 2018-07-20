package com.muwire.core

import java.util.concurrent.CopyOnWriteArrayList

import com.muwire.core.files.FileSharedEvent

class EventBus {
	
	private Map handlers = new HashMap()

	void publish(Event e) {
		def currentHandlers
		final def clazz = e.getClass()
		synchronized(handlers) {
			currentHandlers = handlers.getOrDefault(clazz, [])
		}
		currentHandlers.each {
			it."on${clazz.getSimpleName()}"(e)
		}
	}
	
	synchronized void register(Class<? extends Event> eventType, def handler) {
		def currentHandlers = handlers.get(eventType)
		if (currentHandlers == null) {
			currentHandlers = new CopyOnWriteArrayList()
			handlers.put(eventType, currentHandlers)
		}
		currentHandlers.add handler
	}
}
