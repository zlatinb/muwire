# MuWire Design

### Initial Bootstrap

Initial bootstrap into the MuWire network is done in a similar fashion to Gnutella.  Dedicated bootstrap servers ("HostCaches") listen for incoming I2P Datagrams which contain requests for addresses of ultrapeers.  The incoming datagram has a flag whether the requesting node is an ultrapeer or a leaf; if the node claims to be an ultrapeer it may be added to the list of known ultrapeers by the HostCache.  Implementors of HostCaches are free to choose other discovery strategies such as crawling, active polling and so on.

In response to the request, the HostCache sends back an I2P datagram containing b64 destinations of selected ultrapeers.  The requesting client can then choose to close the I2P tunnel that it used to send the request because it will now know about ultrapeers to connect to.

### Connectivity to peers

Each node, be it leaf or ultrapeer will open two I2P tunnels - one for incoming I2P datagrams containing search results and incoming streaming connections for HTTP file transfer requests.  In addition to this, ultrapeers will open an additional tunnel for incoming MuWire protocol connections.  This simplifies implementation because the different pseudo-sockets can have dedicated logic to handle traffic, i.e. there is no need to multiplex between MuWire protocol connections and incoming HTTP requests.
