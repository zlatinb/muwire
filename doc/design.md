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

Initial bootstrap into the MuWire network is done in a similar fashion to Gnutella.  Dedicated bootstrap servers ("HostCaches") listen for incoming I2P Datagrams which contain requests for addresses of MuWire nodes.  The incoming signed datagram has a flag whether the requesting node wishes to be added to the list of known hosts by the HostCache.  Implementors of HostCaches are free to choose other discovery strategies such as crawling, active polling and so on.

In response to the request, the HostCache sends back an I2P datagram containing Destinations of chosen nodes.  For ease of implementation, the response datagram is also signed and it's payload is JSON.

### Connectivity to peers

Each MuWire node will create a single unique I2P Destination.  Traffic to and from that destination can either be over the I2P streaming or I2P signed datagram protocols.  At the moment the signed datagram protocol is only used during the bootstrap phase and when checking for updates.

Going one level up the stack, traffic to and from the node's Destination can be either the MuWire network protocol or a protocol similar to HTTP1.1 depending on the use case.  MuWire's network protocol is documented in the `wire-protocol.md` document.

Unlike Gnutella 0.6, MuWire nodes are only ultrapeers.  This is similar to Gnutella 0.4.

##### Connectivity

Nodes will establish outgoing connections to other nodes as well as listen to incoming connections.  There exists a "quota" of allowed connections which is further divided into quotas for outgoing connections and one for incoming connections.  Whenever any of the quotas is exhausted, the node will reject further connection attempts.

Upon rejecting connection attempts the node may optionally provide a suggested list of other nodes to try.

### Search request propagation

Nodes forward queries made by the local user to ALL neighbors setting a flag "firstHop" to "true".  When receiving a query with that flag set to true, the node clears the flag and forward the modified query to a random subset of it's own neighbors.  Currently, the size of the subset is set to 2*SQRT(numConnections).  When a nodes receives a query with the "firstHop" flag set to false, it will not forward the query further. 

This is similar but not equivalent to setting the maximum TTL in Gnutella to 1.

### Search result delivery

Unlike Gnutella, MuWire nodes send search results over a streaming I2P connection.  This is to ensure that the origin of the search result cannot be spoofed, and to make blacklisting of personas that return undesired results effective. 

### File transfer

Files are transferred over HTTP1.1 protocol with some custom headers added for download mesh management.  Files are requested with a `GET` request which includes the infohash of the file in the URL.  See the `infohash-upgrade.md` document for information on the delivery of the hash list.

### Mesh management

Download mesh management is a simplified version of Gnutella's "Alternate Location" system.  For more information see the `download-mesh.md` document.

### In-Network updates

Nodes will periodically ping a pre-defined Destination for a JSON document containing the infohash of a new binary update package using signed datagrams.  If the response indicates a newer version is available, the node will issue an automatic search for that infohash and download it, then prompt the user to install the update.

