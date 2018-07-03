# MuWire design

**This document assumes thorough familiarity with the Gnutella and I2P protocols.**

## Network Topology

Similar to Gnutella, MuWire uses two-layer network topology consisting of "leaf" nodes which do not participate in routing or indexing and "Ultrapeer" nodes which do.  There are however some important differences:

### No Automatic election

By default Gnutella nodes become Ultrapeers automatically based on various criteria.  MuWire users will have an explicit choice whether to download an Ultrapeer "edition" of the software or a Leaf one.  The rationale for this is to encourage content curation by operators of Ultrapeer nodes.

### Fan-out factor

In the LimeWire flavor of Gnutella, ultrapeers had 32 slots for leaf connections (later on increased to 64) and 32 slots of connections to other ultrapeers.  MuWire will allow hundreds of connections both by leafs and ultrapeers.  This is done in order to reduce search latency and because CPU and RAM resources in modern computers are much higher than during the Gnutella era.

### Search request routing

Search requests originating from a leaf or locally at the ultrapeer get forwarded to all neighboring ultrapeers.  Search requests arriving from an ultrapeer connection get forwarded only to those ultrapeer that have a keyword hit in their published Bloom filters.  This simplifies the Gnutella model because instead of numeric ttl value a simple boolean can be used.  Due to the higher fan-out factor this should result in similar search horizon.

## Content indexing

In Gnutella leafs upload Bloom filters of the keywords describing the files they are sharing to ultrapeers.  Then, when a search query arrives at an ultrapeer if the hash of that query matches a bloom filter uploaded by a given leaf, the query is forwarded to that leaf.

In MuWire, leafs will upload the full name and metadata of shared files to the ultrapeer and the ultrapeer will match incoming searches locally.  This enables operators of ultrapeers to disallow content they do not like from being indexed by them.

## Search result routing

All search results will be sent directly to the search originator directly via I2P datagrams.  This is because everyone in I2P is reachable, i.e. nobody is behind a firewall in the I2P crypto-space.

## Hashing strategies

Gnutella has evolved it's hashing strategies from none to SHA1 to Tiger-based Merkle trees.  MuWire will instead use a SHA256-based "infohash" strategy similar to Bittorrent where the shared file gets broken into pieces, each piece gets hashed, and then the list of hashes gets hashed again to produce the final infohash.

## Transfer protocol

The transfer protocol will be almost identical to that of Gnutella - HTTP 1.1 with custom headers in the requests and responses

## MuWire network protocol

Unlike Gnutella, MuWire messages will be in length-prefixed JSON format for ease of implementation and extensibility.
