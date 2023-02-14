# Shutdown Notice

The MuWire project has been shut down.  It is not available for download anywhere and while you are free to  build from source, it will most likely not work.

# Java I2P warning

Do NOT use the Java implementation of I2P.  Use the C++ one.  You can read more at the pastebin URLs below:

Clearnet:
```
https://paste.i2pd.xyz/?77b8e66d0f366f87#BSA1kWMHpbAjFbRVRqQdf25feRQJ1sBY4mTDVNvs5dxj
```

Inside I2P (if that site does not resolve, check reg.i2p):
```
http://privatebin.i2p/?77b8e66d0f366f87#BSA1kWMHpbAjFbRVRqQdf25feRQJ1sBY4mTDVNvs5dxj
```

In that paste I make the claim that I have donated 1000 BTC to the Java I2P project.  You can verify that by checking transaction hash:
```
0e42e4c782c73385f2c6b699da5fb81d4609bec4864550818020512bb90e30aa
```

# MuWire - Easy Anonymous File-Sharing

MuWire is a file publishing and networking tool that protects the identity of its users by using I2P technology. Anyone with a desktop computer and an internet connection can create a unique anonymous online persona and publish information of any kind without fear of censorship or persecution.  MuWire works on any platform Java works on, including Windows, MacOS, Linux, Rapsberry Pi. 

Users can then use their MuWire identities to publish files, search for files published by others, subscribe to each other’s publications and communicate through chat and messaging. Furthermore, users can establish trust-based relationship with each other where they assign a “trust level” to their contacts. 

The current stable release is available for download at https://muwire.com.  `.zip` bundles built from the latest source code are available on the [continuous integration] page.

You can find technical documentation in the [doc] folder.  Also check out the [Wiki] for various other documentation.

## Building

You need a JDK version between 11 and 18 inclusive.  Newer versions may not work.  After installing that and setting up the appropriate paths, just type

```
./gradlew clean assemble
```

If you want to run the unit tests, type
```
./gradlew clean build
```

If you want to build binary bundles that do not depend on Java or I2P, see the [muwire-pkg] project.  If you want to package MuWire for a Linux distribution, see the [Packaging] wiki page.

## Running the GUI

Type
```
./gradlew gui:run
```

The setup wizard will ask you for the host and port of an I2P or I2Pd router.

## Docker

MuWire is available as a Docker image.  For more information see the [Docker] page.

## Reproducible build

The `zip` distribution of MuWire can be build reproducibly on some systems.  For more info see the [reproducible build] page.
## Translations
If you want to help translate MuWire, instructions are on the wiki [Translate] page.

## Creating your own MuWire network
If you want to create your own MuWire network instructions are on the [Wiki].

## Related Projects
* MuWire Tracker Daemon - The MuWire Tracker Daemon (or mwtrackerd for short) is a project to bring functionality similar to BitTorrent tracking to MuWire.  For more info see the [Tracker] page.
* [MuCats] - MuCats is a project to create a website for hosting hashes of files shared on the MuWire network.
* [MuWire Seedbox Daemon](https://github.com/zlatinb/muwire-seedbox-daemon)  A headless daemon that only serves files and is controlled via a JSON-RPC interface.  Suitable for seedboxes.  
* [MuWire Seedbox Console](https://github.com/zlatinb/muwire-seedbox-console) A web console to manage one or more seedbox daemons.  


## GPG Fingerprint

```
471B 9FD4 5517 A5ED 101F  C57D A728 3207 2D52 5E41
```



[Default I2CP port]: https://geti2p.net/en/docs/ports
[Wiki]: https://github.com/zlatinb/muwire/wiki
[doc]: https://github.com/zlatinb/muwire/tree/master/doc
[muwire-pkg]: https://github.com/zlatinb/muwire-pkg 
[Packaging]: https://github.com/zlatinb/muwire/wiki/Packaging
[cli options]: https://github.com/zlatinb/muwire/wiki/CLI-Configuration-Options
[I2P Github]: https://github.com/i2p/i2p.i2p
[Plugin]: https://github.com/zlatinb/muwire/wiki/Plugin
[Docker]: https://github.com/zlatinb/muwire/wiki/Docker
[Translate]: https://wiki.localizationlab.org/index.php/MuWire
[jlesage/docker-baseimage-gui]: https://github.com/jlesage/docker-baseimage-gui
[Tracker]: https://github.com/zlatinb/muwire/wiki/Tracker-Daemon
[MuCats]: https://github.com/zlatinb/mucats
[reproducible build]: https://github.com/zlatinb/muwire/wiki/Reproducible-build
[continuous integration]: https://github.com/zlatinb/muwire/actions/workflows/gradle.yml
