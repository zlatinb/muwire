# TODO List

## Priorities
* "Blocker" - any work on the affected component(s) is blocked by this
* "High", "Medium", "Low" - as the name suggests
* "Optional" - may never happen

## Components
* "Network" - network protocol definitions
* "Core" - backend/core, excluding network protocols
* "GUI" - the Swing-based GUI
* "Plugin" - the I2P router plugin
* "Infrastructure" - the centralized infrastructure required to operate the network
* "Build scripts" - the scripts used to build MuWire (not installers)
* "Installers" - the scripts used to build installers
* "Portable" - changes required to make MuWire run as windows portable

## Backlog

|Name|GitHub issue|Component(s)|Priority|
|---|---|---|---|
|"Edit File Details" frame|N/A|GUI|High|
|Fix reproducible build on Windows| N/A| Build scripts | Low|
|Option to disable saving of search tabs| N/A | GUI | Medium |
|Progress bar while collection preview is generated | 70 | GUI | Low |
|Mark collections as inconsistent if file missing/unshared | 70? | GUI | Low|
|Magic tree expansion + expand fully in collection view | 70 | GUI | Low |
|Text on collections wizard is squished in GTK | N/A | GUI | Medium |
|Proper shutdown when disconnected from router | N/A | Core, GUI | High |
|Make sure file exists on server before sharing | N/A | Plugin | Low |
|Create shortcuts optional in windows installer | N/A | Installers | Medium |
|Bloom filters| N/A | Network, Core | Optional |
|Two-tier topology | N/A | Network, Core | Optional |
|Pings with bloom filter | N/A | Network, Core | Optional |
|Rewrite of javascript to enable strictest CSP policy | N/A | Plugin | Blocker |
|Metadata parsing and search | N/A | Core, Network? | Optional |
|Automatic adjustment of I2P tunnels | N/A | Core | Optional |
|Option to share contact list only with trusted users | N/A | Core, GUI|, Low |
|Per-file access list aka confidential files | 29 | Core, GUI | Low |
|Downloads queue with priorities | N/A | Core, GUI | Medium |
|Remote queuing of uploads | N/A | Core, Network, GUI | Medium |
|Incomplete file handling | 2 | Core, Portable | Low |
|Chat - break up lines on CR/LF | N/A | GUI | Low |
|Chat - enforce # in room names | N/A | GUI | Low |
|Chat - jump from notification window to room | N/A | GUI | Optional |
|Chat - emoji support | 113 | GUI | Low |
|Chat - auto connect server list | 105 | GUI | Low |
|I2P Status panel for external router | N/A | GUI | Low |
|Option to disable switching of tabs on actions | N/A | GUI | Low |
|Right-click paste in various text fields | N/A | GUI | Medium |
|HTML 5 media players | N/A | Plugin | Optional |
|Ability to change language after install| 109 | GUI | Medium |
|On-demand browse host|104|Network, Core, GUI | Low |
|Diacritics-insensitive filtering | 103 | GUI | Low |
|Redesign the Browse Collections tab | 92 | GUI | Medium |
|Automatically search for more sources | 75 | Core | Medium |
|Notify user collections loaded after files | 69 | GUI | Low |
|Rewrite H2HostCache to not use H2| N/A | Core | Optional|
|I18n - convert all pluralizable strings to a pattern | N/A | GUI | Medium |
