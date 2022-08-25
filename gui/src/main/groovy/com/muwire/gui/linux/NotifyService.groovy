package com.muwire.gui.linux

import org.freedesktop.Notifications
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.UInt64

class NotifyService {
    
    private static final String BUS_NAME = "org.freedesktop.Notifications"
    private static final String BUS_PATH = "/org/freedesktop/Notifications"
    private static final DBusConnection CONNECTION
    
    static {
        CONNECTION = DBusConnectionBuilder.forSessionBus().build()
    }
    
    static void notify(String text) {
        println "notifying $text"
        Notifications notifications = CONNECTION.getRemoteObject(BUS_NAME, BUS_PATH, Notifications.class)
        println "capabilities"
        println notifications.GetCapabilities()
        
        UInt32 rv = notifications.Notify("MuWire", // app name 
                new UInt32(0L)), // replaces
                "", // no icon
                text, // summary
                text, // body
                Collections.emptyList(), // actions
                Collections.emptyMap(), // hints
                -1) // expire timeout
        
        println "rv is $rv"
    }
}
