package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class CollectionWarningController {
    @MVCMember @Nonnull
    CollectionWarningModel model
    @MVCMember @Nonnull
    CollectionWarningView view

    @ControllerAction
    void unshare() {
        model.answer[0] = true
        
        if (view.rememberCheckbox.model.isSelected()) {
            model.settings.collectionWarning = false
            File propsFile = new File(model.home, "gui.properties")
            propsFile.withOutputStream { 
                model.settings.write(it)
            }
        }
        
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
}