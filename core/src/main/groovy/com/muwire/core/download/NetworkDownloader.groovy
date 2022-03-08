package com.muwire.core.download

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.chat.ChatServer
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.util.BandwidthCounter
import com.muwire.core.util.DataUtil
import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level

@Log
class NetworkDownloader extends Downloader {
    private enum WorkerState { NEW, CONNECTING, HASHLIST, DOWNLOADING, FINISHED}

    private final ChatServer chatServer
    private final Persona me
    private final Pieces pieces
    
    private final I2PConnector connector
    private final Set<Destination> destinations
    
    private final File incompletes
    private final File piecesFile
    private final File incompleteFile

    private final Map<Destination, DownloadWorker> activeWorkers = new ConcurrentHashMap<>()
    final Set<Destination> successfulDestinations = new ConcurrentHashSet<>()
    /** LOCKING: itself */
    private final Map<Destination, Integer> failingDestinations = new HashMap<>()
    private final int maxFailures

    private final int queueSize

    private final AtomicBoolean eventFired = new AtomicBoolean()
    private final AtomicBoolean hopelessEventFired = new AtomicBoolean()
    private boolean piecesFileClosed

    private final AtomicLong dataSinceLastRead = new AtomicLong()
    private volatile BandwidthCounter bwCounter = new BandwidthCounter(0)
    
    
    NetworkDownloader(EventBus eventBus, DownloadManager downloadManager, ChatServer chatServer,
                      Persona me, File file, File toShare, long length, InfoHash infoHash, InfoHash collectionInfoHash,
                      int pieceSizePow2, I2PConnector connector, Set<Destination> destinations,
                      File incompletes, Pieces pieces, int maxFailures) {
        super(eventBus, downloadManager, file, toShare, length, infoHash, collectionInfoHash, pieceSizePow2)
        this.connector = connector
        this.chatServer = chatServer
        this.me = me
        this.destinations = destinations
        this.incompletes = incompletes
        
        
        String ih64 = Base64.encode(infoHash.getRoot())
        this.piecesFile = new File(incompletes, file.getName()+"${ih64}.pieces")
        this.incompleteFile = new File(incompletes, file.getName()+"${ih64}.part")
        this.pieces = pieces
        this.maxFailures = maxFailures

        // base queue size on download piece size
        int queueSize = 1
        if (pieceSizePow2 < 19)
            queueSize++
        if (pieceSizePow2 < 18)
            queueSize++
        this.queueSize = Math.min(getNPieces() - 1, queueSize)
    }

    void download() {
        readPieces()
        destinations.each {
            if (it != me.destination && !isHopeless(it)) {
                def worker = new DownloadWorker(it)
                activeWorkers.put(it, worker)
                executorService.submit(worker)
            }
        }
    }

    private void readPieces() {
        if (!piecesFile.exists())
            return
        piecesFile.eachLine {
            String [] split = it.split(",")
            int piece = Integer.parseInt(split[0])
            if (split.length == 1)
                pieces.markDownloaded(piece)
            else {
                int position = Integer.parseInt(split[1])
                pieces.markPartial(piece, position)
            }
        }
    }

    private void writePieces() {
        synchronized(piecesFile) {
            if (piecesFileClosed)
                return
            piecesFile.withPrintWriter { writer ->
                pieces.write(writer)
            }
        }
    }
    
    private void closePiecesFile() {
        synchronized (piecesFile) {
            if (piecesFileClosed)
                return
            piecesFileClosed = true
            piecesFile.delete()
        }
    }

    long donePieces() {
        pieces.donePieces()
    }

    int speed() {
        int currSpeed = 0
        if (getCurrentState() != DownloadState.DOWNLOADING)
            return currSpeed

        // this is not very accurate since each slot may hold more than a second
        if (bwCounter.getMemory() != downloadManager.muSettings.speedSmoothSeconds)
            bwCounter = new BandwidthCounter(downloadManager.muSettings.speedSmoothSeconds)

        bwCounter.read((int)dataSinceLastRead.getAndSet(0))
        bwCounter.average()
    }
    
