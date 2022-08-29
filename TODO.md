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
|Rewrite download peristence|167|Core|High|
|Redesign options window|N/A|GUI|Medium|
|Free space check before dnwnload|N/A|Core, GUI|Medium|
|"Edit File Details" frame|N/A|GUI|High|
|Fix reproducible build on Windows| N/A| Build scripts | Low|
|Option to disable saving of search tabs| N/A | GUI | Medium |
|Progress bar while collection preview is generated | 70 | GUI | Low |
|Mark collections as inconsistent if file missing/unshared | 70? | GUI | Low|
|Magic tree expansion + expand fully in collection view | 70 | GUI | Low |
|Text on collections wizard is squished in GTK | N/A | GUI | Medium |
|Create shortcuts optional in windows installer | N/A | Installers | Medium |
|Bloom filters| N/A | Network, Core | Optional |
|Two-tier topology | N/A | Network, Core | Optional |
|Pings with bloom filter | N/A | Network, Core | Optional |
|Metadata parsing and search | N/A | Core, Network? | Optional |
|Automatic adjustment of I2P tunnels | N/A | Core | Optional |
|Option to share contact list only with trusted users | N/A | Core, GUI|, Low |
|Downloads queue with priorities | N/A | Core, GUI | Medium |
|Remote queuing of uploads | N/A | Core, Network, GUI | Medium |
|Incomplete file handling | 2 | Core, Portable | Low |
|Chat - enforce # in room names | N/A | GUI | Low |
|Chat - jump from notification window to room | N/A | GUI | Optional |
|Chat - emoji support | 113 | GUI | Low |
|Chat - save chat history | N/A | GUI | Low |
|I2P Status panel for external router | N/A | GUI | Low |
|Option to disable switching of tabs on actions | N/A | GUI | Low |
|Ability to change language after install| 109 | GUI | Medium |
|Diacritics-insensitive filtering | 103 | GUI | Low |
|Redesign the Browse Collections tab | 92 | GUI | Medium |
|Automatically search for more sources | 75 | Core | Medium |
|Notify user collections loaded after files | 69 | GUI | Low |
|Rewrite H2HostCache to not use H2| N/A | Core | Optional|
|I18n - convert all pluralizable strings to a pattern | N/A | GUI | Medium |
