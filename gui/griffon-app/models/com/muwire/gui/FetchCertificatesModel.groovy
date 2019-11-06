package com.muwire.gui

import com.muwire.core.filecert.CertificateFetchStatus
import com.muwire.core.search.UIResultEvent

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class FetchCertificatesModel {
    UIResultEvent result
    
    @Observable CertificateFetchStatus status
    @Observable int totalCertificates
    @Observable int certificateCount
    
    @Observable boolean importActionEnabled
    @Observable boolean showCommentActionEnabled
    
    def certificates = []
}