package com.muwire.core.chat

class ChatCommand {
    final ChatAction action
    final String payload
    final String source
    ChatCommand(String source) {
        if (source.charAt(0) != '/')
            throw new Exception("command doesn't start with / $source")
        
        int position = 1
        StringBuilder sb = new StringBuilder()
        while(position < source.length()) {
            char c = source.charAt(position)
            if (c == ' ')
                break
            sb.append(c)
            position++    
        }
        String command = sb.toString().toUpperCase()
        action = ChatAction.valueOf(command)
        if (position < source.length())
            payload = source.substring(position + 1)
        else
            payload = ""
        this.source = source
    }
}
