package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.search.UIResultEvent
import com.muwire.core.util.DataUtil

import java.util.function.Predicate

import static com.muwire.gui.Translator.trans

import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import java.awt.Component

class ResultTreeRenderer extends DefaultTreeCellRenderer {
    private final ImageIcon commentIcon, sharedIcon
    private final String bShort
    private final StringBuffer sb = new StringBuffer()
    private final Predicate<InfoHash> sharedPredicate
    
    ResultTreeRenderer(Predicate<InfoHash> sharedPredicate) {
        commentIcon = new ImageIcon((URL) PathTreeRenderer.class.getResource("/comment.png"))
        sharedIcon = new ImageIcon((URL) PathTreeRenderer.class.getResource("/yes.png"))
        bShort = trans("BYTES_SHORT")
        this.sharedPredicate = sharedPredicate
    }

     Component getTreeCellRendererComponent(JTree tree, Object value, 
                                            boolean sel, boolean expanded, 
                                            boolean leaf, int row, boolean hasFocus) {
         def userObject = value.getUserObject()
    
         def defaultRenderer = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus) 
         if (userObject == null || userObject instanceof String || userObject instanceof ResultTreeNode) {
             userObject = HTMLSanitizer.sanitize(userObject.toString())
             defaultRenderer.setText(userObject)
             return defaultRenderer
         }

         UIResultEvent result = (UIResultEvent) userObject
         SizeFormatter.format(result.size, sb)
         sb.append(bShort)
         setText(HTMLSanitizer.sanitize("$result.name (${sb.toString()})"))
         setEnabled(true)
         
         if (sharedPredicate.test(result.infohash))
             setIcon(sharedIcon)
         else if (result.comment)
             setIcon(commentIcon)
    
         this
    }
    
    public static class ResultTreeNode {
        private final String hiddenRoot, element
        ResultTreeNode(String hiddenRoot, String element) {
            this.hiddenRoot = hiddenRoot
            this.element = element
        }
        
        public String toString() {
            element
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof ResultTreeNode))
                return false
            ResultTreeNode other = (ResultTreeNode)o
            hiddenRoot == other.hiddenRoot && 
                    element == other.element
        }
    }
}
