# MuWire Collections
Status: Draft, Proposal, Unimplemented

MuWire collections are files containing meta-information about a grouping of files.  They serve a similar purpose like .torrent files, but the internal format is rather different to account for the MuWire identity management.

A user wishing to create a collection of files needs to have shared all the files that are going to be part of the collection.  Their full MuWire ID will be stored in the collection, so anyone wishing to download any of the files in the collection will try to download from them first.

The collection will be signed, so anyone can verify that the embedded full MuWire ID authored the collection.

### File Format

Header:

```
byte 0: Collection version, currently fixed at "1".
bytes 1,2 : unsigned 16-bit value of the number of files in the collection.  Empty files or directories are not allowed.
bytes 3-N: Full MuWire ID of the publisher of the collection, in Persona format.
bytes N+1 to N+9: Timestamp of the collection, in milliseconds since epoch UTC
bytes N+9 to M: Free-form description of the collection (comment).  Format is UTF-8, maximum size is 32kb.
```

The header is followed by a file entry for each file in the collection.  The format is the follows:

```
byte 0: File entry version, currently fixed at "1".
byte 1-33: hash of the file
byte 34: piece size power of 2
bytes 35-43: length of the file
byte 44: Unsigned 8-bit number of path elements from root to where the file will ultimately be placed upon download.
bytes 45-N : UTF-8 encoded length-prefixed path elements.  Each element can be at most 32kb long.  The last element is the name of the file.
bytes N-M: length-prefixed free from description of the file (comment).  Format is UTF-8, maximum size is 32kb.
```

After the file entries follows a footer, which is simply a signature of the byte payload of the header and the file entries.

### Downloading

Since the collection is created from individual shared files, every file within the collection is searchable.  The returned result will contain a list of infohashes of collections that the file belongs to, since a single file may participate in multiple collections.

When a node receives a query and there is keyword or infohash match for a file belonging to a collection, even if the collections is not downloaded yet it will return a search result.  

* If the query is hash and it matches the hash of the collection, all contained files are returned as results.
* If the query is keyword and it matches the general description of the collection, all contained files are returned as results.
* If the query is hash or keyword but matches only some file(s) from the collection, only that file is returned as result.  

If the user chooses to fetch the collection metafile, they will be presented with a dialog containing the metainformation contained in the collection descriptor.  They will be able to see directory structure contained in the collection and will be able to choose individual files to download, or to download the entire collection at once.

If some of the files are already downloaded but are not in the final directory location, they will be copied there.

Finally, when starting the download, the downloader always queries the persona in the collection first, regardless of who returned the search result.  

### Sharing and storage

Collection metafiles are not indexed the same way as regular files.  They are more similar to the way certificates work, i.e. they are stored in the MuWire home directory in a subdirectory called "collections".  Collections follow the naming convention "<hash of the collection>_<human-readable persona of the publisher>_<timestamp>.mwcollection".  To prevent leakage, such files are explicitly not going to be shareable.

### Fetching the descriptor

The downloader connects to the node that returned the search result which contained references to the collection(s) it is part of.  Ihen it issues a request starting with "METAFILE" followed by a space and comma-separated list of base64-encoded hashes of the referenced collections.  After the list the terminator \r\n is appended.

A set of request headers follows, each terminated by \r\n.  After the last header an additional \r\n is appended.  The headers can be any, the only one being mandatory at this time is the "Version" header, currently fixed at 1.

The uploader responds with a response code - 200 for ok or 404 for not found followed by \r\n and set of headers encoded in the same fashion.  The only mandatory headers are the "Version" currently fixed at 1 and the "Count" header which indicates how many collections are included in the body of the response.  After the headers a gzip-compressed binary stream of the following format:

```
bytes 0-32: hash of the first collection
bytes 33-N: the payload of the first collection
bytes N+1-N+33: hash of the second collection
etc.
```





