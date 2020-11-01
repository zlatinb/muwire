package com.muwire.gui

import java.awt.Component

import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer

import com.muwire.core.collections.FileCollectionItem

class PathTreeRenderer extends DefaultTreeCellRenderer {

    private final ImageIcon commentIcon
    
    public PathTreeRenderer() {
        commentIcon = new ImageIcon((URL) PathTreeRenderer.class.getResource("/comment.png"))
    }
    
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        def userObject = value.getUserObject()
        
        def defaultRenderer = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus) 
        if (userObject == null || userObject instanceof String) {
            return defaultRenderer
        }
        
        FileCollectionItem item = (FileCollectionItem) userObject
        String fileName = item.pathElements.get(item.pathElements.size() - 1)
        setText(fileName)
        setEnabled(true)
        if (item.comment != "" && item.comment != null) {
            setIcon(commentIcon)
        }
            
        this
    }
}
