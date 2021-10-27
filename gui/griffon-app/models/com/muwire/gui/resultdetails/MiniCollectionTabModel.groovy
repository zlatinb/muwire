package com.muwire.gui.resultdetails

import com.muwire.core.Core
import com.muwire.core.collections.CollectionFetchStatus
import com.muwire.core.collections.CollectionFetchStatusEvent
import com.muwire.core.collections.CollectionFetchedEvent
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.UICollectionFetchEvent
import com.muwire.core.search.UIResultEvent
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import griffon.transform.Observable

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class MiniCollectionTabModel {
    
    @MVCMember @Nonnull
    MiniCollectionTabView view
    
    Core core
    UIResultEvent resultEvent
    
    @Observable CollectionFetchStatus status
    @Observable int count
    @Observable int fetched
    @Observable boolean viewCommentActionEnabled
    @Observable boolean viewCollectionActionEnabled
    
    private volatile UUID uuid
    
    final List<FileCollection> collections = new ArrayList<>()
    
    void register() {
        uuid = UUID.randomUUID()
        def event = new UICollectionFetchEvent(uuid: uuid, host: resultEvent.sender,
                infoHashes: resultEvent.collections)
        core.eventBus.with {
            register(CollectionFetchStatusEvent.class, this)
            register(CollectionFetchedEvent.class, this)
            publish event
        }
    }
    
    void mvcGroupDestroy() {
        if (uuid != null) {
            core.eventBus.with {
                unregister(CollectionFetchStatusEvent.class, this)
                unregister(CollectionFetchedEvent.class, this)
            }
        }
    }

    void onCollectionFetchStatusEvent(CollectionFetchStatusEvent event) {
        if (event.uuid != uuid)
            return
        runInsideUIAsync {
            status = event.status
            if (status == CollectionFetchStatus.FETCHING)
                count = event.count
        }
    }

    void onCollectionFetchedEvent(CollectionFetchedEvent event) {
        if (event.uuid != uuid)
            return
        runInsideUIAsync {
            collections << event.collection
            fetched++
            view.refresh()
        }
    }
}
