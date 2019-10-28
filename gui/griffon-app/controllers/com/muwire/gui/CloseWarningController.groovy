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
        boolean rememberDecision = view.checkbox.model.isSelected()
        if (rememberDecision) {
            settings.exitOnClose = false
            settings.closeWarning = false
            saveMuSettings()
        }
        
        view.dialog.setVisible(false)
        view.mainFrame.setVisible(false)
        mvcGroup.destroy()
    }
    
    @ControllerAction
    void exit() {
        boolean rememberDecision = view.checkbox.model.isSelected()
        if (rememberDecision) {
            settings.exitOnClose = true
            settings.closeWarning = false
            saveMuSettings()
        }
        view.dialog.setVisible(false)
        view.mainFrame.setVisible(false)
        def parentView = mvcGroup.parentGroup.view
        mvcGroup.destroy()
        parentView.closeApplication()
    }
    
    private void saveMuSettings() {

        File props = new File(home, "gui.properties")
        props.withOutputStream {
            settings.write(it)
        }
    }
}