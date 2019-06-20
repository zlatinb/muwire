# MuWire - Easy Anonymous File-Sharing

MuWire is an easy to use file-sharing program which offers anonymity using [I2P technology](http://geti2p.net).  It works on any platform Java works on, including Windows,MacOS,Linux.

It is inspired by the LimeWire Gnutella client and developped by a former LimeWire developer.

The current stable release - 0.2.5 is avaiable for download at http://muwire.com.  You can find technical documentation in the "doc" folder.

### Building

You need JRE 8 or newer.  After installing that and setting up the appropriate paths, just type

```
./gradlew clean assemble 
```

If you want to run the unit tests, type
```
./gradlew clean build
```

Some of the UI tests will fail because they haven't been written yet :-/

### Running

You need to have an I2P router up and running on the same machine.  After you build the application, look inside "gui/build/distributions".  Untar/unzip one of the "shadow" files and then run the jar contained inside by typing "java -jar MuWire-x.y.z.jar" in a terminal or command prompt.  If you use a custom I2CP host and port, create a file $HOME/.MuWire/i2p.properties and put "i2cp.tcp.host=<host>" and "i2cp.tcp.port=<port>" in there.

The first time you run MuWire it will ask you to select a nickname.  This nickname will be displayed with search results, so that others can verify the file was shared by you.  It is best to leave MuWire running all the time, just like I2P.


### Known bugs and limitations

* Many UI features you would expect are not there yet

### Quick FAQ

* why is MuWire slow ?
  
  - too few sources you're downloading from
  - you can increse the number of tunnels by using more tunnels via Options->I2P Inbound/Outbound Quantity 
    the default is 4 and you could raise it and even can go up as high as 16 ( Caution !!!!)

* my search is not returning (enough) results !

  - search is keyword or hash based
  - keywords and hash(es) are NOT regexed or wildcarded so they have to be complete
    so searching for 'musi' will not return results with 'music' - you have to search for 'music'
  - ALL keywords have to match 
  - only use <SPACE> for keyword separation
