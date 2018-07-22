package com.muwire.core

import java.util.concurrent.CopyOnWriteArrayList

import com.muwire.core.files.FileSharedEvent

import groovy.util.logging.Log
@Log
class EventBus {
	
	private Map handlers = new HashMap()

	void publish(Event e) {
		log.fine "publishing event of type ${e.getClass().getSimpleName()}"
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
		log.info "Registering $handler for type $eventType"
		def currentHandlers = handlers.get(eventType)
		if (currentHandlers == null) {
			currentHandlers = new CopyOnWriteArrayList()
			handlers.put(eventType, currentHandlers)
		}
		currentHandlers.add handler
	}
}
