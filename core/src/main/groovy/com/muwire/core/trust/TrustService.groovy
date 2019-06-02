package com.muwire.core.trust

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.Persona
import com.muwire.core.Service

import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

class TrustService extends Service {

	final File persistGood, persistBad
	final long persistInterval
	
	final Map<Destination, Persona> good = new ConcurrentHashMap<>()
	final Map<Destination, Persona> bad = new ConcurrentHashMap<>()
	
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
		if (persistGood.exists()) {
			persistGood.eachLine {
                byte [] decoded = Base64.decode(it)
                Persona persona = new Persona(new ByteArrayInputStream(decoded))
				good.put(persona.destination, persona)
			}
		}
		if (persistBad.exists()) {
			persistBad.eachLine {
                byte [] decoded = Base64.decode(it)
                Persona persona = new Persona(new ByteArrayInputStream(decoded))
                bad.put(persona.destination, persona)
			}
		}
		timer.schedule({persist()} as TimerTask, persistInterval, persistInterval)
		loaded = true
	}
	
	private void persist() {
		persistGood.delete()
		persistGood.withPrintWriter { writer ->
			good.each {k,v ->
				writer.println v.toBase64()
			}
		}
		persistBad.delete()
		persistBad.withPrintWriter { writer ->
			bad.each { k,v ->
				writer.println v.toBase64()
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
				good.put(e.persona.destination, e.persona)
				break
			case TrustLevel.DISTRUSTED:
				good.remove(e.persona.destination)
				bad.put(e.persona.destination, e.persona)
				break
			case TrustLevel.NEUTRAL:
				good.remove(e.persona.destination)
				bad.remove(e.persona.destination)
				break
		}
	}
}
