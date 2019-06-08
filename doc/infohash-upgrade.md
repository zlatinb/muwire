# InfoHash Upgrade

An infohash is a list of hashes of the pieces of the file.  In MuWire 0.1.0 the piece size is determined by policy based on the file size, with the intention being to keep the list of hashes to maximum 128 in number.  The reason for this is that infohashes get returned with search results, and smaller piece size will result in larger infohash which will slow down the transmission of search results.

### The problem

This presents the following problem - larger files have larger piece sizes: a 2GB file will have a 16MB piece size, a 4GB file 32MB and so on.  Pieces are atomic, i.e. if a download fails halfway through a piece it will resume since the beginning of the piece.  Unfortunately in the current state of I2P the failure rate of streaming connections is too high and transmitting an entire piece over a single connection is not likely to succeed as the size of the piece increases.  This makes downloading multi-gigabyte files nearly impossible.

### Out-of-band request proposal

Barring any improvement to the reliability of I2P connections, the following approach can be used to enable smaller piece sizes and the corresponding increase in download success rate of large files:

* Search results do carry the full infohash of the file, instead they carry just the root and the number of 32-byte hashes in the infohash
* When a downloader begins a download, it issues a request for the full infohash first.  Only after that is fetched and verified, the download proceeds as usual.

Such approach is more complicated in nature than the current simplistic one, but it has the additional benefit of reducing the size of search results.

### Wire protocol changes

A new request method - "HASHLIST" is introduced.  It is immediately followed by the Base64 encoded root of the infohash and '\r\n'.  The request may contain HTTP headers in the same format as in HTTP 1.1.  One such header may be the X-Persona header, but that is optional.  After all the headers a blank line with just '\r\n' is sent.

The response is identical to that of regular GET request, with the same response codes.  The response may also carry headers, and is also followed by a blank line with '\r\n'.  What follows immediately after that is a binary representation of the hashlist.  After sending the full hashlist, the uploader keeps the connection open in anticipation of the first content GET request.

The downloader verifies the hashlist by hashing it with SHA256 and comparing it to the advertised infohash root.  If there is a match, it proceeds with the rest of the download as in MuWire 0.1.0.

### Necessary changes to MuWire 0.1.0

To accommodate this proposal in a backwards compatible manner, it is necessary to first de-hardcode the piece count computation logic which is currently hardcoded in a few places.  Then it is necessary to:

* persist the piece size to disk when a file is being shared so that it can be returned in search results
* search queries need to carry a flag of some kind that indicates support for out-of-band infohash support
  * that in turn requires nodes to support passing of that flag as the queries are being routed through the network
* the returned results need to indicate whether they are returning a full infohash or just a root; the "version" field in the json can be used for that

### Roadmap

Support for this proposal is currently intended for MuWire 0.2.0.  However, in order to make rollout smooth, in MuWire 0.1.1 support for the first two items will be introduced.  Since there already are users on the network who have shared files without persisting the size of their pieces on disk, those files will not be eligible to participate in this scheme unless re-shared (which implies re-hashing).
