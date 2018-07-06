# MuWire Design

### Initial Bootstrap

Initial bootstrap into the MuWire network is done in a similar fashion to Gnutella.  Dedicated bootstrap servers ("HostCaches") listen for incoming I2P Datagrams which contain requests for addresses of ultrapeers.  The incoming signed datagram has a flag whether the requesting node is an ultrapeer or a leaf; if the node claims to be an ultrapeer it may be added to the list of known ultrapeers by the HostCache.  Implementors of HostCaches are free to choose other discovery strategies such as crawling, active polling and so on.

In response to the request, the HostCache sends back an I2P datagram containing b64 destinations of chosen ultrapeers.  For ease of implementation, the response datagram is also signed and it's payload is JSON.

### Connectivity to peers

Each node will register a single I2P Destination and then multiplex between I2P streaming and signed datagram protocols.  The streaming pseudo-socket will further multiplex between MuWire protocol connections and HTTP1.1 requests.


### Search request propagation

Leafs send search requests to all ultrapeers they are connected to.  Ultrapeers in turn forward those queries, as well as any queries made by the local user to ALL neighboring ultrapeers, setting a flag "firstHop" to "true".  When an ultrapeer receives a query with that flag set to true, it will clear the flag and forward it only to those neighboring ultrapeers that have a keyword hit in their Bloom filter, as well as to any local leafs that match the keyword search.  When an ultrapeer receives a query with the "firstHop" flag set o false, it will only forward it to any of its connected leafs that match the keyword search.

This is equivalent to setting the maximum TTL in Gnutella to 1.

### File transfer

Files are transferred over HTTP1.1 protocol with some custom headers added for download mesh management.  Files are requested with a GET request which includes the infohash of the file in the URL.  

### List of hashes transfer

Before issuing any GET requests for actual content, it is necessary to acquire the list of hashes of the pieces of the file.  This is done by appending "/hashlist" to the URL after the infohash.  The server will then respond with a body containing list of binary SHA256 hashes of the pieces of the file.  Clients are encouraged to then verify that the SHA256 hash of this list matches the infohash of the file as presented in the search result.

### Mesh management

Download mesh management is identical to Gnutella, except instead of ip addresses b64 I2P destinations are used.  [More information](http://rfc-gnutella.sourceforge.net/developer/tmp/download-mesh.html) 

