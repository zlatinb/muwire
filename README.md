# MuWire - Easy Anonymous File-Sharing

MuWire is an easy to use file-sharing program which offers anonymity using [I2P technology](http://geti2p.net).

It is inspired by the LimeWire Gnutella client and developped by a former LimeWire developer.

The project is in early planning stage.  You can find technical documentation in the "doc" folder.

### Building

You need Gradle and a JDK 8 or newer.  After installing those and setting up the appropriate paths, just type

```
gradle build
```

And that will build the "pinger", "host-cache" and "core" sub-projects.

### Pinger sub-project

This is a simple command-line utility that sends a datagram with specified payload to a specified destination and prints out any responses

### Host-Cache sub-project

This is the bootstrap server (aka "HostCache") that MuWire uses.  It listens for incoming Pings from MuWire nodes and responds with other nodes to connect to.  In addition, it "crawls" the network to discover live nodes.

### Core sub-project
This is the headless core / backend of MuWire.  At the moment this just queries a HostCache on startup.