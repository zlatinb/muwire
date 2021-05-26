# Upgrade to binary pongs

### Motivation

The current intra-peer protocol is JSON, which means messages need to be ascii-armored.  This is not as optimal as a binary protocol, especially when sending large blobs like I2P destinations.

### Upgrade path

The pinging node sends pings with the `version` field set to 2.

Upon receiving pings with version 2, the responding node will send a binary Pong.  

### Pong format:

```
byte 0: message type, fixed at 1 for "Pong"
byte 1: version, fixed at 2
bytes 2-18: UUID of the Pong
byte 19: number of destinations contained in this Pong
bytes 20-end: destinations
```

### Other benefits

With the reduced size of Pong messages each Pong can carry more destinations.  Also, the default maximum number of connections can be increased.
