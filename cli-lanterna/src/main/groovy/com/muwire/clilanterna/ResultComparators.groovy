package com.muwire.clilanterna

import com.muwire.core.search.UIResultEvent

class ResultComparators {
    public static final Comparator<UIResultEvent> NAME_ASC = new Comparator<UIResultEvent>() {
        public int compare(UIResultEvent a, UIResultEvent b) {
            a.name.compareTo(b.name)
        }
    }
    
    public static final Comparator<UIResultEvent> NAME_DESC = NAME_ASC.reversed()
    
    public static final Comparator<UIResultEvent> SIZE_ASC = new Comparator<UIResultEvent>() {
        public int compare(UIResultEvent a, UIResultEvent b) {
            Long.compare(a.size, b.size)
        }
    }
    
    public static final Comparator<UIResultEvent> SIZE_DESC = SIZE_ASC.reversed()
}
