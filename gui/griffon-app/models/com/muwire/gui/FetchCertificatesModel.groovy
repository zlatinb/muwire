package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.filecert.CertificateFetchStatus
import com.muwire.core.filefeeds.FeedItem
import com.muwire.core.search.UIResultEvent

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class FetchCertificatesModel {
    Persona host
    InfoHash infoHash
    String name
    
    @Observable CertificateFetchStatus status
    @Observable int totalCertificates
    @Observable int certificateCount
    
    @Observable boolean importActionEnabled
    @Observable boolean showCommentActionEnabled
    
    def certificates = []
}