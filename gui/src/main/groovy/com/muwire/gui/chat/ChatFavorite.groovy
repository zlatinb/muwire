package com.muwire.gui.chat

import com.muwire.core.Persona

class ChatFavorite {
    final Persona address
    boolean autoConnect
    
    ChatFavorite(Persona address, boolean  autoConnect) {
        this.address = address
        this.autoConnect = autoConnect
    }
}
