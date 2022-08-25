package org.freedesktop;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public interface Notifications extends DBusInterface {


    public UInt32 Notify(String arg_0, UInt32 arg_1, String arg_2, String arg_3, String arg_4, List<String> arg_5, Map<String, Variant<?>> arg_6, int arg_7);
    public void CloseNotification(UInt32 arg_0);
    public List<String> GetCapabilities();
    public GetServerInformationTuple GetServerInformation();


    public static class NotificationClosed extends DBusSignal {

        private final UInt32 arg_0;
        private final UInt32 arg_1;

        public NotificationClosed(String _path, UInt32 _arg_0, UInt32 _arg_1) throws DBusException {
            super(_path, _arg_0, _arg_1);
            this.arg_0 = _arg_0;
            this.arg_1 = _arg_1;
        }


        public UInt32 getArg_0() {
            return arg_0;
        }

        public UInt32 getArg_1() {
            return arg_1;
        }


    }

    public static class ActionInvoked extends DBusSignal {

        private final UInt32 arg_0;
        private final String arg_1;

        public ActionInvoked(String _path, UInt32 _arg_0, String _arg_1) throws DBusException {
            super(_path, _arg_0, _arg_1);
            this.arg_0 = _arg_0;
            this.arg_1 = _arg_1;
        }


        public UInt32 getArg_0() {
            return arg_0;
        }

        public String getArg_1() {
            return arg_1;
        }


    }
}