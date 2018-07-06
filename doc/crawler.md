# Crawling

Crawling the network is one possible method for a HostCache to discover currently active nodes on the network.  Only ultrapeers support crawling, and that can be disabled by the operator.

### Crawler Ping

The crawler ping is a message sent in a signed datagram to the target ultrapeer.  It contains uncompressed JSON payload with just the message type and version:

```
{
    type: "CrawlerPing",
    version: 1
}
```

### Crawler Pong

The ultrapeer responds with the following message, also in a signed datagram.  It contains the list of Destinations that it is currently connected to.  Since the datagram is limited in size, not all Destinations will be able to fit in it, but that should be fine for the purpose of ultrapeer discovery.

```
{
    type: "CrawlerPong",
    version: 1,
    peers: [ b64.1, b64.2...]
}
```

### Operator control

The operator of the ultrapeer can choose to allow anyone to crawl their node, or just the bundled HostCaches, or nobody at all.
