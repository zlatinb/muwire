# MuWire - Easy Anonymous File-Sharing

MuWire is an easy to use file-sharing program which offers anonymity using [I2P technology](http://geti2p.net).  It works on any platform Java works on, including Windows,MacOS,Linux.

The current stable release - 0.6.8 is avaiable for download at https://muwire.com.  The latest plugin build and instructions how to install the plugin are available inside I2P at http://muwire.i2p.  

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

If you want to build binary bundles that do not depend on Java or I2P, see the [muwire-pkg] project

## Running the GUI

Type
```
./gradlew gui:run
```

If you have an I2P router running on the same machine that is all you need to do.  If you use a custom I2CP host and port, create a file `i2p.properties` and put `i2cp.tcp.host=<host>` and `i2cp.tcp.port=<port>` in there.  On Windows that file should go into `%HOME%\AppData\Roaming\MuWire`, on Mac into `$HOME/Library/Application Support/MuWire` and on Linux `$HOME/.MuWire`

[Default I2CP port]\: `7654`

## Running the CLI

Look inside `cli-lanterna/build/distributions`.  Untar/unzip one of the `shadow` files and then run the jar contained inside by typing `java -jar cli-lanterna-x.y.z-all.jar` in a terminal.  The CLI will ask you about the router host and port on startup, no need to edit any files.  However, the CLI does not have an options window yet, so if you need to change any options you will need to edit the configuration files.  The CLI options are documented here [cli options]

The CLI is under active development and doesn't have all the features of the GUI.

## Running the Web UI / Plugin

There is a Web-based UI under development.  It is intended to be run as a plugin to the Java I2P router.  Instructions how to build it are available at the wiki [Plugin] page.

## Docker

The Docker image is based on the wonderful work in [jlesage/docker-baseimage-gui].
You can refer to it for environment variables to pass to the container.

If you don't want to use the image on dockerhub, build an image yourself.
```bash
MUWIRE_VERSION=`awk -F "=" '/^version/ { gsub(" ","") ; print $2}' gradle.properties`
docker build -t muwire:latest,muwire:${MUWIRE_VERSION} .
```

**Necessary configuration**

Since MuWire will be running in a container, it won't have direct access to the host's localhost.
By default, it will be configured to use `172.17.0.1` as the target host.  
You'll need to open the I2CP port on that interface.
If you're running I2P on the localhost, navigate to http://localhost:7657/configi2cp and make the necessary changes.

![i2cp_config.png]

Should you be using a different interface write an `i2p.properties` and then put that into the shared docker volume.

Example configuration file:
```properties
i2cp.tcp.host=112.13.0.1
```

**Running**

```bash
docker run \
    -p 5800:5800 \
    -v config:/muwire/.MuWire \
    -v incompletes:/incompletes \
    -v output:/output \
    --name muwire \
    muwire
```

You will then be able to access the muwire GUI over a browser at http://localhost:5800

**Options**


| Option       | Description                                  |
|--------------|--------------------------------------------|
|`-v config:/muwire/.MuWire`| This is where the `i2p.properties` and possibly other config should go |
|`-v incompletes:/incompletes`| The `/incompletes` volume should be used to store MuWire's **incomplete** download/upload data \*|
|`-v output:/output`| The `/output` volume should be used to store MuWire's download/upload data |

Beware, **by default** incomplete downloads are put in `/muwire/.MuWire/incompletes`.
It might be prudent to use the `/incompletes` volume and configure MuWire accordingly.
Your config volume might otherwise fill up.

![muwire_incompletes.png]

## Translations
If you want to help translate MuWire, instructions are on the wiki https://github.com/zlatinb/muwire/wiki/Translate

## GPG Fingerprint

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
[Plugin]: https://github.com/zlatinb/muwire/wiki/Plugin
[i2cp_config.png]: ./images/i2cp_config.png
[muwire_incompletes.png]: ./images/muwire_incompletes.png
[jlesage/docker-baseimage-gui]: https://github.com/jlesage/docker-baseimage-gui
