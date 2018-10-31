# MuWire Design

MuWire operates a Web-Of-Trust layer on top of a Gnutella-like topology on top of I2P.  To understand the rest of this document, it is important to understand the following terms:

* I2P terms:
  * Destination - a cryptographic abstraction which represents a destination in the I2P cryptospace.  It is not possible to correlate a destination with an IP address in the clearnet.  While IP addresses and I2P Destinations share a lot in common, there are some important differences.  The one most crucial to the design of MuWire is that I2P Destinations are always directly reachable, i.e. they are never behind a firewall.
  * I2P datagram - similar to clearnet UDP datagram.  Can be signed or raw; MuWire only uses signed datagrams, which are datagrams containing the sender's Destination and can be replied to.
  * I2P streaming - similar to clearnet TCP/IP protocol.
* Gnutella terms:
  * Leaf - a node in the Gnutella network which indexes only content shared by the local user.  Leafs do not route queries and connect only to ultrapeers
  * Ultrapeer - a node in the Gnutella network which listens for incoming connections from leafs, indexes (loosely) their content and routes search queries to other ultrapeers and leafs.
* MuWire Persona - each MuWire node operates a single Persona.  For complete definition, see the "web-of-trust" document.

### Initial Bootstrap

Initial bootstrap into the MuWire network is done in a similar fashion to Gnutella.  Dedicated bootstrap servers ("HostCaches") listen for incoming I2P Datagrams which contain requests for addresses of ultrapeers.  The incoming signed datagram has a flag whether the requesting node is an ultrapeer or a leaf; if the node claims to be an ultrapeer it may be added to the list of known ultrapeers by the HostCache.  Implementors of HostCaches are free to choose other discovery strategies such as crawling, active polling and so on.

In response to the request, the HostCache sends back an I2P datagram containing Destinations of chosen ultrapeers.  For ease of implementation, the response datagram is also signed and it's payload is JSON.

### Connectivity to peers

Each MuWire node will create a single unique I2P Destination.  Traffic to and from that destination can either be over the I2P streaming or I2P signed datagram protocols.  At the moment the signed datagram protocol is only used during the bootstrap phase.

Going one level up the stack, traffic to and from the node's Destination can be either the MuWire network protocol or HTTP1.1 protocols, depending on the use case.  MuWire's network protocol is documented in the "wire-protocol" document.

Similar to Gnutella, MuWire nodes can be either leafs or ultrapeers.

##### Leaf connectivity

If the node is a leaf, it will only establish outgoing connections to a small number of ultrapeers.  In Gnutella that number was between 2 and 5.  The node will not accept any incoming connections over the MuWire protocol.

##### Ultrapeer connectivity

Ultrapeer nodes will establish outgoing connections to other ultrapeers as well as listen to incoming connections from leafs and ultrapeers.  Ultrapeers have a "quota" of allowed leaf and ultrapeer connections.  The quota for ultrapeer connections is further divided into quotas for outgoing connections and one for incoming connections.  Whenever any of the quotas is exhausted, the node will reject further connection attempts

### Search request propagation

Leafs send search requests to all ultrapeers they are connected to.  Ultrapeers in turn forward those queries, as well as any queries made by the local user to ALL neighboring ultrapeers, setting a flag "firstHop" to "true".  When an ultrapeer receives a query with that flag set to true, it will clear the flag and forward it only to those neighboring ultrapeers that have a keyword hit in their Bloom filter, as well as to any local leafs that match the keyword search.  When an ultrapeer receives a query with the "firstHop" flag set o false, it will only return local search results or forward the query to those leaves that have a keyword match.

This is similar but not equivalent to setting the maximum TTL in Gnutella to 1.

### Search result verification

Unlike Gnutella, MuWire nodes send search results over a streaming I2P connection.  This is to ensure that the persona carried in the search result cannot be spoofed, and to make blacklisting of personas that return undesired results effective.  To make spamming less efficient, the UI will show up to 3 search results by default from each persona, but will give the option to display all of them.  After the results have been delivered, the streaming connection is closed.

### File transfer

Files are transferred over HTTP1.1 protocol with some custom headers added for download mesh management.  Files are requested with a GET request which includes the infohash of the file in the URL.  The URL itself is encrypted with the public key of the target persona to prevent carpet-bombing the network with GET requests.

### Mesh management

Download mesh management is identical to Gnutella, except instead of ip addresses MuWire personas are used.  [More information](http://rfc-gnutella.sourceforge.net/developer/tmp/download-mesh.html) 

### In-Network updates

Nodes will periodically ping a pre-defined Destination for a JSON document containing the infohash of a new binary update package using signed datagrams.  If the response indicates a newer version is available, the node will issue an automatic search for that infohash and download it, then prompt the user to install the update.

### Web Of Trust

Users may choose to "Trust" or "Blacklist" Destinations that return search results.  This trust is local to the user, but it can be shared with others.  Each user is assigned a perona which is returned together with search results and when displayed in the UI.  For more information see the web-of-trust document.
