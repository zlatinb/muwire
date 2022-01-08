package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.Constants
import com.muwire.core.Core
import com.muwire.core.files.UICommentEvent
import com.muwire.core.util.DataUtil

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonController)
class AddCommentController {
    @MVCMember @Nonnull
    AddCommentModel model
    @MVCMember @Nonnull
    AddCommentView view

    Core core
    
    @ControllerAction
    void save() {
        String comment = view.textarea.getText()
        if (comment.length() > Constants.MAX_COMMENT_LENGTH ) {
            JOptionPane.showMessageDialog(null, trans("ADD_COMMENT_TOO_LONG_BODY", comment.length(), Constants.MAX_COMMENT_LENGTH), 
                trans("ADD_COMMENT_TOO_LONG"), JOptionPane.WARNING_MESSAGE)
            return
        }
        if (comment.trim().length() == 0)
            comment = null
        else
            comment = Base64.encode(DataUtil.encodei18nString(comment))
        model.selectedFiles.each {
            def event = new UICommentEvent(sharedFile : it, oldComment : it.getComment())
            it.setComment(comment)
            core.eventBus.publish(event)    
        }
        mvcGroup.parentGroup.view.refreshSharedFiles()
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
    }
}