package com.muwire.gui

import java.awt.Image
import java.awt.Window

import static com.muwire.gui.Translator.trans
import java.awt.Taskbar
import java.awt.Taskbar.Feature
import java.awt.TrayIcon

class MessageNotificator {
    
    private final UISettings settings
    private final TrayIcon trayIcon
    private final Window window
    private final Image image
    MessageNotificator(UISettings settings, TrayIcon trayIcon, Window window, Image image) {
        this.settings = settings
        this.trayIcon = trayIcon
        this.window = window
        this.image = image
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
        } else if (taskBar.isSupported(Feature.ICON_BADGE_IMAGE_WINDOW)) {
            if (count == 0)
                taskBar.setWindowIconBadge(window, null)
            else
                taskBar.setWindowIconBadge(window, image)
        }
    }
}
