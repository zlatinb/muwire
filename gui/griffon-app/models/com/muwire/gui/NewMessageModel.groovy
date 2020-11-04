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
    
    String replyBody = ""
    
    Core core
    MWMessage reply
    List<Persona> recipients 
    List<?> attachments = new ArrayList<>()
    
    void mvcGroupInit(Map<String, String> args) {
        if (reply == null)
            return
        String text = reply.body
        String [] lines = text.split("\n")
        Stack<String> stack = new Stack<>()
        for (String line : lines) {
            line = line.trim()
            line = "> " + line
            stack.push(line)
        }
        
        StringBuilder sb = new StringBuilder()
        while(stack.size() > 0) {
            sb.append(stack.pop()).append('\n')
        }
        
        replyBody = sb.toString()
    }
}