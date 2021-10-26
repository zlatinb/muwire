package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.search.UIResultEvent
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class ResultDetailsModel {
    
    @MVCMember @Nonnull
    ResultDetailsView view

    Core core
    String fileName
    InfoHash infoHash
    List<UIResultEvent> results

    String key
    List<SharedFile> localFiles
    
    void mvcGroupInit(Map<String,String> args) {
        key = fileName + Base64.encode(infoHash.getRoot())
        SharedFile[] locals = core.fileManager.getSharedFiles(infoHash.getRoot())
        if (locals == null)
            localFiles = Collections.emptyList()
        else
            localFiles = Arrays.asList(locals)
    }
}
