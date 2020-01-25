package com.muwire.core.files


import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.UILoadedEvent
import groovy.json.JsonSlurper
import groovy.util.logging.Log

@Log
class PersisterService extends BasePersisterService {

    final File location
    final EventBus listener
    final int interval
    final Timer timer
    final FileManager fileManager

    PersisterService(File location, EventBus listener, int interval, FileManager fileManager) {
        this.location = location
        this.listener = listener
        this.interval = interval
        this.fileManager = fileManager
        timer = new Timer("file persister timer", true)
    }

    void stop() {
        timer.cancel()
    }

    void onUILoadedEvent(UILoadedEvent e) {
        timer.schedule({load()} as TimerTask, 1)
    }

    void load() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY)

        if (location.exists() && location.isFile()) {
            int loaded = 0
            def slurper = new JsonSlurper()
            try {
                location.eachLine {
                    if (it.trim().length() > 0) {
                        def parsed = slurper.parseText it
                        def event = fromJson parsed
                        if (event != null) {
                            log.fine("loaded file $event.loadedFile.file")
                            event.source = "PersisterService"
                            listener.publish event
                            loaded++
                            if (loaded % 10 == 0)
                                Thread.sleep(20)
                        }
                    }
                }
                // Backup the old hashes
                location.renameTo(
                        new File(location.absolutePath + ".bak")
                )
                listener.publish(new PersisterDoneEvent())
            } catch (IllegalArgumentException e) {
                log.log(Level.WARNING, "couldn't load files",e)
            }
        } else {
            listener.publish(new PersisterDoneEvent())
        }
        loaded = true
    }

}
