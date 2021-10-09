package com.muwire.gui

import com.muwire.core.files.DirectoryUnsharedEvent
import com.muwire.core.messenger.UIFolderCreateEvent
import com.muwire.core.messenger.UIFolderDeleteEvent

import javax.swing.JTextField
import java.util.stream.Collectors

import static com.muwire.gui.Translator.trans
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.crypto.DSAEngine
import net.i2p.data.Base64
import net.i2p.data.Signature
import net.i2p.data.i2cp.MessageStatusMessage

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.nio.charset.StandardCharsets

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JOptionPane
import javax.swing.JTable

import com.muwire.core.Constants
import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.SplitPattern
import com.muwire.core.collections.FileCollection
import com.muwire.core.collections.UICollectionDeletedEvent
import com.muwire.core.download.Downloader
import com.muwire.core.download.UIDownloadCancelledEvent
import com.muwire.core.download.UIDownloadPausedEvent
import com.muwire.core.download.UIDownloadResumedEvent
import com.muwire.core.filecert.UICreateCertificateEvent
import com.muwire.core.filefeeds.Feed
import com.muwire.core.filefeeds.FeedItem
import com.muwire.core.filefeeds.UIDownloadFeedItemEvent
import com.muwire.core.filefeeds.UIFeedConfigurationEvent
import com.muwire.core.filefeeds.UIFeedDeletedEvent
import com.muwire.core.filefeeds.UIFeedUpdateEvent
import com.muwire.core.filefeeds.UIFilePublishedEvent
import com.muwire.core.filefeeds.UIFileUnpublishedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.trust.RemoteTrustList
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService.TrustEntry
import com.muwire.core.trust.TrustSubscriptionEvent
import com.muwire.core.upload.HashListUploader
import com.muwire.core.upload.Uploader
import com.muwire.core.util.DataUtil

@ArtifactProviderFor(GriffonController)
class MainFrameController {
    @Inject @Nonnull GriffonApplication application
    @MVCMember @Nonnull
    FactoryBuilderSupport builder

    @MVCMember @Nonnull
    MainFrameModel model
    @MVCMember @Nonnull
    MainFrameView view

    private volatile Core core

    @ControllerAction
    void search(ActionEvent evt) {
        if (evt?.getActionCommand() == null)
            return
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "search window")

