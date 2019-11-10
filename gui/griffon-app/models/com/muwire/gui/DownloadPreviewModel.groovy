package com.muwire.gui

import com.muwire.core.download.Downloader

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class DownloadPreviewModel {
    Downloader downloader
}