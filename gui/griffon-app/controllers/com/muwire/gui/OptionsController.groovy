package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonController)
class OptionsController {
    @MVCMember @Nonnull
    OptionsModel model
    @MVCMember @Nonnull
    OptionsView view

    @ControllerAction
    void save() {
        String text = view.retryField.text
        model.downloadRetryInterval = text
        
        def settings = application.context.get("muwire-settings")
        settings.downloadRetryInterval = Integer.valueOf(text)

        text = view.updateField.text
        model.updateCheckInterval = text
        settings.updateCheckInterval = Integer.valueOf(text)
                
        File settingsFile = new File(application.context.get("core").home, "MuWire.properties")
        settingsFile.withOutputStream { 
            settings.write(it)
        }
        
        cancel()
    }
    
    @ControllerAction
    void cancel() {
        view.d.setVisible(false)
        mvcGroup.destroy()
    }
}