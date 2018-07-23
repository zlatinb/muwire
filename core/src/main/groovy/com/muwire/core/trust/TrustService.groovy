package com.muwire.core.trust

import com.muwire.core.Service

import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

class TrustService extends Service {

	final File persistGood, persistBad
	final long persistInterval
	
	final Set<Destination> good = new ConcurrentHashSet<>()
	final Set<Destination> bad = new ConcurrentHashSet<>()
	
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
				good.add(new Destination(it))
			}
		}
		if (persistBad.exists()) {
			persistBad.eachLine {
				bad.add(new Destination(it))
			}
		}
		timer.schedule({persist()} as TimerTask, persistInterval, persistInterval)
		loaded = true
	}
	
	private void persist() {
		persistGood.delete()
		good.each {
			persistGood.append("${it.toBase64()}\n")
		}
		persistBad.delete()
		bad.each {
			persistBad.append("${it.toBase64()}\n")
		}
	}
	
	TrustLevel getLevel(Destination dest) {
		if (good.contains(dest))
			return TrustLevel.TRUSTED
		else if (bad.contains(dest))
			return TrustLevel.DISTRUSTED
		TrustLevel.NEUTRAL
	}
	
	void onTrustEvent(TrustEvent e) {
		switch(e.level) {
			case TrustLevel.TRUSTED:
				bad.remove(e.destination)
				good.add(e.destination)
				break
			case TrustLevel.DISTRUSTED:
				good.remove(e.destination)
				bad.add(e.destination)
				break
			case TrustLevel.NEUTRAL:
				good.remove(e.destination)
				bad.remove(e.destination)
				break
		}
	}
}
