package com.muwire.gui

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.profile.MWProfileHeader
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.ThumbnailIcon
import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

import javax.swing.Icon

@ArtifactProviderFor(GriffonModel)
class ChatRoomModel {
    Core core
    Persona host
    String tabName
    String room
    boolean console
    boolean privateChat
    String roomTabName
    Map<Persona, MWProfileHeader> profileHeaders = new HashMap<>() 
    
    List<ChatPOP> members = []
    
    UISettings settings
    
    void mvcGroupInit(Map<String,String> args) {
        members.add(new ChatPOP(core.me))
        if (core.myProfile != null)
            profileHeaders.put(core.me, core.myProfile.getHeader())
        settings = application.context.get("ui-settings")
    }
    
    ChatPOP buildChatPOP(Persona persona) {
        new ChatPOP(persona)
    }
    
    class ChatPOP implements PersonaOrProfile {
        final Persona persona
        Icon icon
        
        ChatPOP(Persona persona) {
            this.persona = persona
        }
        
        public Persona getPersona() {
            return persona
        }
        
        public String getTitle() {
            MWProfileHeader header = profileHeaders.get(persona)
            if (header == null)
                return ""
            return HTMLSanitizer.sanitize(header.getTitle())
        }
        
        public Icon getThumbnail() {
            MWProfileHeader header = profileHeaders.get(persona)
            if (header == null)
                return null
            if (icon == null) {
                icon = new ThumbnailIcon(header.getThumbNail())
            }
            return icon
        }
        
        public int hashCode() {
            persona.hashCode()
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof ChatPOP))
                return false
            ChatPOP other = (ChatPOP)o
            persona == other.persona
        }
    }
}