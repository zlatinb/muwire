package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.search.UIResultEvent
import com.muwire.core.util.DataUtil
import net.i2p.data.DataHelper

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
         if (userObject instanceof ResultTreeNode) {
             String name = HTMLSanitizer.sanitize(userObject.toString())
             defaultRenderer.setText(name)
             defaultRenderer.setToolTipText(userObject.getToolTip())
             return defaultRenderer
         }

         UIResultEvent result = (UIResultEvent) userObject
         SizeFormatter.format(result.size, sb)
         sb.append(bShort)
         setText(HTMLSanitizer.sanitize("$result.name (${sb.toString()})"))
         setToolTipText(null)
         setEnabled(true)
         
         if (sharedPredicate.test(result.infohash))
             setIcon(sharedIcon)
         else if (result.comment)
             setIcon(commentIcon)
    
         this
    }
    
    public static class ResultTreeNode {
        private final String hiddenRoot, element
        private int files
        private long size
        ResultTreeNode(String hiddenRoot, String element) {
            this.hiddenRoot = hiddenRoot
            this.element = element
        }
        
        private String getToolTip() {
            files + " " + trans("FILES") + " " +
                    DataHelper.formatSize(size) + trans("BYTES_SHORT")
        }
        
        public String toString() {
            element
        }
        
        void addResult(UIResultEvent event) {
            files++
            size += event.size
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
