The GitHub repo is mirrored from the in-I2P GitLab repo.  Please open PRs and issues at http://git.idk.i2p/zlatinb/muwire

# MuWire - Easy Anonymous File-Sharing

MuWire is a file publishing and networking tool that protects the identity of its users by using I2P technology. Anyone with a desktop computer and an internet connection can create a unique anonymous online persona and publish information of any kind without fear of censorship or persecution.

Users can then use their MuWire identities to publish files, search for files published by others, subscribe to each other’s publications and communicate through chat and messaging. Furthermore, users can establish trust-based relationship with each other where they assign a “trust level” to their contacts. 

The current stable release - 0.8.6 is avaiable for download at https://muwire.com.  The latest plugin build and instructions how to install the plugin are available inside I2P at http://muwire.i2p.  MuWire works on any platform Java works on, including Windows, MacOS, Linux, Rapsberry Pi. 

You can find technical documentation in the [doc] folder.  Also check out the [Wiki] for various other documentation.

## Building

You need JDK 9 or newer.  After installing that and setting up the appropriate paths, just type

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

## Running the Web UI / Plugin

There is a Web-based UI under development.  It is intended to be run as a plugin to the Java I2P router.  Instructions how to build it are available at the wiki [Plugin] page.

## Running the CLI

Look inside `cli-lanterna/build/distributions`.  Untar/unzip one of the `shadow` files and then run the jar contained inside by typing `java -jar cli-lanterna-x.y.z-all.jar` in a terminal.  The CLI options are documented here [cli options]

The CLI is in maintenance mode and will not be getting any new features.

## Docker

MuWire is available as a Docker image.  For more information see the [Docker] page.

## Translations
If you want to help translate MuWire, instructions are on the wiki [Translate] page.

## Creating your own MuWire network
If you want to create your own MuWire network instructions are on the [Wiki].

## Related Projects
### MuWire Tracker Daemon
The MuWire Tracker Daemon (or mwtrackerd for short) is a project to bring functionality similar to BitTorrent tracking to MuWire.  For more info see the [Tracker] page.
### MuCats
MuCats is a project to create a website for hosting hashes of files shared on the MuWire network.  For more info see the [MuCats] project.

## GPG Fingerprint

```
471B 9FD4 5517 A5ED 101F  C57D A728 3207 2D52 5E41
```

You can find the full key at https://keybase.io/zlatinb


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
