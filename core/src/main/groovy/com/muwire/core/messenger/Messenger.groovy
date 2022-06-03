package com.muwire.core.messenger

import com.muwire.core.profile.MWProfile
import com.muwire.core.profile.MWProfileHeader

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.function.Supplier
import java.util.logging.Level
import java.util.zip.GZIPOutputStream

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

    public final static String INBOX = "inbox"
    public final static String OUTBOX = "outbox"
    public final static String SENT = "sent"
    public static final Set<String> RESERVED_FOLDERS = new HashSet<>()
    static {
        RESERVED_FOLDERS.add(INBOX)
        RESERVED_FOLDERS.add(OUTBOX)
        RESERVED_FOLDERS.add(SENT)
    }
    
    private static final int MAX_IN_PROCESS = 4
    
    private final EventBus eventBus
    private final Map<String,File> folders = new HashMap<>()
    private final I2PConnector connector
    private final Supplier<MWProfile> profileSupplier
    private final MuWireSettings settings
    
    private final Map<File, Set<MWMessage>> messages = new HashMap<>()
    private final Set<MWMessage> inboxMessages = new LinkedHashSet<>()
    private final Set<MWMessage> outboxMessages = new LinkedHashSet<>()
    private final Set<MWMessage> sentMessages = new LinkedHashSet<>()
    private final File home
    private File localFolders
    
    private final Set<MWMessage> inProcess = new HashSet<>()
    
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor({ Runnable r ->
        new Thread(r, "messenger-disk-io")
    } as ThreadFactory)
    
    private final ExecutorService netIO = Executors.newCachedThreadPool({ Runnable r ->
        new Thread(r, "messenger-net-io")
    } as ThreadFactory)
    
    private final Timer timer = new Timer()
    private long lastSendTime
    
    
    Messenger(EventBus eventBus, File home, I2PConnector connector, 
              Supplier<MWProfile> profileSupplier, MuWireSettings settings) {
        this.eventBus = eventBus
        this.home = home
        this.connector = connector
        this.profileSupplier = profileSupplier
        this.settings = settings
        
        File messages = new File(home, "messages")
        localFolders = new File(messages, "folders")
        localFolders.mkdirs()
        
        folders.put(INBOX, new File(messages, INBOX))
        folders.put(OUTBOX, new File(messages, OUTBOX))
        folders.put(SENT, new File(messages, SENT))
     
        folders.values().each {it.mkdirs()}

        this.messages.put(folders[INBOX], inboxMessages)
        this.messages.put(folders[OUTBOX], outboxMessages)
        this.messages.put(folders[SENT], sentMessages)
    }
    
    Set<String> getFolderNames() {
        folders.keySet()
    }
    
    void onUILoadedEvent(UILoadedEvent e) {
        diskIO.execute({load()} as Runnable)
    }
    
    void stop() {
        diskIO.shutdown()
        netIO.shutdown()
        timer.cancel()
    }
    
    private void load() {
        log.info("loading messages")

        localFolders.listFiles().toList().stream().filter({ it.isDirectory() }).
                forEach({
                    folders.put(it.getName(), it)
                    eventBus.publish(new MessageFolderLoadingEvent(name: it.getName()))
                })

        folders.each { name, file ->
            log.info("loading message folder $name")
            Set<MWMessage> set = messages.get(file)
            if (set == null) {
                set = new LinkedHashSet<>()
                messages.put(file, set)
            }
            loadFolder(file, set, name)
        }
        log.info("loaded messages")
        timer.schedule({send()} as TimerTask, 1000, 1000)
    }
    
    private void loadFolder(File file, Set<MWMessage> dest, String folder) {
        Files.walk(file.toPath())
            .filter({it.getFileName().toString().endsWith(".mwmessage")})
            .forEach { Path path ->
                try {
                    File f = path.toFile()
                    MWMessage message
                    f.withInputStream {
                        message = new MWMessage(it)
                    }
                    addMessage(message, dest)
                    MWProfileHeader header = null
                    File profile = new File(file, deriveProfile(message))
                    if (profile.exists()) {
                        profile.withInputStream {
                            header = new MWProfileHeader(it)
                        }
                    }
                    File unread = new File(file, deriveUnread(message))
                    eventBus.publish(new MessageLoadedEvent(message: message, folder: folder, 
                            unread: unread.exists(), profileHeader: header))
                } catch (IOException iox) {
                    log.warning("couldn't load message from $path" )
                }
        }
    }
    
    private synchronized void addMessage(MWMessage message, Set<MWMessage> dest) {
        dest.add(message)
    }
    
    synchronized void onUIMessageEvent(UIMessageEvent e) {
        outboxMessages.add(e.message)
        diskIO.execute({persist(e.message, folders.get(OUTBOX))})
    }
    
    synchronized void onUIMessageReadEvent(UIMessageReadEvent e) {
        diskIO.execute({
            File unread = new File(folders.get(e.folder), deriveUnread(e.message))
            unread.delete()
        })
    }
    
    private void persist(MWMessage message, File folder) {
        File f = new File(folder, deriveName(message))
        f.withOutputStream { 
            message.write(it)
        }
    }
    
    private void moveToSent(MWMessage message) {
        String name = deriveName(message)
        File f = new File(folders.get(OUTBOX), name)
        File target = new File(folders.get(SENT), name)
        Files.move(f.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        eventBus.publish(new MessageSentEvent(message : message))
        log.fine("moved message to ${message.recipients} to sent folder")
    }
    
    private static String deriveName(MWMessage message) {
        namePrefix(message) + ".mwmessage"
    }
    
    private static String deriveUnread(MWMessage message) {
        namePrefix(message) + ".unread"
    }
    
    private static String deriveProfile(MWMessage message) {
        namePrefix(message) + ".profile"
    }
    
    private static String namePrefix(MWMessage message) {
        String ih = Base64.encode(message.getInfoHash().getRoot())
        "${ih}_${message.sender.getHumanReadableName()}_${message.timestamp}"
    }
    
    private synchronized void send() {
        final long now = System.currentTimeMillis()
        if (now - lastSendTime < settings.messageSendInterval * 1000)
            return
        lastSendTime = now
        
        log.fine("sending messages...")
        
        Iterator<MWMessage> iter = outboxMessages.iterator()
        while(inProcess.size() < MAX_IN_PROCESS && iter.hasNext()) {
            MWMessage candidate = iter.next()
            if (inProcess.contains(candidate))
                continue
            inProcess.add(candidate)
            netIO.execute({deliver(candidate)})
        }
    }
    
    private void deliver(MWMessage message) {
        Set<Persona> successful = new HashSet<>()
        for (Persona recipient : message.recipients) {
            if (deliverTo(message, recipient))
                successful.add(recipient)
        }
        if (successful.containsAll(message.recipients)) {
            synchronized(this) {
                outboxMessages.remove(message)
                sentMessages.add(message)
            }
            diskIO.execute({moveToSent(message)})
        }
    }
    
    public synchronized void onMessageReceivedEvent(MessageReceivedEvent e) {
        if (inboxMessages.add(e.message)) {
            diskIO.execute({
                File unread = new File(folders.get(INBOX), deriveUnread(e.message))
                unread.createNewFile()
                if (e.profileHeader != null) {
                    File profile = new File(folders.get(INBOX), deriveProfile(e.message))
                    profile.withOutputStream {
                        e.profileHeader.write(it)
                    }
                }
                persist(e.message, folders.get(INBOX))
            })
        }
    }
    
    private boolean deliverTo(MWMessage message, Persona recipient) {
        try {
            Endpoint e = connector.connect(recipient.destination)
            OutputStream os = e.getOutputStream()
            os.write("LETTER\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Version:1\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("Count:1\r\n".getBytes(StandardCharsets.US_ASCII))
            MWProfile profile = profileSupplier.get()
            if (profile != null)
                os.write("ProfileHeader:${profile.getHeader().toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
            os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
            os = new GZIPOutputStream(os)
            message.write(os)
            os.flush()
            try {
                os.close()
            } catch (Exception ignore) {}
            return true
        } catch (Exception e) {
            log.log(Level.WARNING, "failed to send message to ${recipient.getHumanReadableName()}", e)
            return false
        } finally {
            synchronized(this) {
                inProcess.remove(message)
            }
        }
    }
    
    synchronized void onUIMessageDeleteEvent(UIMessageDeleteEvent e) {
        def file = folders.get(e.folder)
        def set = messages.get(file)
        deleteFromFolder(e.message, set, file)
    }
    
    private void deleteFromFolder(MWMessage message, Set<MWMessage> set, File file) {
        set.remove(message)
        File messageFile = new File(file, deriveName(message))
        messageFile.delete()
        File unreadFile = new File(file, deriveUnread(message))
        unreadFile.delete()
        File profileFile = new File(file, deriveProfile(message))
        profileFile.delete()
    }
    
    synchronized void onUIFolderCreateEvent(UIFolderCreateEvent e) {
        File folder = new File(localFolders, e.name)
        if (folder.mkdir()) {
            folders.put(e.name, folder)
            messages.put(folder, new LinkedHashSet())
        }
    }
    
    synchronized void onUIFolderDeleteEvent(UIFolderDeleteEvent e) {
        File folder = new File(localFolders, e.name)
        if (!folder.exists())
            return
        
        folders.remove(e.name)
        messages.remove(folder)
        
        diskIO.execute({
            folder.deleteDir()
        })
    }
    
    synchronized void onUIMessageMovedEvent(UIMessageMovedEvent e) {
        diskIO.execute {
            File containerFrom
            if (RESERVED_FOLDERS.contains(e.from)) {
                containerFrom = new File(home, "messages")
            } else {
                containerFrom = localFolders
            }

            File from = new File(containerFrom, e.from)
            from = new File(from, deriveName(e.message))
            if (!from.exists()) {
                return
            }

            File unread = new File(containerFrom, e.from)
            unread = new File(unread, deriveUnread(e.message))
            boolean unreadExists = unread.exists()

            File profileFrom = new File(containerFrom, e.from)
            profileFrom = new File(profileFrom, deriveProfile(e.message))

            File containerTo
            if (RESERVED_FOLDERS.contains(e.to)) {
                containerTo = new File(home, "messages")
            } else {
                containerTo = localFolders
            }

            File to = new File(containerTo, e.to)
            to = new File(to, deriveName(e.message))

            if (to.exists()) {
                return
            }

            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE)

            if (unreadExists) {
                unread.delete()
                unread = new File(containerTo, e.to)
                unread = new File(unread, deriveUnread(e.message))
                unread.createNewFile()
            }
            
            MWProfileHeader profileHeader = null
            if (profileFrom.exists()) {
                profileFrom.withInputStream {
                    profileHeader = new MWProfileHeader(it)
                }
                File profileTo = new File(containerTo, e.to)
                profileTo = new File(profileTo, deriveProfile(e.message))
                Files.move(profileFrom.toPath(), profileTo.toPath(), StandardCopyOption.ATOMIC_MOVE)
            }

            eventBus.publish(new MessageLoadedEvent(message: e.message, folder: e.to,
                    unread: unreadExists, profileHeader: profileHeader))
        }
    }
}