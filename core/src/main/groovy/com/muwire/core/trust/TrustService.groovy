package com.muwire.core.trust

import com.muwire.core.EventBus
import com.muwire.core.profile.MWProfileFetchEvent
import com.muwire.core.profile.MWProfileHeader

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
                    def te = fromJson(json)
                    good.put(te.persona.destination, te)
                } catch (Exception bad) {
                    log.log(Level.WARNING,"couldn't parse trust entry $it",bad)
                }
            })
        }
        if (persistBad.exists()) {
            persistBad.eachLine("UTF-8", {
                try {
                    def json = slurper.parseText(it)
                    def te = fromJson(json)
                    bad.put(te.persona.destination, te)
                } catch (Exception bad) {
                    log.log(Level.WARNING,"couldn't parse trust entry $it",bad)
                }
            })
        }
        loaded = true
        eventBus.publish(new TrustServiceLoadedEvent())
    }
    
    private static TrustEntry fromJson(def json) {
        byte [] decoded = Base64.decode(json.persona)
        Persona persona = new Persona(new ByteArrayInputStream(decoded))
        MWProfileHeader profileHeader = null
        if (json.profileHeader != null) {
            decoded = Base64.decode(json.profileHeader)
            profileHeader = new MWProfileHeader(new ByteArrayInputStream(decoded))
        }
        new TrustEntry(persona, (String)json.reason, profileHeader)
    }

    private void persist() {
        persistGood.delete()
        persistGood.withPrintWriter("UTF-8", { writer ->
            good.each {k,v ->
                def json = [:]
                json.persona = v.persona.toBase64()
                json.reason = v.reason
                if (v.profileHeader != null)
                    json.profileHeader = v.profileHeader.toBase64()
                writer.println JsonOutput.toJson(json)
            }
        })
        persistBad.delete()
        persistBad.withPrintWriter("UTF-8", { writer ->
            bad.each { k,v ->
                def json = [:]
                json.persona = v.persona.toBase64()
                json.reason = v.reason
                if (v.profileHeader != null)
                    json.profileHeader = v.profileHeader.toBase64()
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
    
    MWProfileHeader getProfileHeader(Persona persona) {
        def rv = good[persona.destination]?.getProfileHeader()
        if (rv == null)
            rv = bad[persona.destination]?.getProfileHeader()
        rv
    }

    void onTrustEvent(TrustEvent e) {
        switch(e.level) {
            case TrustLevel.TRUSTED:
                bad.remove(e.persona.destination)
                good.put(e.persona.destination, new TrustEntry(e.persona, e.reason, e.profileHeader))
                break
            case TrustLevel.DISTRUSTED:
                good.remove(e.persona.destination)
                bad.put(e.persona.destination, new TrustEntry(e.persona, e.reason, e.profileHeader))
                break
            case TrustLevel.NEUTRAL:
                good.remove(e.persona.destination)
                bad.remove(e.persona.destination)
                break
        }
        diskIO.submit({persist()} as Runnable)
    }
    
    void onMWProfileFetchEvent(MWProfileFetchEvent event) {
        if (event.profile == null)
            return
        def dest = event.profile.getHeader().getPersona().getDestination()
        TrustEntry te = good[dest]
        if (te == null)
            te = bad[dest]
        if (te == null)
            return
        te.profileHeader = event.profile.getHeader()
        diskIO.submit({persist()} as Runnable)
    }
    
    public static class TrustEntry {
        final Persona persona
        final String reason
        volatile MWProfileHeader profileHeader
        TrustEntry(Persona persona, String reason, MWProfileHeader profileHeader) {
            this.persona = persona
            this.reason = reason
            this.profileHeader = profileHeader
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
