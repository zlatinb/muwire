package com.muwire.core.chat;

enum ChatAction {
    JOIN(true, false), 
    LEAVE(false, false), 
    SAY(false, false), 
    LIST(true, true), 
    HELP(true, true),
    INFO(true, true);
    
    final boolean console;
    final boolean stateless;
    ChatAction(boolean console, boolean stateless) {
        this.console = console;
        this.stateless = stateless;
    }
}
