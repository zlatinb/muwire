package com.muwire.core.files.directories

import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class WatchedDirectory {
    final File directory, canonical
    final String encodedName
    final Set<File> aliases = new HashSet<>()
    boolean autoWatch
    int syncInterval
    long lastSync
    
    WatchedDirectory(File directory) {
        this.directory = directory
        aliases.add(directory)
        this.canonical = directory.getCanonicalFile()
        this.encodedName = encodeFileName(directory)
    }
    
    def toJson() {
        def rv = [:]
        rv.directory = encodedName
        rv.autoWatch = autoWatch
        rv.syncInterval = syncInterval
        rv.lastSync = lastSync
        rv.aliases = aliases.collect {encodeFileName(it)}
        rv
    }
    
    static WatchedDirectory fromJson(def json) {
        File dir = decodeFileName(json.directory)
        def rv = new WatchedDirectory(dir)
        rv.autoWatch = json.autoWatch
        rv.syncInterval = json.syncInterval
        rv.lastSync = json.lastSync
        if (json.aliases != null)
            rv.aliases.addAll json.aliases.collect{ decodeFileName(it)}
        rv
    }
    
    private static String encodeFileName(File file) {
        Base64.encode(DataUtil.encodei18nString(file.getAbsolutePath()))
    }
    
    private static File decodeFileName(String encoded) {
        new File(DataUtil.readi18nString(Base64.decode(encoded)))
    }
}
