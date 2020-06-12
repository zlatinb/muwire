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
byte 34: Unsigned 8-bit number of path elements from root to where the file will ultimately be placed upon download.
bytes 35-N : UTF-8 encoded length-prefixed path elements.  Each element can be at most 32kb long.  The last element is the name of the file.
bytes N-M: free from description of the file (comment).  Format is UTF-8, maximum size is 32kb.
```

After the file entries follows a footer, which is simply a signature of the byte payload of the header and the file entries.

### Downloading

Since the collection is created from individual shared files, every file within the collection is searchable.  It is possible to extend the shared file data structure to contain refererences to any collections the file belongs to - TBD.

When a user searches for a keyword or hash, they can find either the collection metafile itself or a file which is a member of one or more collections.  In the latter case, the user is given the option to download the collection metafile.

If the user chooses to download the collection metafile, they will be presented with a dialog containing the metainformation contained in the collection descriptor.  They will be able to see directory structure contained in the collection and will be able to choose individual files to download.

TBD - what happens when some of the files are already downloaded but are not in the final directory location?

Finally, when starting the download, the downloader always queries the persona in the collection first, regardless of who returned the search result.  

### Sharing

When downloading the collection descriptor, the user makes the descriptor available for indexing.  This way collection descriptors can propagate on the network.
TBD - do they also index the comments and file names in the descriptor, even if they haven't downloaded the files?

