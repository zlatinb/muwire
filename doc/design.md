# MuWire Design

### Initial Bootstrap

Initial bootstrap into the MuWire network is done in a similar fashion to Gnutella.  Dedicated bootstrap servers ("HostCaches") listen for incoming I2P Datagrams which contain requests for addresses of ultrapeers.  The incoming signed datagram has a flag whether the requesting node is an ultrapeer or a leaf; if the node claims to be an ultrapeer it may be added to the list of known ultrapeers by the HostCache.  Implementors of HostCaches are free to choose other discovery strategies such as crawling, active polling and so on.

In response to the request, the HostCache sends back an I2P datagram containing b64 destinations of chosen ultrapeers.  For ease of implementation, the response datagram is also signed and it's payload is JSON.

### Connectivity to peers

Each node will register a single I2P Destination and then multiplex between I2P streaming and signed datagram protocols.  The streaming pseudo-socket will further multiplex between MuWire protocol connections and HTTP1.1 requests.


### Search request propagation

Leafs send search requests to all ultrapeers they are connected to.  Ultrapeers in turn forward those queries, as well as any queries made by the local user to ALL neighboring ultrapeers, setting a flag "firstHop" to "true".  When an ultrapeer receives a query with that flag set to true, it will clear the flag and forward it only to those neighboring ultrapeers that have a keyword hit in their Bloom filter, as well as to any local leafs that match the keyword search.  When an ultrapeer receives a query with the "firstHop" flag set o false, it will only return local search results or forward the query to those leaves that have a keyword match.

This is similar but not equivalent to setting the maximum TTL in Gnutella to 1.

### Search result verification

Unlike Gnutella, MuWire nodes send search results over a streaming connection.  This is to ensure that the Destination carried in the search result cannot be spoofed, and to make blacklisting of Destinations that return undesired results effective.  To make spamming more difficult, the UI will show up to 3 search results by default from each Destination, but will give the option to display all of them.  After the results have been delivered, the streaming connection is closed.

### File transfer

Files are transferred over HTTP1.1 protocol with some custom headers added for download mesh management.  Files are requested with a GET request which includes the infohash of the file in the URL.  

### Mesh management

Download mesh management is identical to Gnutella, except instead of ip addresses b64 I2P destinations are used.  [More information](http://rfc-gnutella.sourceforge.net/developer/tmp/download-mesh.html) 

### In-Network updates

Nodes will periodically ping a pre-defined Destination for a JSON document containing the infohash of a new binary update package using signed datagrams.  If the response indicates a newer version is available, the node will issue an automatic search for that infohash and download it, then prompt the user to install the update.

### Web Of Trust

Users may choose to "Trust" or "Blacklist" Destinations that return search results.  This trust is local to the user, but it can be shared with others.  Upon installation the user picks a username which does not need to be unique, but from which a globally unique "persona" is created by concatenating the human-readable username with @ and the b32 hash of the destination of their node.  This persona is returned together with search results and when displayed in the UI the user is given the choice to trust or distrust them.
