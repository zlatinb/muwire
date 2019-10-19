package com.muwire.cli

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.download.DownloadStartedEvent
import com.muwire.core.download.Downloader
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.UIResultEvent

import net.i2p.data.Base64

class CliDownloader {

    private static final List<Downloader> downloaders = Collections.synchronizedList(new ArrayList<>())
    private static final Map<UUID,ResultsHolder> resultsListeners = new ConcurrentHashMap<>()

    public static void main(String []args) {
        def home = System.getProperty("user.home") + File.separator + ".MuWire"
        home = new File(home)
        if (!home.exists())
            home.mkdirs()

        def propsFile = new File(home,"MuWire.properties")
        if (!propsFile.exists()) {
            println "create props file ${propsFile.getAbsoluteFile()} before launching MuWire"
            System.exit(1)
        }

        def props = new Properties()
        propsFile.withInputStream { props.load(it) }
        props = new MuWireSettings(props)

        def filesList
        int connections
        int resultWait
        if (args.length != 3) {
            println "Enter a file containing list of hashes of files to download, " +
                "how many connections you want before searching" +
                "and how long to wait for results to arrive"
                System.exit(1)
        } else {
            filesList = args[0]
            connections = Integer.parseInt(args[1])
            resultWait = Integer.parseInt(args[2])
        }

        Core core
        try {
            core = new Core(props, home, "0.5.1")
        } catch (Exception bad) {
            bad.printStackTrace(System.out)
            println "Failed to initialize core, exiting"
            System.exit(1)
        }


        def latch = new CountDownLatch(connections)
        def connectionListener = new ConnectionWaiter(latch : latch)
        core.eventBus.register(ConnectionEvent.class, connectionListener)

        core.startServices()
        println "starting to wait until there are $connections connections"
        latch.await()

        println "connected, searching for files"

        def file = new File(filesList)
        file.eachLine {
            String[] split = it.split(",")
            UUID uuid = UUID.randomUUID()
            core.eventBus.register(UIResultEvent.class, new ResultsListener(fileName : split[1]))
            def hash = Base64.decode(split[0])
            def searchEvent = new SearchEvent(searchHash : hash, uuid : uuid)
            core.eventBus.publish(new QueryEvent(searchEvent : searchEvent, firstHop:true,
                replyTo: core.me.destination, receivedOn : core.me.destination, originator: core.me))
        }

        println "waiting for results to arrive"
        Thread.sleep(resultWait * 1000)

        core.eventBus.register(DownloadStartedEvent.class, new DownloadListener())
        resultsListeners.each { uuid, resultsListener ->
            println "starting download of $resultsListener.fileName from ${resultsListener.getResults().size()} hosts"
            File target = new File(resultsListener.fileName)

            core.eventBus.publish(new UIDownloadEvent(target : target, result : resultsListener.getResults()))
        }

        Thread.sleep(1000)

        Timer timer = new Timer("stats-printer")
        timer.schedule({
            println "==== STATUS UPDATE ==="
            downloaders.each {
                int donePieces = it.donePieces()
                int totalPieces = it.nPieces
                int sources = it.activeWorkers.size()
                def root = Base64.encode(it.infoHash.getRoot())
                def state = it.getCurrentState()
                println "file $it.file hash: $root progress: $donePieces/$totalPieces sources: $sources status: $state}"
                it.resume()
            }
            println "==== END ==="
        } as TimerTask, 60000, 60000)

        println "waiting for downloads to finish"
        while(true) {
            boolean allFinished = true
            for (Downloader d : downloaders) {
                allFinished &= d.getCurrentState() == Downloader.DownloadState.FINISHED
            }
            if (allFinished)
                break
            Thread.sleep(1000)
        }

        println "all downloads finished"
    }

    static class ResultsHolder {
        final List<UIResultEvent> results = Collections.synchronizedList(new ArrayList<>())
        String fileName
        void add(UIResultEvent e) {
            results.add(e)
        }
        List getResults() {
            results
        }
    }

    static class ResultsListener {
        UUID uuid
        String fileName
        public onUIResultEvent(UIResultEvent e) {
            println "got a result for $fileName from ${e.sender.getHumanReadableName()}"
            ResultsHolder listener = resultsListeners.get(e.uuid)
            if (listener == null) {
                listener = new ResultsHolder(fileName : fileName)
                resultsListeners.put(e.uuid, listener)
            }
            listener.add(e)
        }
    }

    static class ConnectionWaiter {
        CountDownLatch latch
        public void onConnectionEvent(ConnectionEvent e) {
            if (e.status == ConnectionAttemptStatus.SUCCESSFUL)
                latch.countDown()
        }
    }


    static class DownloadListener {
        public void onDownloadStartedEvent(DownloadStartedEvent e) {
            downloaders.add(e.downloader)
        }
    }
}
