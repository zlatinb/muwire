package com.muwire.gui

import static com.muwire.gui.Translator.trans

import javax.annotation.Nonnull
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.collections.CollectionFetchStatus
import com.muwire.core.collections.CollectionFetchStatusEvent
import com.muwire.core.collections.CollectionFetchedEvent
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.FileCollectionItem
import com.muwire.core.collections.UICollectionFetchEvent

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class CollectionTabModel {
    
    @MVCMember @Nonnull
    CollectionTabView view
    
    String fileName
    EventBus eventBus
    Persona host
    List<InfoHash> infoHashes
    UUID uuid
    boolean everything
    
    List<FileCollection> collections = new ArrayList<>()
    List<FileCollectionItem> items = new ArrayList<>()
    TreeModel fileTreeModel
    DefaultMutableTreeNode root
    boolean treeVisible = true
    
    @Observable CollectionFetchStatus status
    @Observable String comment = trans("COLLECTION_SELECT")
    @Observable int totalCollections
    @Observable boolean viewCommentButtonEnabled
    @Observable boolean downloadItemButtonEnabled
    @Observable boolean downloadCollectionButtonEnabled
    @Observable boolean downloadSequentiallyCollection
    @Observable boolean downloadSequentiallyItem
    
    void mvcGroupInit(Map<String,String> args) {
        root = new DefaultMutableTreeNode()
        fileTreeModel = new DefaultTreeModel(root)
        eventBus.with {
            register(CollectionFetchStatusEvent.class, this) 
            register(CollectionFetchedEvent.class, this)
            publish(new UICollectionFetchEvent(uuid : uuid, host : host, infoHashes : infoHashes, everything : everything))
        }
    }
    
    void mvcGroupDestroy() {
        eventBus.unregister(CollectionFetchedEvent.class, this)
    }
    
    void onCollectionFetchStatusEvent(CollectionFetchStatusEvent e) {
        if (uuid != e.uuid)
            return
        runInsideUIAsync {
            status = e.status
            if (status == CollectionFetchStatus.FETCHING)
                totalCollections = e.count
        }
    }
    
    void onCollectionFetchedEvent(CollectionFetchedEvent e) {
        if (uuid != e.uuid)
            return
        runInsideUIAsync {
            collections.add(e.collection)
            view.collectionsTable.model.fireTableDataChanged() // maybe preserving selection?
        }
    }
}