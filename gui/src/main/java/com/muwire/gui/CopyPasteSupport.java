package com.muwire.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.InputStream;

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

    /**
     * @return what's in the clipboard if it can be represented as a string.  
     * This does not handle multi-byte characters unless copied from MuWire itself. 
     */
    public static String pasteFromClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        InputStream is;
        try {
            return (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch ( UnsupportedFlavorException|IOException notJavaString) {
            try {
                is = (InputStream) clipboard.getData(DataFlavor.getTextPlainUnicodeFlavor());
            } catch (UnsupportedFlavorException|IOException notPlainText) {
                return null;
            }
        }
        try {
            StringBuilder sb = new StringBuilder();
            int b;
            while ((b = is.read()) >= 0)
                sb.append((char)b);
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }
    
    public static boolean canPasteString() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        return clipboard.isDataFlavorAvailable(DataFlavor.getTextPlainUnicodeFlavor()) ||
                clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor);
                
    }
}
