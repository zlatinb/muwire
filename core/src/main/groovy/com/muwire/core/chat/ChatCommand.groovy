package com.muwire.core.chat

class ChatCommand {
    private final ChatAction action
    private final String payload
    
    ChatCommand(String source) {
        int space = source.indexOf(' ')
        if (space < 0)
            throw new Exception("Invalid command $source")
        String command = source.substring(0, space)
        if (command.charAt(0) != '/')
            throw new Exception("command doesn't start with / $source")
        command = command.substring(1)
        action = ChatAction.valueOf(command)
        payload = source.substring(space + 1)
    }
}
