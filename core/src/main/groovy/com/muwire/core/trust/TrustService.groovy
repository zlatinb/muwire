package com.muwire.core.trust

import java.util.concurrent.ConcurrentHashMap
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

    final File persistGood, persistBad
    final long persistInterval

    final Map<Destination, TrustEntry> good = new ConcurrentHashMap<>()
    final Map<Destination, TrustEntry> bad = new ConcurrentHashMap<>()

    final Timer timer

    TrustService() {}

    TrustService(File persistGood, File persistBad, long persistInterval) {
        this.persistBad = persistBad
        this.persistGood = persistGood
        this.persistInterval = persistInterval
        this.timer = new Timer("trust-persister",true)
    }

    void start() {
        timer.schedule({load()} as TimerTask, 1)
    }

    void stop() {
        timer.cancel()
    }

    void load() {
        JsonSlurper slurper = new JsonSlurper()
        if (persistGood.exists()) {
            persistGood.eachLine {
                try {
                    byte [] decoded = Base64.decode(it)
                    Persona persona = new Persona(new ByteArrayInputStream(decoded))
                    good.put(persona.destination, new TrustEntry(persona, null))
                } catch (Exception e) {
                    try {
                        def json = slurper.parseText(it)
                        byte [] decoded = Base64.decode(json.persona)
                        Persona persona = new Persona(new ByteArrayInputStream(decoded))
                        good.put(persona.destination, new TrustEntry(persona, json.reason))
                    } catch (Exception bad) {
                        log.log(Level.WARNING,"couldn't parse trust entry $it",bad)
                    }
                }
            }
        }
        if (persistBad.exists()) {
            persistBad.eachLine {
                 try {
                    byte [] decoded = Base64.decode(it)
                    Persona persona = new Persona(new ByteArrayInputStream(decoded))
                    bad.put(persona.destination, new TrustEntry(persona, null))
                } catch (Exception e) {
                    try {
                        def json = slurper.parseText(it)
                        byte [] decoded = Base64.decode(json.persona)
                        Persona persona = new Persona(new ByteArrayInputStream(decoded))
                        bad.put(persona.destination, new TrustEntry(persona, json.reason))
                    } catch (Exception bad) {
                        log.log(Level.WARNING,"couldn't parse trust entry $it",bad)
                    }
                }
            }
        }
        timer.schedule({persist()} as TimerTask, persistInterval, persistInterval)
        loaded = true
    }

    private void persist() {
        persistGood.delete()
        persistGood.withPrintWriter { writer ->
            good.each {k,v ->
                def json = [:]
                json.persona = v.persona.toBase64()
                json.reason = v.reason
                writer.println JsonOutput.toJson(json)
            }
        }
        persistBad.delete()
        persistBad.withPrintWriter { writer ->
            bad.each { k,v ->
                def json = [:]
                json.persona = v.persona.toBase64()
                json.reason = v.reason
                writer.println JsonOutput.toJson(json)
            }
        }
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
    }
    
    public static class TrustEntry {
        private final Persona persona
        private final String reason
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
