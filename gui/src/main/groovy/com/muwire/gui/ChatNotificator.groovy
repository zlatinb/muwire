package com.muwire.gui

import java.awt.Image
import java.awt.Taskbar
import java.awt.Taskbar.Feature

import javax.swing.JPanel
import javax.swing.JTabbedPane

import griffon.core.mvc.MVCGroupManager

import java.awt.Window

class ChatNotificator {
    
    public static interface Listener {
        void update()
    }
    
    private final MVCGroupManager groupManager
    private final Window window
    private final Image image
    
    private boolean chatInFocus
    private String currentServerTab
    private String currentRoomTab
    
    private final Map<String, Integer> roomsWithMessages = new HashMap<>()
    
    private Listener listener
    
    ChatNotificator(MVCGroupManager groupManager, Window window, Image image) {
        this.groupManager = groupManager
        this.window = window
        this.image = image
    }
    
    void serverTabChanged(JTabbedPane source) {
        JPanel panel = source.getSelectedComponent()
        if (panel == null) {
            currentServerTab = null
            currentRoomTab = null
            return
        }
            
        String mvcId = panel.getClientProperty("mvcId")
        def group = groupManager.getGroups().get(mvcId)
        JTabbedPane childPane = panel.getClientProperty("childPane")
        JPanel roomPanel = childPane.getSelectedComponent()
        
        currentServerTab = mvcId
        currentRoomTab = childPane.getSelectedComponent()?.getClientProperty("mvcId")
        
        if (currentRoomTab != null) {
            roomsWithMessages.remove(currentRoomTab)
            updateBadge()
        }
    }
    
    void roomTabChanged(JTabbedPane source) {
        JPanel panel = source.getSelectedComponent()
        if (panel == null) {
            currentRoomTab = null
            return
        }
        currentRoomTab = panel.getClientProperty("mvcId")
        roomsWithMessages.remove(currentRoomTab)
        updateBadge()
    }
    
    void roomClosed(String mvcId) {
        roomsWithMessages.remove(mvcId)
        updateBadge()
    }
    
    void mainWindowDeactivated() {
        chatInFocus = false
    }
    
    void mainWindowActivated() {
        chatInFocus = true
        if (currentRoomTab != null)
            roomsWithMessages.remove(currentRoomTab)
        updateBadge()
    }
    
    void onMessage(String roomId) {
        if (roomId != currentRoomTab || !chatInFocus) {
            Integer previous = roomsWithMessages[roomId]
            if (previous == null)
                roomsWithMessages[roomId] = 1
             else
                roomsWithMessages[roomId] = previous + 1
        }
        updateBadge()
    }
    
    private void updateBadge() {
        listener?.update()
        if (!Taskbar.isTaskbarSupported())
            return

        int total = 0
        roomsWithMessages.values().each {
            total += it
        }

        def taskBar = Taskbar.getTaskbar()
        if (taskBar.isSupported(Feature.ICON_BADGE_NUMBER)) {
            if (total == 0)
                taskBar.setIconBadge("")
            else {
                taskBar.setIconBadge(String.valueOf(total))
            }
        } else if (taskBar.isSupported(Feature.ICON_BADGE_IMAGE_WINDOW)) {
            if (total > 0)
                taskBar.setWindowIconBadge(window, image)
            else
                taskBar.setWindowIconBadge(window, null)
        }
    }
}
