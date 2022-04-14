package com.muwire.core.trust

import com.muwire.core.EventBus

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Level

import com.muwire.core.Persona
import com.muwire.core.Service

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

@Log
class TrustService extends Service {

    private final EventBus eventBus
    final File persistGood, persistBad

    final Map<Destination, TrustEntry> good = new ConcurrentHashMap<>()
    final Map<Destination, TrustEntry> bad = new ConcurrentHashMap<>()

    final Executor diskIO = Executors.newSingleThreadExecutor()

    TrustService() {}

    TrustService(EventBus eventBus, File persistGood, File persistBad) {
        this.eventBus = eventBus
        this.persistBad = persistBad
        this.persistGood = persistGood
    }

    void start() {
        diskIO.submit ( {load()} as Runnable)
    }

    void stop() {
        diskIO.shutdown()
    }

    void load() {
        JsonSlurper slurper = new JsonSlurper()
        if (persistGood.exists()) {
            persistGood.eachLine("UTF-8", {
                try {
                    def json = slurper.parseText(it)
                    byte [] decoded = Base64.decode(json.persona)
                    Persona persona = new Persona(new ByteArrayInputStream(decoded))
                    good.put(persona.destination, new TrustEntry(persona, json.reason))
                } catch (Exception bad) {
                    log.log(Level.WARNING,"couldn't parse trust entry $it",bad)
                }
            })
        }
        if (persistBad.exists()) {
            persistBad.eachLine("UTF-8", {
                try {
                    def json = slurper.parseText(it)
                    byte [] decoded = Base64.decode(json.persona)
                    Persona persona = new Persona(new ByteArrayInputStream(decoded))
                    bad.put(persona.destination, new TrustEntry(persona, json.reason))
                } catch (Exception bad) {
                    log.log(Level.WARNING,"couldn't parse trust entry $it",bad)
                }
            })
        }
        loaded = true
        eventBus.publish(new TrustServiceLoadedEvent())
    }

    private void persist() {
        persistGood.delete()
        persistGood.withPrintWriter("UTF-8", { writer ->
            good.each {k,v ->
                def json = [:]
                json.persona = v.persona.toBase64()
                json.reason = v.reason
                writer.println JsonOutput.toJson(json)
            }
        })
        persistBad.delete()
        persistBad.withPrintWriter("UTF-8", { writer ->
            bad.each { k,v ->
                def json = [:]
                json.persona = v.persona.toBase64()
                json.reason = v.reason
                writer.println JsonOutput.toJson(json)
            }
        })
    }

    TrustLevel getLevel(Destination dest) {
        if (good.containsKey(dest))
            return TrustLevel.TRUSTED
        else if (bad.containsKey(dest))
            return TrustLevel.DISTRUSTED
        TrustLevel.NEUTRAL
    }

    void onTrustEvent(TrustEvent e) {
        switch(e.level) {
            case TrustLevel.TRUSTED:
                bad.remove(e.persona.destination)
                good.put(e.persona.destination, new TrustEntry(e.persona, e.reason))
                break
            case TrustLevel.DISTRUSTED:
                good.remove(e.persona.destination)
                bad.put(e.persona.destination, new TrustEntry(e.persona, e.reason))
                break
            case TrustLevel.NEUTRAL:
                good.remove(e.persona.destination)
                bad.remove(e.persona.destination)
                break
        }
        diskIO.submit({persist()} as Runnable)
    }
    
    public static class TrustEntry {
        final Persona persona
        final String reason
        TrustEntry(Persona persona, String reason) {
            this.persona = persona
            this.reason = reason
        }
        
        public int hashCode() {
            persona.hashCode()
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof TrustEntry))
                return false
            persona == o.persona
        }
    }
}
