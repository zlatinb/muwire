package com.muwire.gui

import java.awt.Color
import java.awt.Component
import java.util.function.Predicate

import static com.muwire.gui.Translator.trans

import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer

import com.muwire.core.SharedFile

class SharedTreeRenderer extends DefaultTreeCellRenderer {
    private final String bShort;
    private final ImageIcon commentIcon
    private final StringBuffer sb = new StringBuffer(32)
    private final Predicate<File> isDirSharedPredicate
    
    SharedTreeRenderer(Predicate<File> isDirSharedPredicate) {
        commentIcon = new ImageIcon((URL) SharedTreeRenderer.class.getResource("/comment.png"))
        bShort = trans("BYTES_SHORT")
        this.isDirSharedPredicate = isDirSharedPredicate
    }
    
    public Component getTreeCellRendererComponent(JTree tree, Object value,
        boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        
        def userObject = value.getUserObject()
        Component defaultRenderer = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
        if (userObject == null)
            return defaultRenderer
        if (userObject instanceof InterimTreeNode) {
            InterimTreeNode node = (InterimTreeNode) userObject
            boolean shared = isDirSharedPredicate.test(node.getFile())
            if (!shared) 
                defaultRenderer.setForeground(Color.GRAY)
            return defaultRenderer
        }
            
        
        SharedFile sf = (SharedFile) userObject
        String name = sf.getFile().getName()
        long length = sf.getCachedLength()
        SizeFormatter.format(length,sb)
        sb.append(bShort)
        
        setText(HTMLSanitizer.sanitize("$name (${sb.toString()})"))
        setEnabled(true)
        if (sf.comment != null) {
            setIcon(commentIcon)
        }

        this        
    }
}
