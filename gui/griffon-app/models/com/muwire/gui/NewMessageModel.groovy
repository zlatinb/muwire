package com.muwire.gui

import javax.annotation.Nonnull

import com.muwire.core.Core
import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.collections.FileCollection
import com.muwire.core.messenger.MWMessage
import com.muwire.core.messenger.MWMessageAttachment

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import net.i2p.data.SigningPrivateKey
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class NewMessageModel {
    @MVCMember @Nonnull
    NewMessageView view
    
    String replySubject = ""
    String replyBody = ""
    
    Core core
    MWMessage reply
    Set<Persona> recipients 
    List<?> attachments = new ArrayList<>()
    
    void mvcGroupInit(Map<String, String> args) {
        if (reply == null)
            return
        String text = reply.body
        String [] lines = text.split("\n")
        List<String> l = new ArrayList<>()
        for (String line : lines) {
            line = line.trim()
            line = "> " + line
            l.add(line)
        }
        
        StringBuilder sb = new StringBuilder()
        sb.append('\n')
        for (String line : l) {
            sb.append(line).append('\n')
        }
        
        replyBody = sb.toString()
        
        replySubject = "Re: " + reply.subject
        
    }
}