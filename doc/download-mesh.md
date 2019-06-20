# Download Mesh / Partial Sharing

MuWire uses a system similar to Gnutella's "Alternate Location" download mesh management system, however it is simplified to account for I2P's strengths and borrows a bit from BitTorrent's "Have" message.

### "X-Have" header

With every request a downloader makes it sends an "X-Have" header containing the Base64-encoded representation of a bitfield where bits set to 1 represent pieces of the file that the downloader already has.  To make partial file sharing possible, if the uploader does not have the complete file it also sends this header in every response.  If the header is missing it is assumed the uploader has the complete file.

### "X-Alt" header

The uploader can recommend other uploaders to the downloader via the "X-Alt" header.  The format of this header is a comma-separated list of Base64-encoded Personas that have previously reported having at least one piece of the file to the uploader via the "X-Have" header.

### Differences from Gnutella

Unlike Gnutella the uploader is the sole repository where possible sources of the file are tracked.  There is no negative "X-Nalt" header to prevent attacking the download mesh by mass downvoting of sources.
