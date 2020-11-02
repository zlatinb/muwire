package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

import javax.annotation.Nonnull

import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.UICollectionDeletedEvent
import com.muwire.core.util.DataUtil

@ArtifactProviderFor(GriffonController)
class CollectionsToolController {
    @MVCMember @Nonnull
    CollectionsToolView view
    @MVCMember @Nonnull
    CollectionsToolModel model
    
    @ControllerAction
    void clearHits() {
        model.collection.hits.clear()
        model.hits.clear()
        view.hitsTable.model.fireTableDataChanged()
    }
}