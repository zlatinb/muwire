package com.muwire.core.files

import com.muwire.core.DownloadedFile
import com.muwire.core.Event
import com.muwire.core.download.Downloader

import net.i2p.data.Destination

class FileDownloadedEvent extends Event {
    Downloader downloader
	DownloadedFile downloadedFile
}
