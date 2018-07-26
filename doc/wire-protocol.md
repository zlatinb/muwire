# MuWire network protocol

The MuWire protocol operates over a TCP-like streaming layer offered by the I2P streaming library.

## Handshake

A connection begins with the word "MuWire" followed by a space and one of the following words: "leaf", "peer" or "results", depending on whether Alice is in a leaf, ultrapeer or responder role.  This allows Bob to immediately drop the connection without allocating any more resources.  

If Bob is an ultrapeer he responds to the handshake by either accepting it or rejecting it and optionally suggesting other ultrapeers to connect to.

Accepting the handshake is done by responding with a single word "OK".

Rejecting the handshake is done by responding with the word "REJECT", optionally followed JSON payload prefixed by two unsigned bytes indicating the length of the JSON document.  In that document, Bob can suggest ultrapeers to connect to in b64 format:

```
{
    tryHosts: [b64.1, b64.2...]
}
```

## Compression

All traffic after the handshake is compressed using the same compression algorithm in Gnutella.

## Messages

After the handhsake follows a stream of messages.  Messages can arrive in any order.  Every 10 seconds a "Ping" message is sent on each connection which serves as a keep-alive.  The response to that message is a "Pong" which carries b64 destinations of ultrapeers the remote side has been able to connect to.  This keeps the host pool in each node fresh.

Between ultrapeers, each message consists of 3 bytes - the most significant bit of the first byte indicates if the payload is binary or JSON.  The remaining 23 bits indicate the length of the message.

Between leaf and ultrapeer, each message consists of 2 bytes unsigned payload length followed by the JSON payload.

The JSON structure has two mandatory top-level fields - type and version:

```
{
    type : "MessageType",
    version : 1,
    ...
}
```

Binary messages can be two types: full bloom filter or a patch message to be applied to a previously sent bloom filter.  Binary messages travel only between ultrapeers.  There is a single byte after the payload indicating the type of the binary message.  That byte is counted in the total payload length.

#### "Ping"

Sent periodically as a keep-alive on every type of connection.  Other than the header this message has no payload.

#### Pong

A message containing addresses of other ultrapeers that the sender has successfully connected to.
```
{
    type: "Pong",
    version: 1,
    pongs: [ "b64.1", "b64.2" ... ]
}
```

### Leaf to ultrapeer

#### "Upsert"

This message is sent from a leaf to ultrapeer to indicating that the leaf is sharing a given file:

```
{
    type : "Upsert",
    version : 1,
    infoshash : "asdfasf...",
    names : [ "file name 1", "file name 2"...]
}
```

Multiple file names per infohash are allowed.  In future versions this message may be extended to carry metadata such as artist, album and so on.

#### "Delete"

The opposite of Upsert - the leaf is no longer sharing the specified file.  The file is identified only by the infohash.

```
{
    type: "Delete",
    version: 1,
    infohash "asdfasdf..."
}
```

#### "Search"

Sent by a leaf or ultrapeer when performing a search.  Contains the reply-to b64 destination for signed I2P datagrams.

```
{
    type : "Search",
    version: 1,
	uuid: "asdf-1234..."
    firstHop: false,
    keywords : "great speeches",
    infohash: "asdfasdf...",
    replyTo : "asdfasf...b64"
}
```

A search can contain either a list of keyword or the infohash if the user is looking for a specific file.  If both are present, the infohash takes precedence and the keywords are ignored.

### Ultrapeer to leaf

The "Search" message is also sent from an ultrapeer to a leaf.

### Between Ultrapeers

The only JSON message other than "Ping" and "Pong" that can travel between ultrapeers is the "Search" message which is identical to the one sent from a leaf.

There are two types of binary messages that can travel between ultrapeers - Bloom filter and Patch.  Bloom filter should be the first message that is sent after establishing the connection, but that is not enforced.  If any Patch messages arrive before any Bloom filter has been received, they are ignored.  In the unlikely case that the size of a Patch message would exceed that of a complete Bloom filter the ultrapeer may choose to send a new Bloom filter which replaces the old one.

#### Bloom filter

This message starts with a single byte which indicates the size of the bloom filter in bits in power of 2, maximum being 22 -> 512kb.  The rest of the payload is the bloom filter itself.

#### Patch

This message starts with two unsigned bytes indicating the number of patches included in the message.  Each patch consists of 3 bytes, where the most significant bit indicates whether the corresponding bit should be set or cleared and the remaining 23 contain the position within the Bloom filter that is to be patched.

### Search results - any node to any node

Search results are sent in "results" type connection from the responder to the originator of the query.  This connection is uncompressed and only Alice sends any data on it, i.e. there is no handshake.  The first thing that Alice sends is the UUID of the search that generated the results.  This allows Bob to drop the connection if he does not recognize that UUID.  After that, Alice sends a stream containing JSON messages prefixed by two unsigned bytes indicating the length of each message.  The format is the following:  

```
{
    type: "Result",
	version: 1,
	name: "File name",
	infohash: "asdfasdf..."
	size: 12345,
	pieceSize: 17,
	hashList: [ "asdfasdf...", "asdfasdf...", ... ]
	altlocs: [ "b64.1", "b64.2", ... ]
}
```
* The "hashList" list contains the list of hashes that correspond to the pieces of the file
* The "altlocs" list contains list of alternate locations in b64 format that Alice thinks may also have the file.
* The "pieceSize" field is the size of the each individual file piece (except possibly the last) in powers of 2

# HostCache protocol

### Node to HostCache

Nodes send a "Ping" message to the hostcache, enriched with a boolean "leaf" field indicating whether the node is a leaf or not:

```
{
    type: "Ping",
    version: 1,
    leaf: true
}
```

### HostCache to Node

The HostCache replies with a "Pong" message.
