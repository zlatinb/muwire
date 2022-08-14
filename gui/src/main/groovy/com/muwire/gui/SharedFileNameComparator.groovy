package com.muwire.gui

import com.muwire.core.SharedFile

import java.text.Collator

class SharedFileNameComparator implements Comparator<SharedFile>{

    @Override
    int compare(SharedFile o1, SharedFile o2) {
        String path1 = o1.getFile().getAbsolutePath()
        String path2 = o2.getFile().getAbsolutePath()
        return Collator.getInstance().compare(path1, path2)
    }
}
