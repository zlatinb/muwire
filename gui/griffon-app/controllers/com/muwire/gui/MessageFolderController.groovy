package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.core.collections.UIDownloadCollectionEvent
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.MWMessageAttachment
import com.muwire.core.messenger.UIDownloadAttachmentEvent
import com.muwire.core.messenger.UIMessageDeleteEvent
import com.muwire.core.messenger.UIMessageReadEvent
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
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
        int row = view.selectedMessageHeader()
        if (row < 0)
            return
        MWMessage msg = model.messageHeaders.get(row).message

        def params = [:]
        params.reply = msg
        params.core = core
        params.recipients = new HashSet<>(Collections.singletonList(msg.sender))
        mvcGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
    }

    @ControllerAction
    void messageReplyAll() {
        int row = view.selectedMessageHeader()
        if (row < 0)
            return
        MWMessage msg = model.messageHeaders.get(row).message

        Set<Persona> all = new HashSet<>()
        all.add(msg.sender)
        all.addAll(msg.recipients)

        def params = [:]
        params.reply = msg
        params.core = core
        params.recipients = all
        mvcGroup.createMVCGroup("new-message", UUID.randomUUID().toString(), params)
    }

    @ControllerAction
    void messageDelete() {
        int row = view.selectedMessageHeader()
        if (row < 0)
            return
        MWMessage msg = model.messageHeaders.get(row).message
        model.deleteMessage(msg)
        model.core.eventBus.publish(new UIMessageDeleteEvent(message : msg, folder : model.folderIdx))
    }

    @ControllerAction
    void messageCompose() {
        def params = [:]
        params.recipients = new HashSet<>()
        params.core = core
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
        int messageRow = view.selectedMessageHeader()
        if (messageRow < 0)
            return

        MWMessage message = model.messageHeaders.get(messageRow).message
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

        application.mvcGroupManager.findGroup('main-frame').view.showDownloadsWindow.call()
    }

    void markMessageRead(MWMessageStatus status) {
        if (status.status) {
            status.status = false
            model.core.eventBus.publish(new UIMessageReadEvent(message : status.message))
        }
    }
}