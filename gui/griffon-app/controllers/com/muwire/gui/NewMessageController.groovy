package com.muwire.gui

import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonController

import javax.inject.Inject

import static com.muwire.gui.Translator.trans

import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.Constants
import com.muwire.core.Persona
import com.muwire.core.collections.FileCollection
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.MWMessageAttachment
import com.muwire.core.messenger.UIMessageEvent

@ArtifactProviderFor(GriffonController)
class NewMessageController {
    @MVCMember @Nonnull
    NewMessageModel model
    @MVCMember @Nonnull
    NewMessageView view
    @Inject @Nonnull
    GriffonApplication application
    
    @ControllerAction
    void send() {
        String text = view.bodyArea.getText()
        if (text.length() > Constants.MAX_COMMENT_LENGTH) {
            JOptionPane.showMessageDialog(null, trans("MESSAGE_TOO_LONG_BODY", text.length(), Constants.MAX_COMMENT_LENGTH),
                trans("MESSAGE_TOO_LONG"), JOptionPane.WARNING_MESSAGE)
            return
        }
        
        text = view.subjectField.getText()
        if (text.length() > Constants.MAX_COMMENT_LENGTH) {
            JOptionPane.showMessageDialog(null, trans("SUBJECT_TOO_LONG_BODY", text.length(), Constants.MAX_COMMENT_LENGTH),
                trans("SUBJECT_TOO_LONG"), JOptionPane.WARNING_MESSAGE)
            return
        }
        
        Set<Persona> recipients = new HashSet<>()
        recipients.addAll(model.recipients)
        
        if (recipients.isEmpty()) {
            JOptionPane.showMessageDialog(null, trans("NO_RECIPIENTS_BODY"),
                trans("NO_RECIPIENTS"), JOptionPane.WARNING_MESSAGE)
            return
        }
        
        Set<MWMessageAttachment> attachments = new HashSet<>()
        Set<FileCollection> collections = new HashSet<>()
        model.attachments.each { 
            if (it instanceof FileCollection)
                collections.add(it)
            else
                attachments.add(it)
        }
        MWMessage message = new MWMessage(model.core.me, recipients, view.subjectField.text, 
            System.currentTimeMillis(), view.bodyArea.getText(), 
            attachments, collections,
            model.core.spk)
        model.core.eventBus.publish(new UIMessageEvent(message : message))
        
        application.mvcGroupManager.groups["MainFrame"].model.addToOutbox(message)
        
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}