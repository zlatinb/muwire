package com.muwire.gui

import java.awt.Component

import static com.muwire.gui.Translator.trans

import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer

import com.muwire.core.SharedFile

import net.i2p.data.DataHelper

class SharedTreeRenderer extends DefaultTreeCellRenderer {
    private final String bShort;
    private final ImageIcon commentIcon
    private final StringBuffer sb = new StringBuffer(32)
    
    SharedTreeRenderer() {
        commentIcon = new ImageIcon((URL) SharedTreeRenderer.class.getResource("/comment.png"))
        bShort = trans("BYTES_SHORT")
    }
    
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        
        def userObject = value.getUserObject()
        def defaultRenderer = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus) 
        if (userObject instanceof InterimTreeNode || userObject == null)
            return defaultRenderer
            
        
        SharedFile sf = (SharedFile) userObject
        String name = sf.getFile().getName()
        long length = sf.getCachedLength()
        SizeFormatter.format(length,sb)
        sb.append(bShort)
        
        setText("$name (${sb.toString()})")
        setEnabled(true)
        if (sf.comment != null) {
            setIcon(commentIcon)
        }

        this        
    }
}
