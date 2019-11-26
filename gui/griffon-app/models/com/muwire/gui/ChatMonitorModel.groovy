package com.muwire.gui

import javax.annotation.Nonnull

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class ChatMonitorModel implements ChatNotificator.Listener {
    @MVCMember @Nonnull
    ChatMonitorView view
    
    ChatNotificator chatNotificator
    def rooms = []
    
    void mvcGroupInit(Map<String,String> args) {
        chatNotificator.listener = this
    }
    
    void mvcGroupDestroy() {
        chatNotificator.listener = null
    }
    
    public void update() {
        rooms.clear()
        chatNotificator.roomsWithMessages.each { room, count ->
            int dash = room.indexOf('-')
            String server = room.substring(0, dash)
            String roomName = room.substring(dash + 1)
            rooms.add(new ChatRoomEntry(server, roomName, count))
        }
        view.updateView()
    }
    
    private static class ChatRoomEntry {
        private final String server, room
        private final int count
        ChatRoomEntry(String server, String room, int count) {
            this.server = server
            this.room = room
            this.count = count
        }
    }
}