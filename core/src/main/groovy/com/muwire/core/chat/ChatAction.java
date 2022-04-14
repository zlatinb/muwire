package com.muwire.core.chat;

public enum ChatAction {
    JOIN(true, false, true, false), 
    LEAVE(false, false, true, false), 
    SAY(false, false, true, false), 
    LIST(true, true, true, false), 
    HELP(true, true, true, false),
    INFO(true, true, true, false),
    JOINED(true, true, false, false),
    PROFILE(true, false, false, false),
    TRUST(true, false, true, true),
    DISTRUST(true, false, true, true);
    
    final boolean console;
    final boolean stateless;
    final boolean user;
    final boolean local;
    ChatAction(boolean console, boolean stateless, boolean user, boolean local) {
        this.console = console;
        this.stateless = stateless;
        this.user = user;
        this.local = local;
    }
}
