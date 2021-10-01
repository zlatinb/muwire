package com.muwire.core.download

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import net.i2p.data.Destination

import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CopyingDownloader extends Downloader{
    
    final File source
    private volatile Thread workerThread
    private volatile boolean done
    
    CopyingDownloader(EventBus eventBus, DownloadManager downloadManager, File file, long length,
                      InfoHash infoHash, InfoHash collectionInfoHash, int pieceSizePow2, File source) {
        super(eventBus, downloadManager, file, length, infoHash, collectionInfoHash, pieceSizePow2 )
        this.source = source
    }
    @Override
    void download() {
        def worker = new CopyWorker()
        executorService.submit(worker)
    }

    @Override
    long donePieces() {
        return getNPieces()
    }

    @Override
    int speed() {
        return 0
    }

    @Override
    protected DownloadState getSpecificState() {
        if (!done)
            return DownloadState.COPYING
        DownloadState.FINISHED
    }

    @Override
    protected void doCancel() {
        stop()
    }
    
    boolean isPausable() {
        false
    }

    @Override
    protected void doPause() {
        throw new IllegalStateException()
    }

    @Override
    protected void stop() {
        workerThread?.interrupt()
    }

    @Override
    int activeWorkers() {
        if (done)
            return 0
        return 1
    }

    @Override
    int getTotalWorkers() {
        return 1
    }

    @Override
    int countHopelessSources() {
        return 0
    }

    @Override
    protected void doResume() {
    }

    @Override
    void addSource(Destination d) {
    }

    @Override
    boolean isSequential() {
        true
    }

    @Override
    File generatePreview() {
        return source
    }
    
    private class CopyWorker implements Runnable {
        public void run() {
            workerThread = Thread.currentThread()
            Files.copy(source.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            done = true
            fireEvent(Collections.emptySet())
        }
    }
}
