package com.muwire.hostcache

import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Destination

import java.text.SimpleDateFormat
import java.util.stream.Collectors

import groovy.json.JsonOutput

@Log
class HostPool {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd-HH")
    
    final def maxFailures
    final def maxAge

    def verified = new HashMap()
    def unverified = new HashMap()

    HostPool() {}
    HostPool(maxFailures, maxAge) {
        this.maxAge = maxAge
        this.maxFailures = maxFailures
    }

    synchronized def getVerified(int max, boolean leaf) {
        if (verified.isEmpty()) {
            return Collections.emptyList()
        }
        def asList = verified.values().stream().filter({ it -> leaf ? it.leafSlots : it.peerSlots}).collect(Collectors.toList())
        Collections.shuffle(asList)

        return asList[0..Math.min(max, asList.size()) -1]
    }

    synchronized def addUnverified(host) {
        if (!verified.containsKey(host.destination)) {
            unverified.put(host.destination, host)
        }
    }

    synchronized def getUnverified(int max) {
        if (unverified.isEmpty()) {
            return Collections.emptyList()
        }
        def asList = unverified.values().asList()
        Collections.shuffle(asList)
        return asList[0..(Math.min(max, asList.size())-1)]
    }

    synchronized def verify(host) {
        if (!unverified.remove(host.destination))
            throw new IllegalArgumentException()
        host.verifyTime = System.currentTimeMillis();
        host.verificationFailures = 0
        verified.put(host.destination, host)
    }

    synchronized def fail(host) {
        if (!unverified.containsKey(host.destination))
            return
        host.verificationFailures++
    }

    synchronized def age() {
        final long now = System.currentTimeMillis()
        for (Iterator iter = verified.keySet().iterator(); iter.hasNext();) {
            def destination = iter.next()
            def host = verified.get(destination)
            if (host.verifyTime + maxAge < now) {
                iter.remove()
                unverified.put(host.destination, host)
            }
        }

        for (Iterator iter = unverified.keySet().iterator(); iter.hasNext();) {
            def destination = iter.next()
            def host = unverified.get(destination)
            if (host.verificationFailures >= maxFailures) {
                iter.remove()
            }
        }
    }

    synchronized void serialize(File verifiedPath, File unverifiedPath) {
        def now = new Date()
        now = SDF.format(now)
        write(new File(verifiedPath, now), verified.values())
        write(new File(unverifiedPath, now), unverified.values())
    }

    private void write(File target, Collection hosts) {
        JsonOutput jsonOutput = new JsonOutput()
        target.withPrintWriter { writer ->
            hosts.each {
                def json = [:]
                json.destination = it.destination.toBase64()
                json.verifyTime = it.verifyTime
                json.leafSlots = it.leafSlots
                json.peerSlots = it.peerSlots
                json.verificationFailures = it.verificationFailures
                def str = jsonOutput.toJson(json)
                writer.println(str)
            }
        }
    }
    
    synchronized void load(File path) {
        File [] serialized = path.listFiles(new FileFilter() {
            @Override
            boolean accept(File pathname) {
                return pathname.length() > 0
            }
        })
        if (serialized == null || serialized.length == 0) {
            log.info("couldn't find any files to load from.")
            return
        }
        Arrays.sort(serialized, new Comparator<File>() {
            @Override
            int compare(File o1, File o2) {
                return Long.compare(o2.lastModified(), o1.lastModified())
            }
        })
        
        File toLoad = serialized[0]
        log.info("loading from $toLoad")
        int loaded = 0
        def slurper = new JsonSlurper()
        toLoad.eachLine {
            def parsed = slurper.parseText(it)
            def host = new Host()
            host.destination = new Destination(parsed.destination)
            host.verifyTime = parsed.verifyTime
            host.leafSlots = parsed.leafSlots
            host.peerSlots = parsed.peerSlots
            host.verificationFailures = parsed.verificationFailures
            addUnverified(host)
            loaded++
        }
        log.info("loaded ${unverified.size()}/$loaded hosts")
    }
}
