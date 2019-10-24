package com.muwire.clilanterna

import com.muwire.core.download.Downloader

class DownloaderWrapper {
    final Downloader downloader
    DownloaderWrapper(Downloader downloader) {
        this.downloader = downloader
    }
    
    @Override
    public String toString() {
        downloader.file.getName()
    }
}
