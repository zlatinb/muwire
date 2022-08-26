package com.muwire.gui.linux

import org.freedesktop.Notifications
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.errors.NoReply
import org.freedesktop.dbus.exceptions.DBusException
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.UInt64
import org.freedesktop.dbus.types.Variant

class DBUSNotifyService {
    
    public static final String SOUND_CHAT = "message-new-instant"
    public static final String SOUND_MESSAGE = "message-new-email"
    
    private static final String BUS_NAME = "org.freedesktop.Notifications"
    private static final String BUS_PATH = "/org/freedesktop/Notifications"
    private static final Notifications NOTIFICATIONS
    private static final boolean sound
    static {
        Notifications notifications = null
        try {
            notifications = DBusConnectionBuilder.forSessionBus().build().
                    getRemoteObject(BUS_NAME, BUS_PATH, Notifications.class)
            sound = notifications.GetCapabilities().contains("sound")
        } catch (DBusException | NoReply bad) {
            sound = false
        }
        NOTIFICATIONS = notifications
    }
    
    static void notify(String summary, String body, String soundName) {
        if (NOTIFICATIONS == null)
            return
        
        Map<String, Variant<?>> hints = new HashMap<>()
        if (sound) {
            Variant<String> variant = new Variant(soundName)
            hints.put("sound-name", variant)
        }
        UInt32 rv = NOTIFICATIONS.Notify("MuWire", // app name 
                new UInt32(0L), // replaces
                "", // no icon
                summary, // summary
                body, // body
                Collections.emptyList(), // actions
                hints, // hints
                -1) // expire timeout
    }
}
