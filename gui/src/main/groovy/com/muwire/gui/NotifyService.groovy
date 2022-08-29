package com.muwire.gui

import com.muwire.gui.linux.DBUSNotifyService
import net.i2p.util.SystemVersion

import java.awt.TrayIcon
import static com.muwire.gui.Translator.trans

class NotifyService {
    private final TrayIcon trayIcon
    
    NotifyService(TrayIcon trayIcon) {
        this.trayIcon = trayIcon
    }
    
    void notifyPrivateChat(String from, String server) {
        String caption = trans("NEW_PRIVATE_CHAT")
        String body = trans("NEW_PRIVATE_CHAT_DETAILS", from, server)
        if (!SystemVersion.isWindows() && !SystemVersion.isMac()) {
            DBUSNotifyService.notify(caption, body, DBUSNotifyService.SOUND_CHAT)
        } else {
            trayIcon.displayMessage(caption, body, TrayIcon.MessageType.INFO)
        }
    }
    
    void notifyChatMention(String room, String server) {
        String caption = trans("NEW_CHAT_MENTION")
        String body = trans("NEW_CHAT_MENTION_DETAILS", room , server)
        if (!SystemVersion.isWindows() && !SystemVersion.isMac()) {
            DBUSNotifyService.notify(caption, body, DBUSNotifyService.SOUND_CHAT)
        } else {
            trayIcon.displayMessage(caption, body, TrayIcon.MessageType.INFO)
        }
    }
    
    void notifyNewMessage(String from) {
        String caption = trans("NEW_MESSAGE")
        String body = trans("NEW_MESSAGE_FROM",from)
        if (!SystemVersion.isWindows() && !SystemVersion.isMac()) {
            DBUSNotifyService.notify(caption, body, DBUSNotifyService.SOUND_MESSAGE)
        } else {
            trayIcon.displayMessage(caption, body, TrayIcon.MessageType.INFO)
        }
    }
}
