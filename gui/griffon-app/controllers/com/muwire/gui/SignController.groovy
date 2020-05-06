package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.crypto.DSAEngine
import net.i2p.data.Base64

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.charset.StandardCharsets

import javax.annotation.Nonnull
import javax.swing.JOptionPane

import com.muwire.core.Constants
import com.muwire.core.Core
import com.muwire.core.util.DataUtil

@ArtifactProviderFor(GriffonController)
class SignController {
    
    Core core
    
    @MVCMember @Nonnull
    SignView view

    @ControllerAction
    void sign() {
        String plain = view.plainTextArea.getText()
        byte[] payload = plain.getBytes(StandardCharsets.UTF_8)
        def sig = DSAEngine.getInstance().sign(payload, core.spk)
        view.signedTextArea.setText(Base64.encode(sig.data))
    }
    
    @ControllerAction
    void copy() {
        String signed = view.signedTextArea.getText()
        StringSelection selection = new StringSelection(signed)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
    
    @ControllerAction
    void close() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
}