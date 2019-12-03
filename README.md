# MuWire - Easy Anonymous File-Sharing

MuWire is an easy to use file-sharing program which offers anonymity using [I2P technology](http://geti2p.net).  It works on any platform Java works on, including Windows,MacOS,Linux.

It is inspired by the LimeWire Gnutella client and developped by a former LimeWire developer.

The current stable release - 0.6.7 is avaiable for download at https://muwire.com.  You can find technical documentation in the [doc] folder.  Also check out the [Wiki] for various documentation.

### Building

You need JDK 9 or newer.  After installing that and setting up the appropriate paths, just type

```
./gradlew clean assemble
```

If you want to run the unit tests, type
```
./gradlew clean build
```

If you want to build binary bundles that do not depend on Java or I2P, see the [muwire-pkg] project

### Running the GUI

Type
```
./gradlew gui:run
```

If you have an I2P router running on the same machine that is all you need to do.  If you use a custom I2CP host and port, create a file `i2p.properties` and put `i2cp.tcp.host=<host>` and `i2cp.tcp.port=<port>` in there.  On Windows that file should go into `%HOME%\AppData\Roaming\MuWire`, on Mac into `$HOME/Library/Application Support/MuWire` and on Linux `$HOME/.MuWire`

[Default I2CP port]\: `7654`

### Running the CLI

Look inside `cli-lanterna/build/distributions`.  Untar/unzip one of the `shadow` files and then run the jar contained inside by typing `java -jar cli-lanterna-x.y.z-all.jar` in a terminal.  The CLI will ask you about the router host and port on startup, no need to edit any files.  However, the CLI does not have an options window yet, so if you need to change any options you will need to edit the configuration files.  The CLI options are documented here [cli options]

The CLI is under active development and doesn't have all the features of the GUI.

### Running the Web UI / Plugin

There is a Web-based UI under development.  It is intended to be run as a plugin to the Java I2P router.  To build it, do the following:
1. Check out the Java I2P code from here [I2P Github]
2. Build it using `ant clean pkg`
3. Create a directory under the MuWire source tree called `i2pjars` and copy all jars from the i2p `pkg-temp/lib` directory there.
4. Un-comment the `webui` and `plug` projects in MuWire's `settings.gradle`.  (By default those are commented out)
5. Edit `gradle.properties` to change the signer.  You need to sign your plugin for the I2P router to load it.  More information TODO
6. Run `./gradlew assemble`
7. If all goes well, there will be a file `plug/build/MuWire.su3`.  Install that file from the I2P router console.


### GPG Fingerprint

```
471B 9FD4 5517 A5ED 101F  C57D A728 3207 2D52 5E41
```

You can find the full key at https://keybase.io/zlatinb


[Default I2CP port]: https://geti2p.net/en/docs/ports
[Wiki]: https://github.com/zlatinb/muwire/wiki
[doc]: https://github.com/zlatinb/muwire/tree/master/doc
[muwire-pkg]: https://github.com/zlatinb/muwire-pkg 
[cli options]: https://github.com/zlatinb/muwire/wiki/CLI-Configuration-Options
[I2P Github]: https://github.com/i2p/i2p.i2p
