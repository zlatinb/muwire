package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class CloseWarningController {
    @MVCMember @Nonnull
    CloseWarningModel model
    @MVCMember @Nonnull
    CloseWarningView view

    UISettings settings
    File home
    

    void mvcGroupInit(Map<String, String> args) {
        model.closeWarning = settings.closeWarning
    }    
    
    @ControllerAction
    void close() {
        boolean showWarning = !view.checkbox.model.isSelected()
        model.closeWarning = showWarning
        settings.closeWarning = showWarning
        
        File props = new File(home, "gui.properties")
        props.withOutputStream { 
            settings.write(it)
        }
        
        view.dialog.setVisible(false)
        view.mainFrame.setVisible(false)
        mvcGroup.destroy()
    }
}