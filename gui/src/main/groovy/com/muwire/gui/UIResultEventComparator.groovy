package com.muwire.gui

import com.muwire.core.search.UIResultEvent

import java.text.Collator

class UIResultEventComparator implements Comparator<UIResultEvent>{

    private final boolean fullPath
    UIResultEventComparator(boolean fullPath) {
        this.fullPath = fullPath
    }
    
    @Override
    int compare(UIResultEvent o1, UIResultEvent o2) {
        String s1,s2
        if (fullPath) {
            s1 = o1.getFullPath()
            s2 = o2.getFullPath()
        } else {
            s1 = o1.getName()
            s2 = o2.getName()
        }
        return Collator.getInstance().compare(s1, s2)
    }
}
