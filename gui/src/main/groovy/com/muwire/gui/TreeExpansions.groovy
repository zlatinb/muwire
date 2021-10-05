package com.muwire.gui

import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener
import javax.swing.tree.TreePath

class TreeExpansions implements TreeExpansionListener {
    boolean manualExpansion
    final Set<TreePath> expandedPaths = new HashSet<>()


    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        manualExpansion = true
        expandedPaths.add(event.getPath())
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        manualExpansion = true
        expandedPaths.remove(event.getPath())
    }
}
