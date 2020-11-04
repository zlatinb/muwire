package com.muwire.gui;

import java.awt.datatransfer.DataFlavor;

public class CopyPasteSupport {

    public static final DataFlavor LIST_FLAVOR;
    static {
        try {
            LIST_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList");
        } catch (Exception impossible) {
            throw new RuntimeException(impossible);
        }
    }
}
