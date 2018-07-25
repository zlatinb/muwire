package com.muwire.core.hostcache

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.MuWireSettings
import com.muwire.core.Service
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.i2p.data.Destination

class HostCache extends Service {

	final TrustService trustService
	final File storage
	final int interval
	final Timer timer
	final MuWireSettings settings
	final Destination myself
	final Map<Destination, Host> hosts = new ConcurrentHashMap<>()
	
	public HostCache(TrustService trustService, File storage, int interval, 
		MuWireSettings settings, Destination myself) {
		this.trustService = trustService
		this.storage = storage
		this.interval = interval
		this.settings = settings
		this.myself = myself
		this.timer = new Timer("host-persister",true)
	}

	void start() {
		timer.schedule({load()} as TimerTask, 1)
	}
	
	void stop() {
		timer.cancel()
	}
	
	void onHostDiscoveredEvent(HostDiscoveredEvent e) {
		if (myself == e.destination)
			return
		if (hosts.containsKey(e.destination))
			return
		Host host = new Host(e.destination)
		if (allowHost(host)) {
			hosts.put(e.destination, host)
		}
	}
	
	void onConnectionEvent(ConnectionEvent e) {
		Destination dest = e.endpoint.destination
		Host host = hosts.get(dest)
		if (host == null) {
			host = new Host(dest)
			hosts.put(dest, host)
		}

		switch(e.status) {
			case ConnectionAttemptStatus.SUCCESSFUL:
			case ConnectionAttemptStatus.REJECTED:
				host.onConnect()
				break
			case ConnectionAttemptStatus.FAILED:
				host.onFailure()
				break
		}
	}
	
	List<Destination> getHosts(int n) {
		List<Destination> rv = new ArrayList<>(hosts.keySet())
		rv.retainAll {allowHost(hosts[it])}
		if (rv.size() <= n)
			return rv
		Collections.shuffle(rv)
		rv[0..n-1]
	}
	
	void load() {
		if (storage.exists()) {
			JsonSlurper slurper = new JsonSlurper()
			storage.eachLine {
				def entry = slurper.parseText(it)
				Destination dest = new Destination(entry.destination)
				Host host = new Host(dest)
				host.failures = Integer.valueOf(String.valueOf(entry.failures))
				if (allowHost(host))
					hosts.put(dest, host)
			}
		}
		timer.schedule({save()} as TimerTask, interval, interval)
		loaded = true
	}
	
	private boolean allowHost(Host host) {
		if (host.isFailed())
			return false
		if (host.destination == myself)
			return false
		TrustLevel trust = trustService.getLevel(host.destination)
		switch(trust) {
			case TrustLevel.DISTRUSTED :
				return false
			case TrustLevel.TRUSTED :
				return true
			case TrustLevel.NEUTRAL :
				return settings.allowUntrusted()
		}
		false
	}
	
	private void save() {
		storage.delete()
		storage.withPrintWriter { writer ->
			hosts.each { dest, host ->
				if (allowHost(host)) {
					def map = [:]
					map.destination = dest.toBase64()
					map.failures = host.failures
					def json = JsonOutput.toJson(map)
					writer.println json
				}
			}
		}
	}
}