    protected DownloadState getSpecificState() {
        boolean allFinished = true
        boolean allRejected = true
        activeWorkers.values().each {
            allFinished &= it.currentState == WorkerState.FINISHED
            allRejected &= it.rejected
        }
        if (allFinished) {
            if (pieces.isComplete())
                return DownloadState.FINISHED
            if (allRejected)
                return DownloadState.REJECTED
            if (!hasLiveSources())
                return DownloadState.HOPELESS
            return DownloadState.FAILED
        }

        // if at least one is downloading...
        boolean oneDownloading = false
        activeWorkers.values().each {
            if (it.currentState == WorkerState.DOWNLOADING) {
                oneDownloading = true
                return
            }
        }

        if (oneDownloading)
            return DownloadState.DOWNLOADING

        // at least one is requesting hashlist
        boolean oneHashlist = false
        activeWorkers.values().each {
            if (it.currentState == WorkerState.HASHLIST) {
                oneHashlist = true
                return
            }
        }
        if (oneHashlist)
            return DownloadState.HASHLIST

        return DownloadState.CONNECTING
    }
    
    
    protected void doCancel() {
        stop()
        synchronized(piecesFile) {
            piecesFileClosed = true
            piecesFile.delete()
        }
        incompleteFile.delete()
        pieces.clearAll()
    }
    
    protected void doPause() {
        stop()
    }

    protected void stop() {
        activeWorkers.values().each {
            it.cancel()
        }
    }

    int activeWorkers() {
        int active = 0
        activeWorkers.values().each {
            if (it.currentState != WorkerState.FINISHED)
                active++
        }
        active
    }

    int getTotalWorkers() {
        return activeWorkers.size();
    }

    int countHopelessSources() {
        synchronized(failingDestinations) {
            return destinations.count { isHopeless(it)}
        }
    }
    
    boolean isPausable() {
        true
    }

    private boolean hasLiveSources() {
        destinations.size() > countHopelessSources()
    }
    
    protected void doResume() {
        executorService.submit {
            readPieces()
            destinations.stream().filter({ !isHopeless(it) }).forEach { destination ->
                log.fine("resuming source ${destination.toBase32()}")
                def worker = activeWorkers.get(destination)
                if (worker != null) {
                    if (worker.currentState == WorkerState.FINISHED) {
                        def newWorker = new DownloadWorker(destination)
                        activeWorkers.put(destination, newWorker)
                        executorService.submit(newWorker)
                    }
                } else {
                    worker = new DownloadWorker(destination)
                    activeWorkers.put(destination, worker)
                    executorService.submit(worker)
                }
            }
        } as Runnable
    }

    void addSource(Destination d) {
        if (activeWorkers.containsKey(d) || isHopeless(d))
            return
        destinations.add(d)
        DownloadWorker newWorker = new DownloadWorker(d)
        activeWorkers.put(d, newWorker)
        executorService.submit(newWorker)
    }

    boolean isSequential() {
        pieces.ratio == 0f
    }

    File generatePreview() {
        int lastCompletePiece = pieces.firstIncomplete() - 1
        if (lastCompletePiece == -1)
            return null
        if (lastCompletePiece < -1)
            return file
        long previewableLength = (lastCompletePiece + 1) * ((long)pieceSize)

        // generate name
        long now = System.currentTimeMillis()
        File previewFile
        File parentFile = file.getParentFile()
        int lastDot = file.getName().lastIndexOf('.')
        if (lastDot < 0)
            previewFile = new File(parentFile, file.getName() + "." + String.valueOf(now) + ".mwpreview")
        else {
            String name = file.getName().substring(0, lastDot)
            String extension = file.getName().substring(lastDot + 1)
            String previewName = name + "." + String.valueOf(now) + ".mwpreview."+extension
            previewFile = new File(parentFile, previewName)
        }

        // copy
        InputStream is = null
        OutputStream os = null
        try {
            is = new BufferedInputStream(new FileInputStream(incompleteFile))
            os = new BufferedOutputStream(new FileOutputStream(previewFile))
            byte [] tmp = new byte[0x1 << 13]
            long totalCopied = 0
            while(totalCopied < previewableLength) {
                int read = is.read(tmp, 0, (int)Math.min(tmp.length, previewableLength - totalCopied))
                if (read < 0)
                    throw new IOException("EOF?")
                os.write(tmp, 0, read)
                totalCopied += read
            }
            return previewFile
        } catch (IOException bad) {
            log.log(Level.WARNING,"Preview failed",bad)
            return null
        } finally {
            try {is?.close() } catch (IOException ignore) {}
            try {os?.close() } catch (IOException ignore) {}
        }
    }

    private boolean isHopeless(Destination d) {
        if (maxFailures < 0)
            return false
        synchronized(failingDestinations) {
            return !successfulDestinations.contains(d) &&
                    failingDestinations.containsKey(d) &&
                    failingDestinations[d] >= maxFailures
        }
    }

    private void markFailed(Destination d) {
        log.fine("marking failed ${d.toBase32()}")
        synchronized(failingDestinations) {
            Integer count = failingDestinations.get(d)
            if (count == null) {
                failingDestinations.put(d, 1)
            } else {
                failingDestinations.put(d, count + 1)
            }
        }
    }

