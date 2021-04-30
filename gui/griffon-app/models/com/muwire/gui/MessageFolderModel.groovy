package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.MessageLoadedEvent
import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonModel)
class MessageFolderModel {
    @MVCMember @Nonnull
    MessageFolderView view
    
    Core core
    boolean outgoing
    String name
    String txKey
    final Set<MWMessageStatus> messages = new LinkedHashSet<>()
    final List<MWMessageStatus> messageHeaders = new ArrayList<>()
    final List<Object> messageAttachments = new ArrayList<>()

    @Observable boolean messageButtonsEnabled
    @Observable boolean messageAttachmentsButtonEnabled
    @Observable String messageRecipientList

    void deleteMessage(MWMessage message) {
        MWMessageStatus status = new MWMessageStatus(message, false)
        messageHeaders.remove(status)
        view.messageHeaderTable.model.fireTableDataChanged()
        view.messageBody.setText("")
        view.messageSplitPane.setDividerLocation(1.0d)
    }
    
    void processMessageLoadedEvent(MessageLoadedEvent e) {
        def status = new MWMessageStatus(e.message, e.unread)
        if (messages.add(status)) {
            messageHeaders.add(status)
        }
        view.messageHeaderTable.model.fireTableDataChanged()
    }
}