# MuWire - Easy Anonymous File-Sharing

MuWire is a file publishing and networking tool that protects the identity of its users by using I2P technology. Anyone with a desktop computer and an internet connection can create a unique anonymous online persona and publish information of any kind without fear of censorship or persecution.  MuWire works on any platform Java works on, including Windows, MacOS, Linux, Rapsberry Pi. 

Users can then use their MuWire identities to publish files, search for files published by others, subscribe to each other’s publications and communicate through chat and messaging. Furthermore, users can establish trust-based relationship with each other where they assign a “trust level” to their contacts. 

You can find technical documentation in the [doc] folder.  Also check out the [Wiki] for various other documentation.

## Downloading

If you prefer not to build MuWire from source, `.zip` bundles built from the latest code are available on the [continuous integration] page.


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

The setup wizard will ask you for the I2CP host and port of a Java I2P or I2Pd router.  If running I2Pd (PurpleI2P), you need to enable the `I2CP` interface.  That interface is enabled by default in the Java I2P router.

## Connecting to other MuWire nodes

At the moment there is no discovery service available so MuWire cannot connect automatically.  To work around, this, you can import the list of connections from another MuWire user.  I plan on setting up a discovery service soon.

## Reproducible build

The `zip` distribution of MuWire can be build reproducibly on some systems.  For more info see the [reproducible build] page.

## Docker

MuWire is available as a Docker image.  For more information see the [Docker] page.

## Translations
If you want to help translate MuWire, instructions are on the wiki [Translate] page.

## Creating your own MuWire network
If you want to create your own MuWire network instructions are on the [Wiki].

## Related Projects
* [MuCats] - MuCats is a project to create a website for hosting hashes of files shared on the MuWire network.
* [MuWire Seedbox Daemon](https://github.com/zlatinb/muwire-seedbox-daemon)  A headless daemon that only serves files and is controlled via a JSON-RPC interface.  Suitable for seedboxes.  


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
