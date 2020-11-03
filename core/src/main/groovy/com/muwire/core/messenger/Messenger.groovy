package com.muwire.core.messenger

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.UILoadedEvent
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector

import groovy.util.logging.Log
import net.i2p.data.Base64

@Log
class Messenger {

    
    private static final int MAX_IN_PROCESS = 4
    
    private final EventBus eventBus
    private final File inbox, outbox, sent
    private final I2PConnector connector
    private final MuWireSettings settings
    
    private final Set<MWMessage> inboxMessages = new LinkedHashSet<>()
    private final Set<MWMessage> outboxMessages = new LinkedHashSet<>()
    private final Set<MWMessage> sentMessages = new LinkedHashSet<>()
    
    private final Set<MWMessage> inProcess = new HashSet<>()
    
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor({ Runnable r ->
        new Thread(r, "messenger-disk-io")
    } as ThreadFactory)
    
    private final ExecutorService netIO = Executors.newCachedThreadPool({ Runnable r ->
        new Thread(r, "messenger-net-io")
    } as ThreadFactory)
    
    private final Timer timer = new Timer()
    
    
    public Messenger(EventBus eventBus, File home, I2PConnector connector, MuWireSettings settings) {
        this.eventBus = eventBus
        this.connector = connector
        this.settings = settings
        
        File messages = new File(home, "messages")
        inbox = new File(messages, "inbox")
        outbox = new File(messages, "outbox")
        sent = new File(messages, "sent")
        
        inbox.mkdirs()
        outbox.mkdirs()
        sent.mkdirs()
    }
    
    public void onUILoadedEvent(UILoadedEvent e) {
        diskIO.execute({load()} as Runnable)
    }
    
    public void stop() {
        diskIO.shutdown()
        netIO.shutdown()
        timer.cancel()
    }
    
    private void load() {
        log.info("loading messages")
        loadFolder(inbox, inboxMessages, "inbox")
        loadFolder(outbox, outboxMessages, "outbox")
        loadFolder(sent, sentMessages, "sent")
        log.info("loaded messages")
        long interval = settings.messageSendInterval * 60 * 1000L
        timer.schedule({send()} as TimerTask, interval, interval)
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
    
    synchronized void onUIMessageEvent(UIMessageEvent e) {
        outboxMessages.add(e.message)
        diskIO.execute({persist(e.message, outbox)})
    }
    
    private void persist(MWMessage message, File folder) {
        File f = new File(folder, deriveName(message))
        f.withOutputStream { 
            message.write(it)
        }
    }
    
    private void moveToSent(MWMessage message) {
        String name = deriveName(message)
        File f = new File(outbox, name)
        File target = new File(sent, name)
        Files.move(f.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        eventBus.publish(new MessageSentEvent(message : message))
    }
    
    private static String deriveName(MWMessage message) {
        String ih = Base64.encode(message.getInfoHash().getRoot())
        "${ih}_${message.sender.getHumanReadableName()}_${message.timestamp}"
    }
    
    private synchronized void send() {
        Iterator<MWMessage> iter = outboxMessages.iterator()
        while(inProcess.size() < MAX_IN_PROCESS && iter.hasNext()) {
            MWMessage candidate = iter.next()
            if (inProcess.contains(candidate))
                continue
            inProcess.add(candidate)
            netIO.execute(deliver(candidate))
        }
    }
    
    private void deliver(MWMessage message) {
        Set<Persona> successful = new HashSet<>()
        for (Persona recipient : message.recipients) {
            if (deliverTo(message, recipient))
                successful.add(message)
        }
        if (successful.containsAll(message.recipients)) {
            synchronized(this) {
                inProcess.remove(message)
                outboxMessages.remove(message)
                sentMessages.add(message)
            }
            diskIO.execute({moveToSent(message)})
        }
    }
    
    public synchronized void onMessageReceivedEvent(MessageReceivedEvent e) {
        inboxMessages.add(e.message)
        diskIO.execute({persist(e.message, inbox)})
    }
    
    private boolean deliverTo(MWMessage message, Persona recipient) {
        try {
            Endpoint e = connector.connect(recipient.destination)
            OutputStream os = e.getOutputStream()
            os.write("ETTER\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Version:1\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Count:1\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
            message.write(os)
            os.flush()
            os.close()
            return true
        } catch (Exception e) {
            log.log(Level.WARNING, "failed to send message to ${recipient.getHumanReadableName()}", e)
            return false
        }
    }
}