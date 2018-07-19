package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

import net.i2p.data.Destination

class FileDownloadedEvent extends Event {

	SharedFile downloadedFile
	List<Destination> sources
}
