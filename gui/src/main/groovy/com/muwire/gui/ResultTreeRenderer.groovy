package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.search.UIResultEvent
import com.muwire.core.util.DataUtil
import net.i2p.data.DataHelper

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import java.util.function.Predicate

import static com.muwire.gui.Translator.trans

import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import java.awt.Component

class ResultTreeRenderer extends DefaultTreeCellRenderer {
    
    public static final TreeNode PLACEHOLDER = new PlaceholderNode()
    
    private final ImageIcon commentIcon, sharedIcon, downloadingIcon
    private final String bShort
    private final StringBuffer sb = new StringBuffer()
    private final Predicate<InfoHash> sharedPredicate, downloadingPredicate
    
    ResultTreeRenderer(Predicate<InfoHash> sharedPredicate, Predicate<InfoHash> downloadingPredicate) {
        commentIcon = new ImageIcon((URL) PathTreeRenderer.class.getResource("/comment.png"))
        sharedIcon = new ImageIcon((URL) PathTreeRenderer.class.getResource("/yes.png"))
        downloadingIcon = new ImageIcon((URL) PathTreeRenderer.class.getResource("/down_arrow.png"))
        bShort = trans("BYTES_SHORT")
        this.sharedPredicate = sharedPredicate
        this.downloadingPredicate = downloadingPredicate
    }

     Component getTreeCellRendererComponent(JTree tree, Object value, 
                                            boolean sel, boolean expanded, 
                                            boolean leaf, int row, boolean hasFocus) {
         def userObject = value.getUserObject()
    
         def defaultRenderer = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)
         
         if (userObject == null)
             return defaultRenderer // TODO investigate
         
         if (userObject instanceof ResultTreeNode) {
             String name = HTMLSanitizer.sanitize(userObject.toString())
             defaultRenderer.setText(name)
             defaultRenderer.setToolTipText(userObject.getToolTip())
             return defaultRenderer
         } else if (userObject instanceof PlaceholderNode) {
             defaultRenderer.setText(userObject.toString())
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
         else if (downloadingPredicate.test(result.infohash))
             setIcon(downloadingIcon)
         else if (result.comment)
             setIcon(commentIcon)
    
         this
    }
    
    public static class ResultTreeNode {
        private final String hiddenRoot, element
        private final int hashCode
        private int files
        private long size
        ResultTreeNode(String hiddenRoot, String element) {
            this.hiddenRoot = hiddenRoot
            this.element = element
            this.hashCode = Objects.hash(hiddenRoot, element)
        }
        
        private String getToolTip() {
            if (files > 0) {
                return files + " " + trans("FILES") + " " +
                        DataHelper.formatSize(size) + trans("BYTES_SHORT")
            } else
                return trans("BROWSE_CLICK_TO_FETCH")
        }
        
        public String toString() {
            element
        }
        
        void addResult(UIResultEvent event) {
            files++
            size += event.size
        }
        
        public int hashCode() {
            hashCode
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof ResultTreeNode))
                return false
            ResultTreeNode other = (ResultTreeNode)o
            Objects.equals(hiddenRoot, other.hiddenRoot) &&
                    Objects.equals(element, other.element)
        }
    }
    
    static class PlaceholderNode {
        String toString() {
            trans("FETCHING")
        }
    }
}
