package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull

import com.muwire.core.util.DataUtil

@ArtifactProviderFor(GriffonController)
class AddCommentController {
    @MVCMember @Nonnull
    AddCommentModel model
    @MVCMember @Nonnull
    AddCommentView view

    @ControllerAction
    void save() {
        String comment = view.textarea.getText()
        if (comment.trim().length() == 0)
            comment = null
        else
            comment = Base64.encode(DataUtil.encodei18nString(comment))
        model.selectedFiles.each {
            it.setComment(comment)
        }
        mvcGroup.parentGroup.view.builder.getVariable("shared-files-table").model.fireTableDataChanged()
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
}