package com.muwire.core.chat;

enum ChatAction {
    JOIN(true, false, true), 
    LEAVE(false, false, true), 
    SAY(false, false, true), 
    LIST(true, true, true), 
    HELP(true, true, true),
    INFO(true, true, true),
    JOINED(true, true, false);
    
    final boolean console;
    final boolean stateless;
    final boolean user;
    ChatAction(boolean console, boolean stateless, boolean user) {
        this.console = console;
        this.stateless = stateless;
        this.user = user;
    }
}
