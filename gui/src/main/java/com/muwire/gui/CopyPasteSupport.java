package com.muwire.gui;

import java.awt.datatransfer.DataFlavor;

public class CopyPasteSupport {

    public static final DataFlavor LIST_FLAVOR;
    public static final DataFlavor[] FLAVORS = new DataFlavor[1];
    static {
        try {
            LIST_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList");
            FLAVORS[0] = LIST_FLAVOR;
        } catch (Exception impossible) {
            throw new RuntimeException(impossible);
        }
    }
}