    class DownloadWorker implements Runnable {
        private final Destination destination
        private volatile WorkerState currentState = WorkerState.NEW
        private volatile Thread downloadThread
        private volatile boolean cancelled, rejected
        private final LinkedList<DownloadSession> sessionQueue = new LinkedList<>()
        private final Set<Integer> available = new HashSet<>()

        DownloadWorker(Destination destination) {
            this.destination = destination
        }

        public void run() {
            if (cancelled) {
                currentState = WorkerState.FINISHED
                return
            }
            
            downloadThread = Thread.currentThread()
            currentState = WorkerState.CONNECTING
            Endpoint endpoint = null
            try {
                endpoint = connector.connect(destination)
                while(infoHash.hashList == null) {
                    currentState = WorkerState.HASHLIST
                    HashListSession session = new HashListSession(me.toBase64(), infoHash, endpoint)
                    InfoHash received = session.request()
                    infoHash = received
                    downloadManager.persistDownloaders()
                }
                currentState = WorkerState.DOWNLOADING

                boolean browse = downloadManager.muSettings.browseFiles
                boolean feed = downloadManager.muSettings.fileFeed && downloadManager.muSettings.advertiseFeed
                boolean chat = chatServer.isRunning() && downloadManager.muSettings.advertiseChat
                boolean message = downloadManager.muSettings.allowMessages

                Set<Integer> queuedPieces = new HashSet<>()
                boolean requestPerformed
                boolean supportsHead = false
                while(!pieces.isComplete()) {
                    if (sessionQueue.isEmpty()) {
                        boolean sentAnyRequests = false
                        queueSize.times {
                            available.removeAll(queuedPieces)
                            def currentSession = new DownloadSession(eventBus, me.toBase64(), pieces, infoHash,
                                    endpoint, incompleteFile, pieceSize, length, available, dataSinceLastRead,
                                    browse, feed, chat, message)
                            if (currentSession.sendRequest()) {
                                queuedPieces.add(currentSession.piece)
                                sessionQueue.addLast(currentSession)
                                sentAnyRequests = true
                            }
                        }
                        if (!sentAnyRequests && queueSize > 0)
                            break;
                        endpoint.getOutputStream().flush()
                    }
                    available.removeAll(queuedPieces)
                    def nextSession = new DownloadSession(eventBus, me.toBase64(), pieces, infoHash,
                            endpoint, incompleteFile, pieceSize, length, available, dataSinceLastRead,
                            browse, feed, chat, message)
                    if (nextSession.sendRequest()) {
                        sessionQueue.addLast(nextSession)
                        queuedPieces.add(nextSession.piece)
                    }

                    def currentSession = sessionQueue.removeFirst()
                    requestPerformed = currentSession.consumeResponse()
                    queuedPieces.remove(currentSession.piece)
                    if (!requestPerformed)
                        break
                    successfulDestinations.add(endpoint.destination)
                    writePieces()
                    supportsHead = currentSession.supportsHead
                }

                // issue a HEAD request when everything is done
                if (supportsHead) {
                    HeadSession headSession = new HeadSession(eventBus, me.toBase64(), pieces, infoHash,
                            endpoint, browse, feed, chat, message)
                    headSession.performRequest()
                }
            } catch (DownloadRejectedException rejected) {
                this.rejected = true  
            } catch (Exception bad) {
                if (!cancelled) {
                    log.log(Level.WARNING, "Exception while downloading", DataUtil.findRoot(bad))
                    markFailed(destination)
                    if (!hasLiveSources() && hopelessEventFired.compareAndSet(false, true)) {
                        log.info("Download hopeless, removing incomplete file")
                        incompleteFile.delete()
                        closePiecesFile()
                        eventBus.publish(new DownloadHopelessEvent(downloader: NetworkDownloader.this))
                    }
                }
            } finally {
                currentState = WorkerState.FINISHED
                try {
                    if (!cancelled) {
                        writePieces()
                        if (pieces.isComplete() && eventFired.compareAndSet(false, true)) {
                            closePiecesFile()
                            activeWorkers.values().each {
                                if (it.destination != destination)
                                    it.cancel()
                            }

                            file.getParentFile().mkdirs()
                            try {
                                Files.move(incompleteFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)
                            } catch (AtomicMoveNotSupportedException e) {
                                Files.copy(incompleteFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                                incompleteFile.delete()
                            }
                            fireEvent(getSuccessfulDestinations())
                        }
                    }
                } finally {
                    endpoint?.close()
                }
                    
            }
        }

        void cancel() {
            cancelled = true
            downloadThread?.interrupt()
        }
    }
}
