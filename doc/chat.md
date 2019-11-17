# MuWire Chat System

Since version 0.6.3 MuWire comes with a built in chat system.  It is very similar to the way IRC operates, and the user experience mimics that of IRC as well.

### Design

The chat system uses a client-server model.  Each MuWire node can run a chat server which accepts incoming connections; clients wishing to connect to a chat server establish an outgoing streaming connection to the destination where the chat server is running.  The local client also connects to server through a special "loopback" connection.

Once connected, the client automatically joins a special room called "__CONSOLE__" which is the server console.  In that room users can issue certain commands, but cannot actually chat.  In order to chat, the client needs to `/join` a chat room first.  The chat room is kept as state in the server and any messages sent to that chat room are forwarded to all other users who have joined the same room.  When the last member of a room leaves, the room state is destroyed server-side.

Private messages work by replacing the room of the message with the base64-encoded persona of the recipient of the message. 


### Chat Commands

Clients issue commands to the chat server in order to perform operations.  Some commands can only be issued in the __CONSOLE__ room, others only in a regular chat room.  The server will ignore commands which are not issued in the appropriate place.

There are several chat commands that MuWire supports, more can be added later.  Commands consist of a prefix and payload.  The prefix always beings with forward slash `/`.  Below is the list of commands a MuWire chat server supports as of version 0.6.6:

/HELP - this command can be issued only in the __CONSOLE__ room.  It results in the server echoing back a help message of the commands it supports.
/SAY - this command can be issued only in a regular chat room or private chat.  It's payload is the content of what the user wishes to say.
/INFO - this command can be issued only in the __CONSOLE__ room.  It results in the server printing a status message.  As of 0.6.6, this consists of the base64-encoded address of the server as well as a list of user who are currently connected.
/LIST - this command can be issued only in the __CONSOLE__ room.  It results in the server echoing the list of rooms which currently have at least one member.
/JOIN - this command can be issued only in the __CONSOLE_ room.  The payload of the command is the name of the room that the user wishes to join.  This results in server-side state being updated to add the user to the membership list of the room.
/LEAVE - this command can be issued only in a regular room.  It has no payload, and the result is that the server removes the user issuing the command from the room.
/TRUST - this command can be issued only in the __CONSOLE__ room and only over the loopback connection, i.e. it is reserved for the owner of the server.  It's payload is the human-readable representation of a user the owner wishes to mark as trusted.  It results in adding the specified user to the owner's trust list.
/DISTRUST - similar to /TRUST, this command results in the opposite; the user specified in the payload being added to the distrusted list.  This also results in the user getting disconnected from the server, i.e. kick/ban-ned.

There is a command called "/JOINED" which is issued from the server to the client upon the client joining a room.  The payload of the command is a comma-separated list of base64-encoded representations of the personas of the users already in that room.

### Protocol

The client wishing to connect to a server establishes an I2P connection and sends the letters "IRC\r\n" in ASCII encoding.  These are followed by one more headers, each header consisting of a name, followed by colon, followed by value, terminated with "\r\n".  After all headers have been sent, an additional "\r\n" is written to the socket.  

As of version 0.6.6 the following headers are required:

"Version" - this header indicates the version of the chat protocol that will be used over this connection.  Currently fixed at 1.
"Persona" - this header contains the base64-encoded representation of the persona of the client.

The server responds with a status code encoded as an aSCII string, terminated with "\r\n", which can be one of the following:

200 - connection accepted
400 - connection not allowed.  This can be issued if the server is down for example.
429 - connection rejected.  This can be issued when the server is overloaded or the client is already connected to the server.  Clients are encouraged to not re-attempt connecting for a short period of time.

After the code, the server responds with a "Version" header followed by a "\r\n" on an empty line.

### Messages

After the headers have been exchanged, the connection starts transmitting messages back and forth.  Messages are encoded in UTF-8 JSON format, and preceeded by two bytes which are the unsigned representation of the number of bytes of JSON.

As of protocol version 1, the following messages are supported:

##### "Keepalive Ping".  
This message serves only to prevent the blocking read from I2P sockets from timing out and is sent on regular intervals by both the server and the client.  Example payload of such message is:
```
{
    "type" : "Ping",
    "version" : 1
}
```

##### "Chat Command"  
This message is sent by both server and client whenever an event occurs, such as user issuing a command, or another user in a room the user has joined issues a command.  The payload is the following:
```
{
    "type" : "Chat",
    "uuid" : "1234-asdf-...",  // unique random UUID of this message
    "host" : "asdf123..",      // base64-encoded persona of the server owner, i.e. the server this message is destined to
    "sender" : "asdf123...",   // base64-encoded persona of the sender of the message.  The server verifies it matches the destination of the I2P socket it was received from.
    "chatTime" : 1235...,      // time since epoch in milliseconds when the message was sent.
    "room" : "asdf..."         // UTF-8 string indicating the room this message is destined to
    "payload" : "/SAY asdf..." // UTF-8 string of the chat command being issued by the user.
    "sig" : "asdf1234..."      // base64-encoded signature.
}
```
In order to prevent spoofing and replay attacks, each Chat Command message contains a signature.  The signature covers the following fields in this order:

uuid - toString() representation of the UUID
host - binary representation of the persona in the host field
sender - binary representation of the persona in the sender field
chatTime - big endian representation of the timestamp of the message (8 bytes)
room - UTF-8 representation of the room field
payload - UTF-8 representation of the payload field.

The signature is created with the signing private key (SPK) of the sender.

##### "Leave"
This message is only sent from a server to a client, whenever another client disconnects from the server.  It's format is the following:
```
{
    "type" : "Leave,
    "persona" : "asdf1234..." // base64-encoded persona of the user being disconnected from the server.
}
```

### Future Work
It is possible to extend this protocol to support inter-server relaying of messages.  Because every Chat Command message is signed, it will not be possible for malicious server operators to spoof its contents.
