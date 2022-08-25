package com.muwire.gui.linux

import org.freedesktop.Notifications
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface

class NotifyService {
    
    private static final String BUS_NAME = "org.freedesktop.Notifications"
    private static final String BUS_PATH = "/org/freedesktop/Notifications"
    private static final DBusConnection CONNECTION
    
    static {
        CONNECTION = DBusConnectionBuilder.forSessionBus().build()
    }
    
    static void notify(String text) {
        Notifications notifications = CONNECTION.getRemoteObject(BUS_NAME, BUS_PATH, Notifications.class)
    }
}
