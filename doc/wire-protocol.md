# MuWire network protocol

The MuWire protocol operates over a TCP-like streaming layer offered by the I2P streaming library, except for "Result" type messages which are delivered of I2P datagrams.

## Handshake

A connection begins with the word "MuWire" followed by a space and either the word "leaf" or "peer", depending on whether Alice is in a leaf or an ultrapeer role.  This allows Bob to immediately drop the connection without allocating any more resources if it is a leaf or if it does not have any more connection slots.

## Compression

All traffic after the handshake is compressed using the same compression algorithm in Gnutella.

## Messages

After the handhsake follows a stream of messages.  Messages can arrive in any order.  

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

#### "Ping"

Sent when the leaf wants to find the addresses of more ultrapeers to connect to.  Other than the header this message has no payload.

#### "Search"

Sent by a leaf when performing a search.  Contains the reply-to b64 destination for I2P datagrams.

```
{
    type : "Search",
    version: 1,
	uuid: "asdf-1234..."
    firstHop: false,
    keywords : "great speeches"
    replyTo : "asdfasf..."
}
```

### Ultrapeer to leaf

The "Search" message is also sent from an ultrapeer to a leaf.

#### Pong

A message containing addresses of other ultrapeers that the leaf is suggested to connect to.
```
{
    type: "Pong",
    version: 1,
    pongs: [ "b64.1", "b64.2" ... ]
}
```

### Between Ultrapeers

The only JSON message that can travel between ultrapeers is the "Search" message which is identical to the one sent from a leaf.

There are two types of binary messages that can travel between ultrapeers - Bloom filter and Patch.  Bloom filter should be the first message that is sent after establishing the connection, but that is not enforced.  If any Patch messages arrive before any Bloom filter has been received, they are ignored.  In the unlikely case that the size of a Patch message would exceed that of a complete Bloom filter the ultrapeer may choose to send a new Bloom filter which replaces the old one.

#### Bloom filter

This message starts with a single byte which indicates the size of the bloom filter in bits in power of 2, maximum being 22 -> 512kb.  The rest of the payload is the bloom filter itself.

#### Patch

This message starts with two unsigned bytes indicating the number of patches included in the message.  Each patch consists of 3 bytes, where the most significant bit indicates whether the corresponding bit should be set or cleared and the remaining 23 contain the position within the Bloom filter that is to be patched.

### Any node to any node over datagrams

Search results are sent in "Result" type message over an I2P datagram, uncompressed.  The format of this message is the following:

```
{
    type: "Result",
	version: 1,
	uuid: "asdf-1234-...",
    address : "asdfqwer...b64"
	results : [ 
	     {
		     name: "File name 1",
			 infohash: "asdfasdf...",
			 size: 12345,
			 pieceSize: 17,
			 altlocs: [ "b64.1", "b64.2", ...]
	     },
		 {
		     name: "File name 2",
			 infohash: "asdfasdf...",
			 size: 12345,
			 pieceSize: 17
			 altlocs: [ "b64.3", "b64.4", ... ]
		 },
		 ...
    ]
}
```

* The "uuid" field must match the uuid of the "Search" message which triggered this search result.
* The "address" field is the I2P Destination listening to incoming HTTP requests that can serve the file
* The "altlocs" list contains list of alternate locations in b64 format that may also have the file.
* The "pieceSize" field is the size of the each individual file piece (except possibly the last) in powers of 2