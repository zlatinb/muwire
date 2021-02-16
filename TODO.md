# TODO List

### Network

##### Bloom Filters

This reduces query traffic by not sending last hop queries to peers that definitely do not have the file

##### Two-tier Topology

This helps with scalability

### Core

* Metadata parsing and search
* Automatic adjustment of number of I2P tunnels
* Persist trust immediately
* Ability to share trust list only with trusted users
* Confidential files visible only to certain users
* Download queue with priorities 
* Use tracker pings - either embedded logic or external mwtrackerd to add more sources to downloads
* PORTABLE - figure out how to handle incomplete files

### Chat
* break up lines on CR/LF, send multiple messages
* enforce # in room names or ignore it
* auto-create/join channel on server start
* jump from notification window to room with message

### Swing GUI
* I2P Status panel - display message when connected to external router
* Search box - left identation
* Ability to disable switching of tabs on actions
* Right-click and paste in various text input fields
* Open containing folder from downloads table for finished downloads
* Subscribe to feed by full id
* Check for duplicate feed subscriptions

### Web UI/Plugin
* HTML 5 media players
* Remove versions from jar names
* Security: POST nonces, CSP header - is this done?s
* Check permissions, display better errors when sharing local folders
* collections, messages - requires drag-and-drop from library

### mwtrackerd
* `save` and `load` JSON-RPC commands that save and load swarm state respectively
* load-test with many many hashes (1M?)
* evaluate other usage scenarios besides website backend 
