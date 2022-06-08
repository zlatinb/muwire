package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.collections.UIDownloadCollectionEvent
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.MWMessageAttachment
import com.muwire.core.messenger.UIDownloadAttachmentEvent
import com.muwire.core.messenger.UIMessageDeleteEvent
import com.muwire.core.messenger.UIMessageReadEvent
import com.muwire.gui.profile.ViewProfileHelper
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Hash

import javax.annotation.Nonnull
import javax.inject.Inject

@ArtifactProviderFor(GriffonController)
class MessageFolderController {
    @MVCMember @Nonnull
    MessageFolderModel model
    @MVCMember @Nonnull
    MessageFolderView view
    @Inject @Nonnull GriffonApplication application

    @ControllerAction
    void messageReply() {
        int[] rows = view.selectedMessageHeaders()
        if (rows.length == 0)
            return
        MWMessageStatus status = model.messageHeaders.get(rows[0]) 
        MWMessage msg = status.message

        def params = [:]
        params.reply = msg
        params.core = model.core
        params.recipientsPOP = new HashSet<>(Collections.singletonList(status))
        mvcGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
    }

    @ControllerAction
    void messageReplyAll() {
        int[] rows = view.selectedMessageHeaders()
        if (rows.length == 0)
            return
        MWMessageStatus status = model.messageHeaders.get(rows[0]) 
        MWMessage msg = status.message

        Set<Persona> all = new HashSet<>()
        all.addAll(msg.recipients)

        def params = [:]
        params.reply = msg
        params.core = model.core
        params.recipients = all
        params.recipientsPOP = new HashSet(Collections.singletonList(status))
        mvcGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
    }

    @ControllerAction
    void viewProfileFromMessage() {
        int []rows = view.selectedMessageHeaders()
        if (rows.length != 1)
            return
        
        MWMessageStatus status = model.messageHeaders.get(rows[0])
        ViewProfileHelper.initViewProfileGroup(model.core, mvcGroup, status)
    }

    @ControllerAction
    void messageDelete() {
        int[] rows = view.selectedMessageHeaders()
        if (rows.length == 0)
            return
        List<MWMessage> toDelete = new ArrayList<>()
        for (int row : rows) {
            MWMessage msg = model.messageHeaders.get(row).message
            toDelete.add(msg)
        }
        toDelete.each {msg ->
            model.deleteMessage(msg)
            model.core.eventBus.publish(new UIMessageDeleteEvent(message: msg, folder: model.name))
        }
    }

    @ControllerAction
    void messageCompose() {
        def params = [:]
        params.recipients = new HashSet<>()
        params.core = model.core
        mvcGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
    }

    @ControllerAction
    void downloadAttachment() {
        List selected = view.selectedMessageAttachments()
        if (selected.isEmpty())
            return

        doDownloadAttachments(selected)
    }

    @ControllerAction
    void downloadAllAttachments() {
        doDownloadAttachments(model.messageAttachments)
    }

    private void doDownloadAttachments(List attachments) {
        int[] messageRows = view.selectedMessageHeaders()
        if (messageRows.length == 0)
            return

        MWMessage message = model.messageHeaders.get(messageRows[0]).message
        attachments.each {
            if (it instanceof MWMessageAttachment)
                model.core.eventBus.publish(new UIDownloadAttachmentEvent(attachment : it, sender : message.sender))
            else {
                def event = new UIDownloadCollectionEvent(
                        collection : it,
                        items : it.getFiles(),
                        host : message.sender,
                        infoHash : it.getInfoHash(),
                        full : true
                )
                model.core.eventBus.publish(event)
            }
        }

        application.mvcGroupManager.findGroup('MainFrame').view.showDownloadsWindow.call()
    }

    void markMessageRead(MWMessageStatus status) {
        if (status.status) {
            status.status = false
            model.core.eventBus.publish(new UIMessageReadEvent(message : status.message, folder: model.name))
        }
    }
}