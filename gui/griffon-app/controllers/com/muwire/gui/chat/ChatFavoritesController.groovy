package com.muwire.gui.chat

import com.muwire.core.Persona
import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import javax.annotation.Nonnull
import javax.swing.JOptionPane

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonController)
class ChatFavoritesController {
    @MVCMember @Nonnull
    ChatFavoritesModel model
    @MVCMember @Nonnull
    ChatFavoritesView view
    
    @ControllerAction
    void addAction() {
        String address = JOptionPane.showInputDialog(trans("COPY_PASTE_SERVER_ADDRESS"))
        if (address == null)
            return
        Persona p
        try {
            p = new Persona(new ByteArrayInputStream(Base64.decode(address)))
        } catch (Exception bad) {
            JOptionPane.showMessageDialog(null, trans("INVALID_SERVER_ADDRESS"), trans("INVALID_SERVER_ADDRESS"), JOptionPane.ERROR_MESSAGE)
            return
        }
        
        def newFavorite = new ChatFavorite(p, false)
        model.chatFavorites. favorites << newFavorite
        model.chatFavorites.save()
        view.refreshTable()
        
    }
    
    @ControllerAction
    void deleteAction() {
        List<ChatFavorite> selected = view.selectedFavorites()
        if (selected.isEmpty())
            return
        
        model.chatFavorites.favorites.removeAll(selected)
        model.chatFavorites.save()
        view.refreshTable()
    }
    
    @ControllerAction
    void connectAction() {
        List<ChatFavorite> selected = view.selectedFavorites()
        if (selected.isEmpty())
            return
        
        for (ChatFavorite cf : selected) {
            model.chatFavorites.connect(cf.address)
        }
    }
    
    @ControllerAction
    void closeAction() {
        view.window.setVisible(false)
        mvcGroup.destroy()
    }
}
