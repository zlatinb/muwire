package com.muwire.core.files.directories

import com.muwire.core.Event

/**
 * Emitted when converting an old watched directory entry to the
 * new format.
 */
class WatchedDirectoryConvertedEvent extends Event {
    String directoryPath
}
