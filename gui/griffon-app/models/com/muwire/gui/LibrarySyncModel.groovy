package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollection
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileModifiedEvent
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.SwingWorker
import java.util.concurrent.atomic.AtomicInteger

@ArtifactProviderFor(GriffonModel)
class LibrarySyncModel {

    @MVCMember @Nonnull
    LibrarySyncView view

    Core core
    List<SharedFile> staleFiles = []
    List<FileCollection> staleCollections


    private LibraryScanner scanner

    void startScan() {
        scanner = new LibraryScanner()
        scanner.execute()
    }

    void cancelScan() {
        if (scanner != null)
            scanner.cancelled = true
    }

    private class LibraryScanner extends SwingWorker<Void, SharedFile> {

        private volatile boolean cancelled
        private final AtomicInteger scanned = new AtomicInteger()

        @Override
        protected Void doInBackground() throws Exception {
            core.getFileManager().getSharedFiles().values().stream().parallel().
                    filter({
                        scanned.incrementAndGet()
                        return !cancelled && it.isStale()
                    }).
                    forEach({publish(it)})
            return null
        }

        @Override
        protected void process(List<SharedFile> chunks) {
            if (cancelled || chunks.isEmpty())
                return
            staleFiles.addAll(chunks)
            view.updateScanProgressBar((int)(scanned.get() * 100 / core.getFileManager().getSharedFiles().size()))
        }

        @Override
        protected void done() {
            if (cancelled)
                return
            Set<FileCollection> affected = new HashSet<>()
            for (SharedFile stale : staleFiles) {
                Set<InfoHash> collectionHashes = core.getCollectionManager().collectionsForFile(stale.getRootInfoHash())
                collectionHashes.each {affected << core.getCollectionManager().getByInfoHash(it)}
            }
            staleCollections = affected.toList()
            view.scanFinished()
        }
    }

    void startReindex() {
        def observer = new ReindexObserver()
        core.eventBus.register(FileHashedEvent.class, observer)
        def event = new FileModifiedEvent(sharedFiles: staleFiles)
        core.eventBus.publish(event)
    }

    private class ReindexObserver {
        private final Set<SharedFile> remaining = new HashSet<>(staleFiles)
        private final int total = remaining.size()
        void onFileHashedEvent(FileHashedEvent event) {
            runInsideUIAsync {
                if (!remaining.remove(event.original))
                    return
                int percentage = (int)((total - remaining.size()) * 100 / total)
                view.updateReindexProgressBar(percentage)
                if (remaining.isEmpty()) {
                    core.eventBus.unregister(FileHashedEvent.class, this)
                    view.reindexComplete()
                }
            }
        }
    }
}
