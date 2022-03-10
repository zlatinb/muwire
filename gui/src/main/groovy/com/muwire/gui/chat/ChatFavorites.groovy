package com.muwire.gui.chat

import com.muwire.core.Persona
import griffon.core.GriffonApplication
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.i2p.data.Base64

class ChatFavorites {
    
    private File favoritesFile
    final List<ChatFavorite> favorites = []
    private def controller
    
    ChatFavorites(GriffonApplication application) {
        application.addPropertyChangeListener("core", {
            File home = it.getNewValue().home
            File chat = new File(home, "chat")
            chat.mkdirs()
            favoritesFile = new File(chat, "favorites.json")
            load()
            controller = application.mvcGroupManager.findGroup("MainFrame").getController()
            favorites.stream().filter({it.autoConnect}).forEach {
                controller.startChat(it.address)
            }
        })
    }
    
    private void load() {
        if (!favoritesFile.exists())
            return
        JsonSlurper slurper = new JsonSlurper()
        favoritesFile.eachLine {
            def json = slurper.parseText(it)
            byte [] personaBytes = Base64.decode(json.address)
            def address = new Persona(new ByteArrayInputStream(personaBytes))
            boolean autoConnect = json.autoConnect
            favorites << new ChatFavorite(address, autoConnect)
        }
    }
    
    void save() {
        favoritesFile.withPrintWriter {
            for (ChatFavorite favorite : favorites) {
                def json = [:]
                json.autoConnect = favorite.autoConnect
                json.address = favorite.address.toBase64()
                it.println(JsonOutput.toJson(json))
            }
        }
    }
    
    void connect(Persona persona) {
        controller.startChat(persona)
    }
}
