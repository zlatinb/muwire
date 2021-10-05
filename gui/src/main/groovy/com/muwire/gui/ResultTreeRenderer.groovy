package com.muwire.gui

import com.muwire.core.search.UIResultEvent
import com.muwire.core.util.DataUtil

import static com.muwire.gui.Translator.trans

import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import java.awt.Component

class ResultTreeRenderer extends DefaultTreeCellRenderer {
    private final ImageIcon commentIcon
    private final String bShort
    private final StringBuffer sb = new StringBuffer()
    
    ResultTreeRenderer() {
        commentIcon = new ImageIcon((URL) PathTreeRenderer.class.getResource("/comment.png"))
        bShort = trans("BYTES_SHORT")
    }

     Component getTreeCellRendererComponent(JTree tree, Object value, 
                                            boolean sel, boolean expanded, 
                                            boolean leaf, int row, boolean hasFocus) {
         def userObject = value.getUserObject()
    
         def defaultRenderer = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus) 
         if (userObject == null || userObject instanceof String) {
             userObject = HTMLSanitizer.sanitize(userObject)
             defaultRenderer.setText(userObject)
             return defaultRenderer
         }

         UIResultEvent result = (UIResultEvent) userObject
         SizeFormatter.format(result.size, sb)
         sb.append(bShort)
         setText(HTMLSanitizer.sanitize("$result.name (${sb.toString()})"))
         setEnabled(true)
         
         if (result.comment)
             setIcon(commentIcon)
    
         this
    }
}
