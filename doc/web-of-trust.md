# MuWire Web-Of-Trust

### Definition of a Persona

MuWire assumes that each human user is going to have a single persona.  A persona consists of the following three items:
* A b64 I2P destination - this is the destination where the MuWire node is listening for incoming requests.
* A human-readable nickname 
* A public key

For UI purposes, the persona will be displayed as the human-readable nickname plus underscore followed by the first 32 bytes of the b32 I2P address derived from the b64 destination.  The intention is to make it easier for users to differentiate between personas, but at the same to prevent spoofing of personas through mass generation of I2P destinations.

### Trusting other personas

Trust between personas is only built manually as a result of user action in the UI.  Whenever a search result is displayed to the user, the persona of the responder is displayed as well.  The user then has the choice to trust or distrust the sender of the search result.  

In order for that to work it is necessary that search results carry the complete persona of the responder.  Since they are returned via the I2P streaming library, the b64 of the responder is already known and verified to be valid.  In order to complete the persona, the first things transmitted on the wire are the human-readable nickname and the public key.

Users are encouraged to only trust after successfully downloading a file and verifying that it is genuine.  Future search results from trusted personas are highlighted in the UI and users are encouraged to download them instead of untrusted ones.  This helps control the spread of viruses and fake files.

### Sharing trust lists

In order to enable users to make better choices whom to trust, the protocol allows for exchange of trust lists.  Users can query the trust lists of other personas through an UI action.  There are two types of queries - "who do you trust" and "who trusts you" which are executed as GET requests to the b64 address of the persona being queried

##### "Who do you trust" query

The response is a list of full persona details that the responder currently trusts.

##### "Who trusts you" query and certificates

This query is more informative when making the decision whether to trust someone, so it is important for it to be more difficult to abuse.  To provide protection against abuse, the response body contains trust "certificates" instead of raw personas.

### Definition of a trust certificate

A trust certificate is a signed document stating that Alice trusts Bob.  If Charlie has Alice's persona details, he can verify that trust even if he does not know anything about Bob.  To make that possible, a trust certificate contains:
1. Alices' persona details
2. Bob's persona details
3. Timestamp of the creation of the certificate
3. Alices' signature of the above.

The timestamp serves only informational purposes, i.e. there is no built-in expiration of certificates.

### Propagation of trust certificates in the network

Trust certificates are generated whenever a user chooses to trust someone.  That action triggers the creation of the certificate and the upload of the certificate via a POST call to the persona being trusted.

The other method of propagation is the "Who trusts you" query.  Since the certificate contains full persona details of the trustee, it enables the user to "crawl" the trust graph of the network without issuing search queries.
