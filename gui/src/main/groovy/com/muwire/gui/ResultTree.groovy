package com.muwire.gui

import com.muwire.core.InfoHash
import com.muwire.core.search.UIResultEvent

import javax.swing.JTree
import javax.swing.ToolTipManager
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath
import java.util.function.Predicate

class ResultTree extends JTree{
    
    ResultTree(TreeModel model) {
        super(model)
        setRootVisible(false)
        setLargeModel(true)
        setExpandsSelectedPaths(true)
        ToolTipManager.sharedInstance().registerComponent(this)
    }
    
    void setSharedPredicate(Predicate<InfoHash> sharedPredicate, Predicate<InfoHash> downloadingPredicate) {
        setCellRenderer(new ResultTreeRenderer(sharedPredicate, downloadingPredicate))
    }
    
    List<ResultAndTargets> decorateResults(List<UIResultEvent> results) {
        List<ResultAndTargets> rv = new ArrayList<>()
        TreePath[] paths = getSelectionPaths()
        for (TreePath path : paths) {
            def node = path.getLastPathComponent()
            def userObject = node.getUserObject()

            if (userObject == ResultTreeRenderer.PLACEHOLDER) { // nothing
            } else if (userObject instanceof UIResultEvent) {
                // a leaf is selected
                if (results.contains(userObject))
                    rv << new ResultAndTargets(userObject, new File(userObject.name), null)
            } else {
                File parent = new File(userObject.toString())
                Set<TreePath> subPaths = new HashSet<>()
                TreeUtil.subPaths(path, subPaths)
                final int start = path.getPathCount() - 1
                for (TreePath subPath : subPaths) {
                    File target = new File("")
                    for (int i = start; i < subPath.getPathCount() - 1; i ++) {
                        def subNode = subPath.getPathComponent(i)
                        if (subNode.isLeaf())
                            target = new File(target, subNode.getUserObject().name)
                        else
                            target = new File(target, subNode.getUserObject().toString())
                    }
                    UIResultEvent event = subPath.getLastPathComponent().getUserObject()
                    if (results.contains(event))
                        rv << new ResultAndTargets(event, target, parent)
                }
            }
        }
        rv
    }

    /**
     * @return if and only if a single result is selected, return it.  Null otherwise.
     */
    UIResultEvent singleResultSelected() {
        TreePath[] selected = getSelectionPaths()
        if (selected == null || selected.length != 1)
            return null
        def obj = selected[0].getLastPathComponent().getUserObject()
        if (obj instanceof UIResultEvent)
            return obj
        return null
    }
   
}
