package com.muwire.gui

import com.muwire.core.search.UIResultEvent

class ResultAndTargets {
    final UIResultEvent resultEvent
    final File target, parent
    ResultAndTargets(UIResultEvent resultEvent, File target, File parent) {
        this.resultEvent = resultEvent
        this.target = target
        this.parent = parent
    }
}
