package org.freedesktop;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class GetServerInformationTuple extends Tuple {
    @Position(0)
    private String arg_0;
    @Position(1)
    private String arg_1;
    @Position(2)
    private String arg_2;
    @Position(3)
    private String arg_3;

    public GetServerInformationTuple(String arg_0, String arg_1, String arg_2, String arg_3) {
        this.arg_0 = arg_0;
        this.arg_1 = arg_1;
        this.arg_2 = arg_2;
        this.arg_3 = arg_3;
    }

    public void setArg_0(String arg) {
        arg_0 = arg;
    }

    public String getArg_0() {
        return arg_0;
    }
    public void setArg_1(String arg) {
        arg_1 = arg;
    }

    public String getArg_1() {
        return arg_1;
    }
    public void setArg_2(String arg) {
        arg_2 = arg;
    }

    public String getArg_2() {
        return arg_2;
    }
    public void setArg_3(String arg) {
        arg_3 = arg;
    }

    public String getArg_3() {
        return arg_3;
    }


}