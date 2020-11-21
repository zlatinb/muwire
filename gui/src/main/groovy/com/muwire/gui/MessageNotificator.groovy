package com.muwire.gui

import static com.muwire.gui.Translator.trans
import java.awt.Taskbar
import java.awt.Taskbar.Feature
import java.awt.TrayIcon

class MessageNotificator {
    
    private final UISettings settings
    private final TrayIcon trayIcon
    MessageNotificator(UISettings settings, TrayIcon trayIcon) {
        this.settings = settings
        this.trayIcon = trayIcon
    }
    
    void newMessage(String from) {
        if (!settings.messageNotifications)
            return
        if (Taskbar.isTaskbarSupported()) {
            def taskBar = Taskbar.getTaskbar()
            if (taskBar.isSupported(Feature.USER_ATTENTION))
                taskBar.requestUserAttention(true, false)
        }
        if (trayIcon != null) {
            trayIcon.displayMessage(trans("NEW_MESSAGE"), trans("NEW_MESSAGE_FROM",from), TrayIcon.MessageType.INFO)
        }
    }
    
    void messages(int count) {
        if (!settings.messageNotifications)
            return
        if (!Taskbar.isTaskbarSupported())
            return
        def taskBar = Taskbar.getTaskbar()
        if(taskBar.isSupported(Feature.ICON_BADGE_NUMBER)) {
            if (count > 0)
                taskBar.setIconBadge(String.valueOf(count))
            else
                taskBar.setIconBadge("")
        }
    }
}
