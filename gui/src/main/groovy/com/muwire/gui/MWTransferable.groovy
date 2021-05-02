package com.muwire.gui

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

class MWTransferable<T> implements Transferable {
    private final List<T> data
    MWTransferable(List<T> data) {
        this.data = data
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        CopyPasteSupport.FLAVORS
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        flavor == CopyPasteSupport.LIST_FLAVOR
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor != CopyPasteSupport.LIST_FLAVOR) {
            return null
        }
        return data
    }
}
