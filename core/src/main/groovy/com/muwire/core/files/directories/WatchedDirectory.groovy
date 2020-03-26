package com.muwire.core.files.directories

import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class WatchedDirectory {
    final File directory
    final String encodedName
    boolean autoWatch
    int syncInterval
    long lastSync
    
    WatchedDirectory(File directory) {
        this.directory = directory.getCanonicalFile()
        this.encodedName = Base64.encode(DataUtil.encodei18nString(directory.getAbsolutePath()))
    }
    
    def toJson() {
        def rv = [:]
        rv.directory = encodedName
        rv.autoWatch = autoWatch
        rv.syncInterval = syncInterval
        rv.lastSync = lastSync
        rv
    }
    
    static WatchedDirectory fromJson(def json) {
        String dirName = DataUtil.readi18nString(Base64.decode(json.directory))
        File dir = new File(dirName)
        def rv = new WatchedDirectory(dir)
        rv.autoWatch = json.autoWatch
        rv.syncInterval = json.syncInterval
        rv.lastSync = json.lastSync
        rv
    }
}
