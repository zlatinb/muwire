package com.muwire.gui

import java.awt.Taskbar
import java.awt.Taskbar.Feature

class MessageNotificator {
    
    private final UISettings settings
    MessageNotificator(UISettings settings) {
        this.settings = settings
    }
    
    void newMessage() {
        if (!settings.messageNotifications)
            return
        if (!Taskbar.isTaskbarSupported())
            return
        def taskBar = Taskbar.getTaskbar()
        if (taskBar.isSupported(Feature.USER_ATTENTION))
            taskBar.requestUserAttention(true, false)
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
