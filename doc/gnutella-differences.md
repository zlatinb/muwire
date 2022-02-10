# Differences between Gnutella and MuWire

**This document assumes thorough familiarity with the Gnutella and I2P protocols.**

## Network Topology

Similar to Gnutella 0.4, MuWire uses a single-layer network topology.  There are however some important differences:

### Fan-out factor

In the LimeWire flavor of Gnutella, ultrapeers had 32 slots for leaf connections (later on increased to 64) and 32 slots of connections to other ultrapeers.  MuWire nodes can handle hundreds of connections so this value is made configurable..  This is done in order to reduce search latency and because CPU and RAM resources in modern computers are much higher than during the Gnutella era.

In Gnutella leafs upload Bloom filters of the keywords describing the files they are sharing to ultrapeers.  Then, when a search query arrives at an ultrapeer if the hash of that query matches a bloom filter uploaded by a given leaf, the query is forwarded to that leaf.

In MuWire, indexing is fully local.  This has the advantage that the sharing massive number of files does not affect the network in any way. 

## Search result routing

All search results will be sent directly to the search originator directly via I2P streaming connections.  This is because everyone in I2P is reachable, i.e. nobody is behind a firewall in the I2P crypto-space.

## Hashing strategies

Gnutella has evolved it's hashing strategies from none to SHA1 to Tiger-based Merkle trees.  MuWire will instead use a SHA256-based "infohash" strategy similar to Bittorrent where the shared file gets broken into pieces, each piece gets hashed, and then the list of hashes gets hashed again to produce the final infohash.

## Transfer protocol

The transfer protocol will be almost identical to that of Gnutella - similar to HTTP 1.1 with custom headers in the requests and responses

## MuWire network protocol

Unlike Gnutella, MuWire messages will be in length-prefixed JSON format for ease of implementation and extensibility.  Exception are binary pongs described in the `binary-pongs.md` document.
