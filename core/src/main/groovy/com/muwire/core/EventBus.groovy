package com.muwire.core

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Level

import com.muwire.core.files.FileSharedEvent

import groovy.util.logging.Log
@Log
class EventBus {

    private Map handlers = new HashMap()
    private final Executor executor = Executors.newSingleThreadExecutor {r ->
        def rv = new Thread(r)
        rv.setDaemon(true)
        rv.setName("event-bus")
        rv
    }

    void publish(Event e) {
        executor.execute({publishInternal(e)} as Runnable)
    }

    private void publishInternal(Event e) {
        log.fine "publishing event $e of type ${e.getClass().getSimpleName()} event $e"
        def currentHandlers
        final def clazz = e.getClass()
        synchronized(this) {
            currentHandlers = handlers.getOrDefault(clazz, [])
        }
        currentHandlers.each {
            try {
                it."on${clazz.getSimpleName()}"(e)
            } catch (Exception bad) {
                log.log(Level.SEVERE, "exception dispatching event",bad)
            }
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
    
    synchronized void unregister(Class<? extends Event> eventType, def handler) {
        log.info("Unregistering $handler for type $eventType")
        handlers[eventType]?.remove(handler)
    }
}
