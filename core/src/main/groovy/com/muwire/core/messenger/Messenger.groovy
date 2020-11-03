package com.muwire.core.messenger

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

import com.muwire.core.EventBus

import groovy.util.logging.Log

@Log
class Messenger {

    private final EventBus eventBus
    private final File inbox, outbox, sent
    
    private final Set<MWMessage> inboxMessages = new LinkedHashSet<>()
    private final Set<MWMessage> outbodMessages = new LinkedHashSet<>()
    private final Set<MWMessage> sentMessages = new LinkedHashSet<>()
    
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor({ Runnable r ->
        new Thread(r, "messenger-io")
    } as ThreadFactory)
    
    public Messenger(EventBus eventBus, File home) {
        this.eventBus = eventBus
        
        File messages = new File(home, "messages")
        inbox = new File(messages, "inbox")
        outbox = new File(messages, "outbox")
        sent = new File(messages, "sent")
        
        inbox.mkdirs()
        outbox.mkdirs()
        sent.mkdirs()
    }
    
    public void start() {
        diskIO.execute({load()} as Runnable)
    }
    
    public void stop() {
        diskIO.shutdown()
    }
    
    private void load() {
        log.info("loading messages")
        loadFolder(inbox, inboxMessages, "inbox")
        loadFolder(outbox, inboxMessages, "outbox")
        loadFolder(sent, inboxMessages, "sent")
    }
    
    private void loadFolder(File file, Set<MWMessage> dest, String folderName) {
        Files.walk(file.toPath())
            .filter({it.getFileName().toString().endsWith(".mwmessage")})
            .forEach { Path path ->
                File f = path.toFile()
                MWMessage message
                f.withInputStream { 
                    message = new MWMessage(it)
                }
                addMessage(message, dest)
                eventBus.publish(new MessageLoadedEvent(message : message, folder : folderName))
        }
    }
    
    private synchronized void addMessage(MWMessage message, Set<MWMessage> dest) {
        dest.add(message)
    }
}