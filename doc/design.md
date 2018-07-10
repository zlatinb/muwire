# MuWire Design

### Initial Bootstrap

Initial bootstrap into the MuWire network is done in a similar fashion to Gnutella.  Dedicated bootstrap servers ("HostCaches") listen for incoming I2P Datagrams which contain requests for addresses of ultrapeers.  The incoming signed datagram has a flag whether the requesting node is an ultrapeer or a leaf; if the node claims to be an ultrapeer it may be added to the list of known ultrapeers by the HostCache.  Implementors of HostCaches are free to choose other discovery strategies such as crawling, active polling and so on.

In response to the request, the HostCache sends back an I2P datagram containing b64 destinations of chosen ultrapeers.  For ease of implementation, the response datagram is also signed and it's payload is JSON.

### Connectivity to peers

Each node will register a single I2P Destination and then multiplex between I2P streaming and signed datagram protocols.  The streaming pseudo-socket will further multiplex between MuWire protocol connections and HTTP1.1 requests.


### Search request propagation

Leafs send search requests to all ultrapeers they are connected to.  Ultrapeers in turn forward those queries, as well as any queries made by the local user to ALL neighboring ultrapeers, setting a flag "firstHop" to "true".  When an ultrapeer receives a query with that flag set to true, it will clear the flag and forward it only to those neighboring ultrapeers that have a keyword hit in their Bloom filter, as well as to any local leafs that match the keyword search.  When an ultrapeer receives a query with the "firstHop" flag set o false, it will only return search results locally.

This is similar but not equivalent to setting the maximum TTL in Gnutella to 1.

### Search result confirmation

Unlike Gnutella clients, MuWire uses a two-step process to download a file.  When search results first arrive at the originator, they are in "Unconfirmed" state.  Then the user must manually choose to "Verify" the search result by sending a signed datagram to the destination that the search result claims to come from.  That datagram contains the infohash of the search result, and the response contains the full name of the file as well as the list of hashes.  Those are verified by the originator and only if they match the claimed file name the search result becomes "verified" and the user is allowed to download it.

### File transfer

Files are transferred over HTTP1.1 protocol with some custom headers added for download mesh management.  Files are requested with a GET request which includes the infohash of the file in the URL.  

### Mesh management

Download mesh management is identical to Gnutella, except instead of ip addresses b64 I2P destinations are used.  [More information](http://rfc-gnutella.sourceforge.net/developer/tmp/download-mesh.html) 

### In-Network updates

Nodes will periodically ping a pre-defined Destination for a JSON document containing the infohash of a new binary update package using signed datagrams.  If the response indicates a newer version is available, the node will issue an automatic search for that infohash and download it, then prompt the user to install the update.