        def searchField = builder.getVariable("search-field") 
        def search = searchField.getSelectedItem()
        searchField.model.addElement(search)
        performSearch(search)
    }
    
    private void performSearch(String search) {
        
        model.sessionRestored = true
        
        search = search.trim()
        if (search.length() == 0)
            return
        if (search.length() > 128) {
            try {
                Persona p = new Persona(new ByteArrayInputStream(Base64.decode(search)))
                String groupId = p.getHumanReadableName() + "-browse"
                def params = [:]
                params['host'] = p
                params['core'] = model.core
                mvcGroup.createMVCGroup("browse",groupId,params)
                return
            } catch (Exception notPersona) {
                search = search.substring(0,128)
            }
        }
        def uuid = UUID.randomUUID()
        Map<String, Object> params = new HashMap<>()
        params["search-terms"] = search
        params["uuid"] = uuid.toString()
        params["core"] = core
        params["settings"] = view.settings
        def group = mvcGroup.createMVCGroup("SearchTab", uuid.toString(), params)
        model.results[uuid.toString()] = group

        boolean hashSearch = false
        byte [] root = null
        if (search.length() == 44 && search.indexOf(" ") < 0) {
            try {
                root = Base64.decode(search)
                hashSearch = true
            } catch (Exception e) {
                // not a hash search
            }
        }

        def searchEvent
        byte [] payload
        if (hashSearch) {
            searchEvent = new SearchEvent(searchHash : root, uuid : uuid, oobInfohash: true,
                compressedResults : true, persona : core.me, collections : core.muOptions.searchCollections)
            payload = root
        } else {
            def nonEmpty = SplitPattern.termify(search)
            payload = String.join(" ",nonEmpty).getBytes(StandardCharsets.UTF_8)
            searchEvent = new SearchEvent(searchTerms : nonEmpty, uuid : uuid, oobInfohash: true,
            searchComments : core.muOptions.searchComments, compressedResults : true, persona : core.me,
            collections : core.muOptions.searchCollections, searchPaths: core.muOptions.searchPaths)
        }
        boolean firstHop = core.muOptions.allowUntrusted || core.muOptions.searchExtraHop

        Signature sig = DSAEngine.getInstance().sign(payload, core.spk)

        long timestamp = System.currentTimeMillis()
        core.eventBus.publish(new QueryEvent(searchEvent : searchEvent, firstHop : firstHop,
        replyTo: core.me.destination, receivedOn: core.me.destination,
        originator : core.me, sig : sig.data, queryTime : timestamp, sig2 : DataUtil.signUUID(uuid, timestamp, core.spk)))

    }

    void search(String infoHash, String tabTitle) {
        def cardsPanel = builder.getVariable("cards-panel")
        cardsPanel.getLayout().show(cardsPanel, "search window")
        def uuid = UUID.randomUUID()
        Map<String, Object> params = new HashMap<>()
        params["search-terms"] = tabTitle
        params["uuid"] = uuid.toString()
        params["core"] = core
        params["settings"] = view.settings
        def group = mvcGroup.createMVCGroup("SearchTab", uuid.toString(), params)
        model.results[uuid.toString()] = group

        byte [] infoHashBytes = Base64.decode(infoHash)
        Signature sig = DSAEngine.getInstance().sign(infoHashBytes, core.spk)
        long timestamp = System.currentTimeMillis()
        byte [] sig2 = DataUtil.signUUID(uuid, timestamp, core.spk)
        
        def searchEvent = new SearchEvent(searchHash : Base64.decode(infoHash), uuid:uuid,
            oobInfohash: true, persona : core.me)
        core.eventBus.publish(new QueryEvent(searchEvent : searchEvent, firstHop : true,
            replyTo: core.me.destination, receivedOn: core.me.destination,
            originator : core.me, sig : sig.data, queryTime : timestamp, sig2 : sig2))
    }
    
    private int selectedDownload() {
        def downloadsTable = builder.getVariable("downloads-table")
        def selected = downloadsTable.getSelectedRow()
        def sortEvt = mvcGroup.view.lastDownloadSortEvent
        if (sortEvt != null)
            selected = downloadsTable.rowSorter.convertRowIndexToModel(selected)
        selected
    }

    @ControllerAction
    void trustPersonaFromSearch() {
        int selected = builder.getVariable("searches-table").getSelectedRow()
        if (selected < 0)
            return
        String reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
        Persona p = model.searches[selected].originator
        core.eventBus.publish( new TrustEvent(persona : p, level : TrustLevel.TRUSTED, reason : reason) )
    }

    @ControllerAction
    void distrustPersonaFromSearch() {
        int selected = builder.getVariable("searches-table").getSelectedRow()
        if (selected < 0)
            return
        String reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
        Persona p = model.searches[selected].originator
        core.eventBus.publish( new TrustEvent(persona : p, level : TrustLevel.DISTRUSTED, reason : reason) )
    }

    @ControllerAction
    void cancel() {
        def downloader = model.downloads[selectedDownload()].downloader
        downloader.cancel()
        model.downloadInfoHashes.remove(downloader.getInfoHash())
        core.eventBus.publish(new UIDownloadCancelledEvent(downloader : downloader))
    }

    @ControllerAction
    void resume() {
        def downloader = model.downloads[selectedDownload()].downloader
        downloader.resume()
        core.eventBus.publish(new UIDownloadResumedEvent())
    }

    @ControllerAction
    void pause() {
        def downloader = model.downloads[selectedDownload()].downloader
        downloader.pause()
        core.eventBus.publish(new UIDownloadPausedEvent())
    }
    
    @ControllerAction
    void preview() {
        def downloader = model.downloads[selectedDownload()].downloader
        def params = [:]
        params['downloader'] = downloader
        mvcGroup.createMVCGroup("download-preview", params)
    }
    
    @ControllerAction
    void open() {
        List<SharedFile> selected = view.selectedSharedFiles()
        if (selected == null || selected.size() != 1)
            return
        SharedFile sf = selected[0]
        Desktop.getDesktop().open(sf.file)
    }

    @ControllerAction
    void clear() {
        model.downloads.removeAll { 
            def state = it.downloader.getCurrentState()
            state == Downloader.DownloadState.CANCELLED ||
                state == Downloader.DownloadState.FINISHED ||
                state == Downloader.DownloadState.HOPELESS
        }
        
         model.clearButtonEnabled = false

    }

    @ControllerAction
    void openContainingFolderFromDownload() {
        def downloader = model.downloads[selectedDownload()].downloader

        try {
            Desktop.getDesktop().open(downloader.file.getParentFile())
        } catch (Exception ignored) {}
    }
    
    @ControllerAction
    void addContact() {
        def params = [:]
        params.core = core
        mvcGroup.createMVCGroup("add-contact", params)
    }
    
    @ControllerAction
    void removeContact() {
        markTrust(TrustLevel.NEUTRAL)
    }

    private void markTrust(TrustLevel level) {
        int row = view.getSelectedContactsTableRow()
        if (row < 0)
            return
        String reason = null
        if (level != TrustLevel.NEUTRAL)
            reason = JOptionPane.showInputDialog(trans("ENTER_REASON_OPTIONAL"))
        builder.getVariable("contacts-table").model.fireTableDataChanged()
        core.eventBus.publish(new TrustEvent(persona : model.contacts[row].persona, level : level, reason : reason))
    }

    @ControllerAction
    void markTrusted() {
        markTrust(TrustLevel.TRUSTED)
        model.markTrustedButtonEnabled = false
    }

    @ControllerAction
    void markDistrusted() {
        markTrust(TrustLevel.DISTRUSTED)
        model.subscribeButtonEnabled = false
        model.markDistrustedButtonEnabled = false
    }

    @ControllerAction
    void subscribe() {
        int row = view.getSelectedContactsTableRow()
        if (row < 0)
            return
        Persona p = model.contacts[row].persona
        core.muOptions.trustSubscriptions.add(p)
        saveMuWireSettings()
        core.eventBus.publish(new TrustSubscriptionEvent(persona : p, subscribe : true))
        model.subscribeButtonEnabled = false
        model.markDistrustedButtonEnabled = false
        model.removeContactButtonEnabled = false
    }
    
    @ControllerAction
    void subscribeToFeed() {
        String address = JOptionPane.showInputDialog(trans("COPY_PASTE_FEED_ID"))
        if (address == null)
            return
        Persona p
        try {
            p = new Persona(new ByteArrayInputStream(Base64.decode(address)))
        } catch (Exception bad) {
            JOptionPane.showMessageDialog(null, trans("ADD_CONTACT_INVALID_ID_TITLE"), trans("ADD_CONTACT_INVALID_ID_TITLE"), JOptionPane.ERROR_MESSAGE)
            return
        }

        Feed feed = new Feed(p)
        feed.setAutoDownload(core.muOptions.defaultFeedAutoDownload)
        feed.setSequential(core.muOptions.defaultFeedSequential)
        feed.setItemsToKeep(core.muOptions.defaultFeedItemsToKeep)
        feed.setUpdateInterval(core.muOptions.defaultFeedUpdateInterval)

        core.eventBus.publish(new UIFeedConfigurationEvent(feed : feed, newFeed: true))
        Thread.sleep(10)
        view.refreshFeeds()
    }
    
    @ControllerAction
    void showMyFeed() {
        def params = [:]
        params['core'] = model.core
        mvcGroup.createMVCGroup("my-feed", params)
    }

    @ControllerAction
    void review() {
        RemoteTrustList list = getSelectedTrustList()
        if (list == null)
            return
        Map<String,Object> env = new HashMap<>()
        env["trustList"] = list
        env["trustService"] = core.trustService
        env["eventBus"] = core.eventBus
        mvcGroup.createMVCGroup("trust-list", env)

    }

    @ControllerAction
    void update() {
        RemoteTrustList list = getSelectedTrustList()
        if (list == null)
            return
        core.eventBus.publish(new TrustSubscriptionEvent(persona : list.persona, subscribe : true))
    }

    @ControllerAction
    void unsubscribe() {
        RemoteTrustList list = getSelectedTrustList()
        if (list == null)
            return
        core.muOptions.trustSubscriptions.remove(list.persona)
        saveMuWireSettings()
        model.subscriptions.remove(list)
        JTable table = builder.getVariable("subscription-table")
        table.model.fireTableDataChanged()
        core.eventBus.publish(new TrustSubscriptionEvent(persona : list.persona, subscribe : false))
    }

    private RemoteTrustList getSelectedTrustList() {
        int row = view.getSelectedContactSubscriptionTableRow()
        if (row < 0)
            return null
        model.subscriptions[row]
    }
    
    @ControllerAction
    void browseFromTrusted() {
        int row = view.getSelectedContactsTableRow()
        if (row < 0)
            return
        Persona p = model.contacts[row].persona
        
        String groupId = UUID.randomUUID().toString()
        def params = [:]
        params['host'] = p
        params['core'] = model.core
        mvcGroup.createMVCGroup("browse",groupId,params)
        view.showSearchWindow.call()
    }
    
    @ControllerAction
    void browseCollectionsFromTrusted() {
        int row = view.getSelectedContactsTableRow()
        if (row < 0)
            return
        Persona p = model.contacts[row].persona
        
        UUID uuid = UUID.randomUUID()
        def params = [:]
        params.fileName = p.getHumanReadableName()
        params.eventBus = core.eventBus
        params.everything = true
        params.uuid = uuid
        params.host = p
        mvcGroup.createMVCGroup("collection-tab", uuid.toString(), params)
        view.showSearchWindow.call()
    }
    
    @ControllerAction
    void chatFromTrusted() {
        int row = view.getSelectedContactsTableRow()
        if (row < 0)
            return
        Persona p = model.contacts[row].persona
        
        startChat(p)
        view.showChatWindow.call()
    }
    
    @ControllerAction
    void browseFromUpload() {
        Uploader u = view.selectedUploader()
        if (u == null)
            return
        Persona p = u.getDownloaderPersona()

        String groupId = UUID.randomUUID().toString()
        def params = [:]
        params['host'] = p
        params['core'] = model.core
        mvcGroup.createMVCGroup("browse",groupId,params)
        view.showSearchWindow.call()
    }
    
    @ControllerAction
    void browseCollectionsFromUpload() {
        Uploader u = view.selectedUploader()
        if (u == null)
            return
        Persona p = u.getDownloaderPersona()
        
        UUID uuid = UUID.randomUUID()
        def params = [:]
        params.fileName = p.getHumanReadableName()
        params.eventBus = core.eventBus
        params.host = p
        params.uuid = uuid
        params.everything = true
        
        mvcGroup.createMVCGroup("collection-tab", uuid.toString(), params)
        view.showSearchWindow.call()
    }
    
    @ControllerAction
    void subscribeFromUpload() {
        Uploader u = view.selectedUploader()
        if (u == null)
            return
        Persona p = u.getDownloaderPersona()
        
        Feed feed = new Feed(p)
        feed.setAutoDownload(core.muOptions.defaultFeedAutoDownload)
        feed.setSequential(core.muOptions.defaultFeedSequential)
        feed.setItemsToKeep(core.muOptions.defaultFeedItemsToKeep)
        feed.setUpdateInterval(core.muOptions.defaultFeedUpdateInterval)
        
        core.eventBus.publish(new UIFeedConfigurationEvent(feed : feed, newFeed : true))
        view.showFeedsWindow.call()
    }
    
    @ControllerAction
    void chatFromUpload() {
        Uploader u = view.selectedUploader()
        if (u == null)
            return
        Persona p = u.getDownloaderPersona()
        startChat(p)
        view.showChatWindow.call()
    }

    void unshareSelectedFile() {
        def sfs = view.selectedSharedFiles()
        Set<File> folders = view.selectedFolders()
        if (sfs == null && folders.isEmpty()) 
            return
        if (sfs == null)
            sfs = Collections.emptyList()
        
        List<SharedFile> toUnshare = new ArrayList<>()
        sfs.each { SharedFile sf ->
            
            if (view.settings.collectionWarning) {
                Set<InfoHash> collectionsInfoHashes = core.collectionManager.collectionsForFile(sf.rootInfoHash)
                if (!collectionsInfoHashes.isEmpty()) {
                    String[] affected = collectionsInfoHashes.collect({core.collectionManager.getByInfoHash(it)}).collect{it.name}.toArray(new String[0])

                    boolean [] answer = new boolean[1]
                    def props = [:]
                    props.collections = affected
                    props.answer = answer
                    props.fileName = sf.file.getName()
                    props.settings = view.settings
                    props.home = core.home
                    def mvc = mvcGroup.createMVCGroup("collection-warning", props)
                    mvc.destroy()
                    if (!answer[0]) {
                        File f = sf.getFile()
                        while(true) {
                            File parent = f.getParentFile()
                            if (parent == null)
                                break
                            folders.remove(parent)
                            f = parent
                        }
                        return
                    }
                }
            }
            toUnshare.add(sf)
        }
        if (!folders.isEmpty())
            core.eventBus.publish(new DirectoryUnsharedEvent(directories: folders.toArray(new File[0]), deleted: false))
        if (!toUnshare.isEmpty())
            core.eventBus.publish(new FileUnsharedEvent(unsharedFiles : toUnshare.toArray(new SharedFile[0])))
    }
    
    @ControllerAction
    void addComment() {
        def selectedFiles = view.selectedSharedFiles()
        if (selectedFiles == null || selectedFiles.isEmpty())
            return
        
        Map<String, Object> params = new HashMap<>()
        params['selectedFiles'] = selectedFiles
        params['core'] = core
        mvcGroup.createMVCGroup("add-comment", "Add Comment", params)
    }
    
    @ControllerAction
    void clearUploads() {
        model.uploads.removeAll { it.finished }
    }
    
    @ControllerAction
    void showInLibrary() {
        Uploader uploader = view.selectedUploader()
        if (uploader == null)
            return
        SharedFile sf = null
        if (uploader instanceof HashListUploader) {
            InfoHash infoHash = uploader.infoHash
            SharedFile[] sfs = core.fileManager.rootToFiles.get(infoHash)
            if (sfs != null  && sfs.length > 0)
                sf = sfs[0]
        } else {
            File f = uploader.file
            sf = core.fileManager.fileToSharedFile.get(f)
        }
        
        if (sf == null)
            return // can happen if user un-shared

        view.focusOnSharedFile(sf)
    }
    
    @ControllerAction
    void restoreSession() {
        model.sessionRestored = true
        view.settings.openTabs.each { 
            performSearch(it)
        }
    }
    
    @ControllerAction
    void issueCertificate() {
        if (view.settings.certificateWarning) {
            def params = [:]
            params['settings'] = view.settings
            params['home'] = core.home
            mvcGroup.createMVCGroup("certificate-warning", params)
        } else {
            int count = 0
            view.selectedSharedFiles().each {
                count++
                core.eventBus.publish(new UICreateCertificateEvent(sharedFile : it))
            }
            if (count > 1)
                JOptionPane.showMessageDialog(null, trans("CERTIFICATES_ISSUED"))
            else if (count == 1)
                JOptionPane.showMessageDialog(null, trans("CERTIFICATE_ISSUED"))
        }
    }
    
    @ControllerAction
    void showFileDetails() {
        def selected = view.selectedSharedFiles()
        if (selected == null || selected.size() != 1) {
            JOptionPane.showMessageDialog(null, trans("PLEASE_SELECT_ONE_FILE_DETAILS"))
            return
        }
        def params = [:]
        params['sf'] = selected[0]
        params['core'] = core
        mvcGroup.createMVCGroup("shared-file", params)
    }
    
    @ControllerAction
    void openContainingFolder() {
        def selected = view.selectedSharedFiles()
        if (selected == null || selected.size() != 1) {
            JOptionPane.showMessageDialog(null, trans("PLEASE_SELECT_ONE_FILE_FOLDER"))
            return
        }
        
        try {
            Desktop.getDesktop().open(selected[0].file.getParentFile())
        } catch (Exception ignored) {}
    }
    
    @ControllerAction
    void filterLibrary() {
        JTextField field = builder.getVariable("library-filter-textfield")
        String filter = field.getText()
        if (filter == null)
            return
        filter = filter.strip().replaceAll(SplitPattern.SPLIT_PATTERN," ").toLowerCase()
        String [] split = filter.split(" ")
        def hs = new HashSet()
        split.each {if (it.length() > 0) hs << it}
        model.filter = hs.toArray(new String[0])
        model.filterLibrary()
    }
    
    @ControllerAction
    void clearLibraryFilter() {
        model.filter = null
        model.filterLibrary()
    }
    
    @ControllerAction
    void startChatServer() {
        model.core.chatServer.start()
        model.chatServerRunning = true
        
        if (!mvcGroup.getChildrenGroups().containsKey("local-chat-server")) {
            def params = [:]
            params['core'] = model.core
            params['host'] = model.core.me
            params['chatNotificator'] = view.chatNotificator
            mvcGroup.createMVCGroup("chat-server","local-chat-server", params)
        }
    }
    
    @ControllerAction
    void stopChatServer() {
        model.core.chatServer.stop()
        model.chatServerRunning = false
    }

    @ControllerAction
    void connectChatServer() {
        String address = JOptionPane.showInputDialog(trans("COPY_PASTE_SERVER_ADDRESS"))
        if (address == null)
            return
        Persona p
        try {
            p = new Persona(new ByteArrayInputStream(Base64.decode(address)))
        } catch (Exception bad) {
            JOptionPane.showMessageDialog(null, trans("INVALID_SERVER_ADDRESS"), trans("INVALID_SERVER_ADDRESS"), JOptionPane.ERROR_MESSAGE)
            return
        }
     
        startChat(p)   
    }
    
    @ControllerAction
    void copyShort() {
        copy(model.core.me.getHumanReadableName())
    }
    
    @ControllerAction
    void copyFull() {
        copy(model.core.me.toBase64())
    }
    
    private void copy(String s) {
        StringSelection selection = new StringSelection(s)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
    
    @ControllerAction
    void publish() {
        def selectedFiles = view.selectedSharedFiles()
        if (selectedFiles == null || selectedFiles.isEmpty())
            return
        
        if (model.publishButtonText == "UNPUBLISH") {
            selectedFiles.each { 
                it.unpublish()
                model.core.eventBus.publish(new UIFileUnpublishedEvent(sf : it))
            }
        } else {
            long now = System.currentTimeMillis()
            selectedFiles.stream().filter({!it.isPublished()}).forEach({
                it.publish(now)
                model.core.eventBus.publish(new UIFilePublishedEvent(sf : it))
            })
        }
        view.refreshSharedFiles()
    }
    
    @ControllerAction
    void updateFileFeed() {
        Feed feed = view.selectedFeed()
        if (feed == null)
            return
        model.core.eventBus.publish(new UIFeedUpdateEvent(host: feed.getPublisher()))
    }
    
    @ControllerAction
    void unsubscribeFileFeed() {
        Feed feed = view.selectedFeed()
        if (feed == null)
            return
        model.core.eventBus.publish(new UIFeedDeletedEvent(host : feed.getPublisher()))
        runInsideUIAsync {
            model.feeds.remove(feed)
            model.feedItems.clear()
            view.refreshFeeds()
        }
    }
    
    @ControllerAction
    void configureFileFeed() {
        Feed feed = view.selectedFeed()
        if (feed == null)
            return
        
        def params = [:]
        params['core'] = core
        params['feed'] = feed
        mvcGroup.createMVCGroup("feed-configuration", params)
    }
    
    @ControllerAction
    void downloadFeedItem() {
        List<FeedItem> items = view.selectedFeedItems()
        if (items == null || items.isEmpty())
            return
        Feed f = model.core.getFeedManager().getFeed(items.get(0).getPublisher())
        items.each { 
            if (!model.canDownload(it.getInfoHash()))
                return
            File target = new File(application.context.get("muwire-settings").downloadLocation, it.getName())
            model.core.eventBus.publish(new UIDownloadFeedItemEvent(item : it, target : target, sequential : f.isSequential()))
        }
        view.showDownloadsWindow.call()
    }
    
    @ControllerAction
    void viewFeedItemComment() {
        List<FeedItem> items = view.selectedFeedItems()
        if (items == null || items.size() != 1)
            return
        FeedItem item = items.get(0)
        
        String groupId = Base64.encode(item.getInfoHash().getRoot())
        Map<String, Object> params = new HashMap<>()
        params['text'] = DataUtil.readi18nString(Base64.decode(item.getComment()))
        params['name'] = item.getName()
        
        mvcGroup.createMVCGroup("show-comment", groupId, params)
    }
    
    @ControllerAction
    void viewFeedItemCertificates() {
        List<FeedItem> items = view.selectedFeedItems()
        if (items == null || items.size() != 1)
            return
        FeedItem item = items.get(0)
        
        def params = [:]
        params['core'] = core
        params['host'] = item.getPublisher()
        params['infoHash'] = item.getInfoHash()
        params['name'] = item.getName()
        mvcGroup.createMVCGroup("fetch-certificates", params)
    }
    
    @ControllerAction
    void collection() {
        def files = view.selectedSharedFiles()

        if (files != null && files.size() > Constants.COLLECTION_MAX_ITEMS) {
            JOptionPane.showMessageDialog(null, trans("CREATE_COLLECTION_MAX_FILES", Constants.COLLECTION_MAX_ITEMS, files.size()),
                trans("CREATE_COLLECTION_MAX_FILES_TITLE"), JOptionPane.WARNING_MESSAGE)
            return
        }
        if (files == null)
            files = new ArrayList<>()        
        def params = [:]
        params['files'] = files
        params['spk'] = model.core.spk
        params['me'] = model.core.me
        params['eventBus'] = model.core.eventBus
        
        mvcGroup.createMVCGroup("collection-wizard", UUID.randomUUID().toString(), params)
    }
    
    @ControllerAction
    void viewCollectionComment() {
        int row = view.selectedCollectionRow()
        if (row < 0)
            return
        FileCollection collection = model.localCollections.get(row)

        def params = [:]
        params['text'] = collection.comment
        mvcGroup.createMVCGroup("show-comment", params)
    }
    
    @ControllerAction
    void copyCollectionHash() {
        int row = view.selectedCollectionRow()
        if (row < 0)
            return
        FileCollection collection = model.localCollections.get(row)
        
        String b64 = Base64.encode(collection.getInfoHash().getRoot())
        
        StringSelection selection = new StringSelection(b64)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
    
    @ControllerAction
    void deleteCollection() {
        int row = view.selectedCollectionRow()
        if (row < 0)
            return
        FileCollection collection = model.localCollections.get(row)
        UICollectionDeletedEvent e = new UICollectionDeletedEvent(collection : collection)
        model.core.eventBus.publish(e)
        model.localCollections.remove(row)
        view.collectionsTable.model.fireTableDataChanged()
        model.collectionFiles.clear()
        view.collectionFilesTable.model.fireTableDataChanged()
    }
    
    @ControllerAction
    void viewItemComment() {
        int row = view.selectedItemRow()
        if (row < 0)
            return
        SharedFile sf = model.collectionFiles.get(row)
        
        def params = [:]
        params['text'] = DataUtil.readi18nString(Base64.decode(sf.getComment()))
        mvcGroup.createMVCGroup("show-comment", params)
    }
    
    @ControllerAction
    void showCollectionTool() {
        int row = view.selectedCollectionRow()
        if (row < 0)
            return
        FileCollection collection = model.localCollections.get(row)
        
        def params = [:]
        params['collection'] = collection
        mvcGroup.createMVCGroup("collections-tool", params)
    }
    
    @ControllerAction
    void messageComposeFromUpload() {
        Uploader u = view.selectedUploader()
        if (u == null)
            return
        Persona p = u.getDownloaderPersona()
        
        def params = [:]
        params.recipients = new HashSet<>(Collections.singletonList(p))
        params.core = core
        mvcGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
    }
    
    @ControllerAction
    void messageFromTrusted() {
        int row = view.getSelectedContactsTableRow()
        if (row < 0)
            return
        TrustEntry te = model.contacts[row]
        
        def params = [:]
        params.recipients = new HashSet<>(Collections.singletonList(te.persona))
        params.core = core
        mvcGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
    }
    
    @ControllerAction
    void copyIdFromUploads() {
        Uploader u = view.selectedUploader()
        if (u == null)
            return
        CopyPasteSupport.copyToClipboard(u.getDownloaderPersona().toBase64())
    }
    
    @ControllerAction
    void copyIdFromTrusted() {
        int row = view.getSelectedContactsTableRow()
        if (row < 0)
            return
        TrustEntry te = model.contacts.get(row)
        CopyPasteSupport.copyToClipboard(te.persona.toBase64())
    }
    
    @ControllerAction
    void copyIdFromFeed() {
        Feed feed = view.selectedFeed()
        if (feed == null)
            return
        CopyPasteSupport.copyToClipboard(feed.getPublisher().toBase64())
    }
    
    @ControllerAction
    void createMessageFolder() {
        String name = null
        while(name == null) {
            name = JOptionPane.showInputDialog(trans("FOLDER_ENTER_NAME"))
            if (name == null)
                return
            name = name.trim()
            if (name.length() == 0) {
                JOptionPane.showMessageDialog(null, trans("FOLDER_NAME_EMPTY"),
                    trans("FOLDER_NAME_EMPTY"), JOptionPane.WARNING_MESSAGE)
                name = null
                continue
            }
            if (model.messageFoldersMap.containsKey(name)) {
                JOptionPane.showMessageDialog(null, trans("FOLDER_ALREADY_EXISTS"),
                    trans("FOLDER_ALREADY_EXISTS"), JOptionPane.WARNING_MESSAGE)
                name = null
                continue
            }
        }
        
        def params = [:]
        params['core'] = core
        params['outgoing'] = false
        params['name'] = name
        def group = application.mvcGroupManager.createMVCGroup('message-folder', 'folder-$name', params)
        view.addUserMessageFolder(group)

        UIFolderCreateEvent event = new UIFolderCreateEvent(name: name)
        core.eventBus.publish(event)
    }
    
    @ControllerAction
    void deleteMessageFolder() {
        
        def group = model.messageFoldersMap.get(model.folderIdx)
        if (group == null)
            return
        
        if (!group.model.messages.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(null, trans("FOLDER_CONFIRM_DELETE"),
                    trans("FOLDER_CONFIRM_DELETE_TITLE"), JOptionPane.YES_NO_OPTION)
            if (result != JOptionPane.YES_OPTION)
                return
        }
        
        view.deleteUserMessageFolder(model.folderIdx)

        UIFolderDeleteEvent event = new UIFolderDeleteEvent(name: model.folderIdx)
        core.eventBus.publish(event)
    }
    
    void startChat(Persona p) {
        if (!mvcGroup.getChildrenGroups().containsKey(p.getHumanReadableName())) {
            def params = [:]
            params['core'] = model.core
            params['host'] = p
            params['chatNotificator'] = view.chatNotificator
            mvcGroup.createMVCGroup("chat-server", p.getHumanReadableName(), params)
        } else 
            mvcGroup.getChildrenGroups().get(p.getHumanReadableName()).model.connect()
    }

    void saveMuWireSettings() {
        core.saveMuSettings()
    }

    void mvcGroupInit(Map<String, String> args) {
        application.addPropertyChangeListener("core", {e->
            core = e.getNewValue()
        })
    }
}
