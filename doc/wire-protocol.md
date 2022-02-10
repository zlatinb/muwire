# MuWire network protocol

The MuWire protocol operates over a TCP-like streaming layer offered by the I2P streaming library.

## Handshake

A connection begins with the word "MuWire" followed by a space and one of the following words: `leaf` o `peer`.  Currently only `peer` is supported.

Bob responds to the handshake by either accepting it or rejecting it and optionally suggesting other nodes to connect to.

Accepting the handshake is done by responding with a single word "OK".

Rejecting the handshake is done by responding with the word "REJECT", optionally followed JSON payload prefixed by two unsigned bytes indicating the length of the JSON document.  In that document, Bob can suggest nodes to connect to in I2P b64 format:

```
{
    tryHosts: [b64.1, b64.2...]
}
```

## Compression

All traffic after the handshake is compressed using the same compression algorithm in Gnutella.

## Blobs in JSON 

All protocol elements that are represented as blobs (personas, certificates, file names, seach terms) get Base64 encoded when transmitted as part of JSON documents.

## Internationalization support

All protocol elements that may contain non-ascii characters (file names, search terms, persona nicknames) are represented as a binary blob with the following format:
```
bytes 0 and 1: unsigned length of the binary representation of the string
bytes 2 to N+2: binary representation of the string in UTF-8 encoding
```

## Persona wire format
(See the "web-of-trust" document for the definition of a MuWire persona).

A persona is represented as a blob with the following format:
```
byte 0: unsigned version number of the format.  Currently fixed at 1.
bytes 1 to N: nickname of the persona in internationalized format
bytes N+1 to M: the I2P destination of the persona
bytes M+1 to M+33: signature of bytes 0 to M
```
## Certificate wire format (not implemented)
(See the "web-of-trust" document for the definition of a MuWire certificate)

A certificate is represented as a blob with the following format:
```
byte 0: unsigned version number of the format.  Currently fixed at 1.
bytes 1 to 8: timestamp in milliseconds the certificate was created
bytes 9 to N: Alice's persona
bytes N+1 to M: Bob's persona
bytes M+1 to O: signature of bytes 0 to M, signed by Alice (length TBD)
```
## Messages

After the handhsake follows a stream of messages.  Messages can arrive in any order.  Every 40 seconds a `Ping` message is sent on each connection which serves as a keep-alive.  The response to that message is a `Pong` which carries b64 destinations of ultrapeers the remote side has been able to connect to.  This keeps the host pool in each node fresh.


Each message begins with 3 bytes - the most significant bit of the first byte indicates if the payload is binary or JSON.  The remaining 23 bits indicate the length of the message.


The JSON structure has two mandatory top-level fields - type and version:

```
{
    type : "MessageType",
    version : 1,
    ...
}
```

### "Ping"

Sent periodically as a keep-alive.

To prevent replay attacks, each `Ping` carries a randomly generated UUID which serves as a nonce and is echoed in the `Pong`.  If the `Ping` does not have such an UUID, it is an indication that a `Pong` is not being solicited, for example when the node has reached it's desired number of connections.

### Pong

A message containing addresses of other ultrapeers that the sender has successfully connected to.  See `binary-pongs.md` for updates to this format.
```
{
    type: "Pong",
    version: 1,
    pongs: [ "b64.1", "b64.2" ... ]
}
```

### "Search"

Sent by a nodewhen performing a search.  Contains the reply-to persona of the originator (base64-encoded).

```
{
    type : "Search",
    version: 1,
    uuid: "asdf-1234..."
    firstHop: boolean,
    keywords : ["keyword1","keyword2"...]
    infohash: "asdfasdf...",
    replyTo : "asdfasf...b64",
    originator : "asfasdf...",
    oobHashlist : boolean,
    searchComments : boolean,
    compressedResults : boolean,
    collections : boolean,
    searchPaths : boolean,
    regex : boolean,
    sig : base64-encoded byte array,
    sig2 : base64-encoded byte array
}
```

A search can contain either the query entered by the user in the UI or the infohash if the user is looking for a specific file.  If both are present, the infohash takes precedence and the keyword query is ignored.

* `originator` field contains the Base64-encoded persona of the originator of the query.  It is used for display purposes only.  The I2P destination in that persona must match the one in the `replyTo` field.  Since MuWire 0.8.12 that destination is also checked against the address of the streaming connection if the `firstHop` flag is set.
* `oobHashlist` flag indicates support for out-of-band hashlist delivery
* `searchComments` flag indicated whether the responding node should also search in the comments associated with shared files as opposed to just their names.
* `compressedResults` flag indicates support for receiving compressed results
* `collections` flag indicates whether the responding node should also search in any collection names.  See `collections.md` for more information.  If combined with `searchComments` flag then comments of collections are searched too.
* `searchPaths` flag indicates whether the responding node should search in the fully-shared parts of the paths to its shared files
* `regex` flag indicates that the first entry in the `keywords` array is to be treated as a regular expression.
* `sig` is a Base64-encoded signature of the search terms (TODO: document properly)
* `sig2` is a Base64-encoded signature of the UUID and timestamp of the query (TODO: document properly)


### Search results

Search results are sent through `POST`(uncompressed, pending deprecation) or `RESULTS`(compressed, preferred) methods from the responder to the originator of the query.  The URL is the UUID of the search that prompted ther response.  The first thing sent on it is the persona of the responder in binary.  That is followed by two unsigned bytes containing the number of search results.  After that follows a stream containing JSON messages prefixed by two unsigned bytes indicating the length of each message.  The format is the following:

```
{
    type: "Result",
	version: 1,
	name: "File name (i18n encoded)",
	infohash: "asdfasdf..."
	size: 12345,
	pieceSize: 17,
	hashList: [ "asdfasdf...", "asdfasdf...", ... ]
	altlocs: [ "persona.1.b64", "persona.2.b64", ... ]
}
```
* The "hashList" list contains the list of hashes that correspond to the pieces of the file
* The "altlocs" list contains list of alternate personas that the responder thinks may also have the file.
* The "pieceSize" field is the size of the each individual file piece (except possibly the last) in powers of 2

Results version 1 contain the full hashlist, version 2 does not contain that list.  See the `infohash-upgrade.md` document for more information.

### "Who do you trust" query 
(See the "web-of-trust" document for more info on this query)

This is a `TRUST` request.

### "Who trusts you" (not implemented)
(See the "web-of-trust" document for more info on this query)

This is a GET request with the URL "/who-trusts-you".  The response is a binary stream of certificate details.

### "Browse host"

This is a `BROWSE` request. TODO: document better

# HostCache protocol

### Node to HostCache

Nodes send a `Ping` message to the hostcache

```
{
    type: "Ping",
    version: 1,
    leaf: boolean
}
```
The `leaf` field is ignored.

### HostCache to Node

The HostCache replies with a "Pong" message.
