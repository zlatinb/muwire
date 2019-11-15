package com.muwire.gui

import java.awt.Taskbar
import java.awt.Taskbar.Feature

import javax.swing.JPanel
import javax.swing.JTabbedPane

import griffon.core.mvc.MVCGroupManager

class ChatNotificator {
    
    private final MVCGroupManager groupManager
    
    private boolean chatInFocus
    private String currentServerTab
    private String currentRoomTab
    
    private final Map<String, Integer> roomsWithMessages = new HashMap<>()
    
    ChatNotificator(MVCGroupManager groupManager) {
        this.groupManager = groupManager
    }
    
    void serverTabChanged(JTabbedPane source) {
        JPanel panel = source.getSelectedComponent()
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
        if (!Taskbar.isTaskbarSupported())
            return
        def taskBar = Taskbar.getTaskbar()
        if (!taskBar.isSupported(Feature.ICON_BADGE_NUMBER))
            return
        if (roomsWithMessages.isEmpty())
            taskBar.setIconBadge("")
        else {
            int total = 0
            roomsWithMessages.values().each { 
                total += it
            }
            taskBar.setIconBadge(String.valueOf(total))
        }
    }
}
