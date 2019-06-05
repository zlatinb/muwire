package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import javax.annotation.Nonnull

import com.muwire.core.Core

@ArtifactProviderFor(GriffonController)
class OptionsController {
    @MVCMember @Nonnull
    OptionsModel model
    @MVCMember @Nonnull
    OptionsView view

    @ControllerAction
    void save() {
        String text
        Core core = application.context.get("core")
        
        def i2pProps = core.i2pOptions
        
        text = view.inboundLengthField.text
        model.inboundLength = text
        i2pProps["inbound.length"] = text
        
        text = view.inboundQuantityField.text
        model.inboundQuantity = text
        i2pProps["inbound.quantity"] = text
        
        text = view.outboundQuantityField.text
        model.outboundQuantity = text
        i2pProps["outbound.quantity"] = text
        
        text = view.outboundLengthField.text
        model.outboundLength = text
        i2pProps["outbound.length"] = text
        
        File i2pSettingsFile = new File(core.home, "i2p.properties")
        i2pSettingsFile.withOutputStream { 
            i2pProps.store(it,"")
        }
                
        text = view.retryField.text
        model.downloadRetryInterval = text

        def settings = application.context.get("muwire-settings")
        settings.downloadRetryInterval = Integer.valueOf(text)

        text = view.updateField.text
        model.updateCheckInterval = text
        settings.updateCheckInterval = Integer.valueOf(text)

        boolean allowUntrusted = view.allowUntrustedCheckbox.model.isSelected()
        model.allowUntrusted = allowUntrusted
        settings.setAllowUntrusted(allowUntrusted)
                        
        File settingsFile = new File(core.home, "MuWire.properties")
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