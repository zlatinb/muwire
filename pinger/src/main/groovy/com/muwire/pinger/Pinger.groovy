package com.muwire.pinger



import java.nio.file.Files
import java.nio.file.Path

import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.client.datagram.I2PDatagramMaker
import net.i2p.data.Destination

public class Pinger {
    public static void main(String []args) {
        if (args.length != 2) {
            println "Pass b64 destination as argument 1 and file with contents to send as argument 2"
            System.exit(1)
        }

        def target = new Destination(args[0])
        def payload = (new File(args[1])).getBytes()

        def i2pClientFactory = new I2PClientFactory()
        def i2pClient = i2pClientFactory.createClient()

        def props = System.getProperties().clone()
        props.putAt("outbound.nickname", "MuWire Pinger")

        def baos = new ByteArrayOutputStream()
        def myDest = i2pClient.createDestination(baos)
        def bais = new ByteArrayInputStream(baos.toByteArray())

        def session = i2pClient.createSession(bais, props)

        session.addMuxedSessionListener(new Listener(),
            I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY)
        session.connect()

        def maker = new I2PDatagramMaker(session)
        payload = maker.makeI2PDatagram(payload)
        session.sendMessage(target, payload, I2PSession.PROTO_DATAGRAM, 0, 0)
        println "Sent message, going to sleep"
        Thread.sleep(Integer.MAX_VALUE)
    }

    static class Listener implements I2PSessionMuxedListener {

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size) {

        }

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromport, int toport) {
            def payload = session.receiveMessage(msgId)
            def dissector = new I2PDatagramDissector()
            try {
                dissector.loadI2PDatagram(payload)
                def sender = dissector.getSender().toBase32()
                payload = new String(dissector.getPayload())
                println "From $sender received $payload"
            } catch (Exception e) {
                e.printStackTrace()
            }
        }

        @Override
        public void reportAbuse(I2PSession session, int severity) {
            println "abuse $severity"
        }

        @Override
        public void disconnected(I2PSession session) {
            println "disconnected"
        }

        @Override
        public void errorOccurred(I2PSession session, String message, Throwable error) {
            println "Error $message $error"
        }

    }
}
