package com.muwire.cli

import java.util.concurrent.CountDownLatch

import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.core.UILoadedEvent
import com.muwire.core.connection.ConnectionAttemptStatus
import com.muwire.core.connection.ConnectionEvent
import com.muwire.core.connection.DisconnectionEvent
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.upload.UploadEvent
import com.muwire.core.upload.UploadFinishedEvent

class Cli {

    public static void main(String[] args) {
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

        Core core
        try {
            core = new Core(props, home, "0.4.7")
        } catch (Exception bad) {
            bad.printStackTrace(System.out)
            println "Failed to initialize core, exiting"
            System.exit(1)
        }




        def filesList
        if (args.length == 0) {
            println "Enter a file containing list of files to share"
            def reader = new BufferedReader(new InputStreamReader(System.in))
            filesList = reader.readLine()
        } else
            filesList = args[0]

        Thread.sleep(1000)
        println "loading shared files from $filesList"

        // listener for shared files
        def sharedListener = new SharedListener()
        core.eventBus.register(FileHashedEvent.class, sharedListener)
        core.eventBus.register(FileLoadedEvent.class, sharedListener)

        // for connections
        def connectionsListener = new ConnectionListener()
        core.eventBus.register(ConnectionEvent.class, connectionsListener)
        core.eventBus.register(DisconnectionEvent.class, connectionsListener)

        // for uploads
        def uploadsListener = new UploadsListener()
        core.eventBus.register(UploadEvent.class, uploadsListener)
        core.eventBus.register(UploadFinishedEvent.class, uploadsListener)

        Timer timer = new Timer("status-printer", true)
        timer.schedule({
            println String.valueOf(new Date()) + " Connections $connectionsListener.connections Uploads $uploadsListener.uploads Shared $sharedListener.shared"
        } as TimerTask, 60000, 60000)

        def latch = new CountDownLatch(1)
        def fileLoader = new Object() {
            public void onAllFilesLoadedEvent(AllFilesLoadedEvent e) {
                latch.countDown()
            }
        }
        core.eventBus.register(AllFilesLoadedEvent.class, fileLoader)
        core.startServices()

        core.eventBus.publish(new UILoadedEvent())
        println "waiting for files to load"
        latch.await()
        // now we begin
        println "MuWire is ready"

        filesList = new File(filesList)
        filesList.withReader {
            def toShare = it.readLine()
            core.eventBus.publish(new FileSharedEvent(file : new File(toShare)))
        }
        Runtime.getRuntime().addShutdownHook({
            println "shutting down.."
            core.shutdown()
            println "shutdown."
        })
        Thread.sleep(Integer.MAX_VALUE)
    }

    static class ConnectionListener {
        volatile int connections
        public void onConnectionEvent(ConnectionEvent e) {
            if (e.status == ConnectionAttemptStatus.SUCCESSFUL)
                connections++
        }
        public void onDisconnectionEvent(DisconnectionEvent e) {
            connections--
        }
    }

    static class UploadsListener {
        volatile int uploads
        public void onUploadEvent(UploadEvent e) {
            uploads++
            println String.valueOf(new Date()) + " Starting upload of ${e.uploader.file.getName()} to ${e.uploader.request.downloader.getHumanReadableName()}"
        }
        public void onUploadFinishedEvent(UploadFinishedEvent e) {
            uploads--
            println String.valueOf(new Date()) + " Finished upload of ${e.uploader.file.getName()} to ${e.uploader.request.downloader.getHumanReadableName()}"
        }
    }

    static class SharedListener {
        volatile int shared
        void onFileHashedEvent(FileHashedEvent e) {
            if (e.error != null)
                println "ERROR $e.error"
            else {
                println "Shared file : $e.sharedFile.file"
                shared++
            }
        }
        void onFileLoadedEvent(FileLoadedEvent e) {
            shared++
        }
    }
}
