package com.muwire.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

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
    
    public static void copyToClipboard(String str) {
        StringSelection selection = new StringSelection(str);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }
}
