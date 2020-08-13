package com.muwire.hostcache

import groovy.json.JsonOutput
import groovy.util.logging.Log
import net.i2p.client.I2PSession
import net.i2p.client.SendMessageOptions
import net.i2p.client.datagram.I2PDatagramMaker

@Log
class Pinger {

    final I2PSession session
    Pinger(I2PSession session) {
        this.session = session
    }

    def ping(host, uuid) {
        log.info("pinging $host with uuid:$uuid")
        def maker = new I2PDatagramMaker(session)
        def payload = new HashMap()
        payload.type = "CrawlerPing"
        payload.version = 1
        payload.uuid = uuid
        payload = JsonOutput.toJson(payload)
        payload = maker.makeI2PDatagram(payload.bytes)
        def options = new SendMessageOptions()
        options.setSendLeaseSet(true)
        session.sendMessage(host.destination, payload, 0, payload.length, I2PSession.PROTO_DATAGRAM, 0, 0, options)
    }
}
