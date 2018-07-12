package com.muwire.hostcache

import java.util.stream.Collectors

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.client.datagram.I2PDatagramMaker
import net.i2p.util.SystemVersion
import net.i2p.data.*

public class HostCache {

    public static void main(String[] args) {
        if (SystemVersion.isWindows()) {
            println "This will likely not run on windows"
            System.exit(1)
        }
            
        def home = System.getProperty("user.home") + "/.MuWireHostCache"
        home = new File(home)
        if (home.exists() && !home.isDirectory()) {
            println "${home} exists but not a directory?  Delete it or make it a directory"
            System.exit(1)
        }
        
        if (!home.exists()) {
            home.mkdir()
        }
        
        def keyfile = new File(home, "key.dat")
        
        def i2pClientFactory = new I2PClientFactory()
        def i2pClient = i2pClientFactory.createClient()
        
        def myDest
        def session
        if (!keyfile.exists()) {
            def os = new FileOutputStream(keyfile);
            myDest = i2pClient.createDestination(os)
            os.close()
            println "No key.dat file was found, so creating a new destination."
            println "This is the destination you want to give out for your new HostCache"
            println myDest.toBase64()
        } 
        
        def props = System.getProperties().clone()
        props.putAt("inbound.nickname", "MuWire HostCache")
        session = i2pClient.createSession(new FileInputStream(keyfile), props)
        myDest = session.getMyDestination()
        
		// initialize hostpool and crawler
		HostPool hostPool = new HostPool(3, 60 * 1000 * 1000)
		Pinger pinger = new Pinger(session)
		Crawler crawler = new Crawler(pinger, hostPool, 5)
		
		Timer timer = new Timer("timer", true)
		timer.schedule({hostPool.age()} as TimerTask, 1000,1000)
		timer.schedule({crawler.startCrawl()} as TimerTask, 10000, 10000)
		
        session.addMuxedSessionListener(new Listener(hostPool: hostPool, toReturn: 2, crawler: crawler), 
            I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY)
        session.connect()
        println "INFO: connected, going to sleep"
        Thread.sleep(Integer.MAX_VALUE)
        
    }
    
    static class Listener implements I2PSessionMuxedListener {
        final def json = new JsonSlurper()
		def hostPool
		int toReturn
		def crawler
        
        void reportAbuse(I2PSession sesison, int severity) {}
        void disconnected(I2PSession session) {
            println "ERROR: session disconnected, exiting"
            System.exit(1)
        }
        void errorOccurred(I2PSession session, String message, Throwable error) {
            println "ERROR: ${message} ${error}"
        }
        void messageAvailable(I2PSession session, int msgId, long size, int proto,
            int fromport, int toport) {
            if (proto != I2PSession.PROTO_DATAGRAM) {
                println "WARN: received unexpected protocol ${proto}"
                return
            }

            def payload = session.receiveMessage(msgId)
            def dissector = new I2PDatagramDissector()
            try {
                dissector.loadI2PDatagram(payload)
                def sender = dissector.getSender()
                println "INFO: Received something from ${sender.toBase32()}"
                
                payload = dissector.getPayload()
                payload = json.parse(payload)
                if (payload.type == null) {
                    println "WARN: type field missing"
                    return
                }
                switch(payload.type) {
                    case "Ping" : 
                    println "Ping"
					if (payload.leaf == null) {
						println "WARN: ping didn't specify if leaf"
						return
					}
					payload.leaf = Boolean.parseBoolean(payload.leaf.toString())
					if (!payload.leaf)
					    hostPool.addUnverified(new Host(destination: sender))
					respond(session, sender, payload)
                    break
                    case "CrawlerPong":
                    println "CrawlerPong"
					crawler.handleCrawlerPong(sender, payload)
                    break
                    default:
                    println "WARN: Unexpected message type ${payload.type}, dropping"
                }
            } catch (Exception dfe) {
                println "WARN: invalid datagram ${dfe}"
            } 
        }
        void messageAvailable(I2PSession session, int msgId, long size) {
        }
		
		def respond(session, destination, ping) {

			def pongs = hostPool.getVerified(toReturn, ping.leaf)
			pongs = pongs.stream().map({ x -> x.destination.toBase64() }).collect(Collectors.toList())
			
			def pong = [type:"Pong", version: 1, pongs: pongs]
			pong = JsonOutput.toJson(pong)
			def maker = new I2PDatagramMaker(session)
			pong = maker.makeI2PDatagram(pong.bytes)
			session.sendMessage(destination, pong, I2PSession.PROTO_DATAGRAM, 0, 0)
		}
    }
}
