package com.muwire.gui

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode

import com.muwire.core.Core
import com.muwire.core.files.FileTree

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class AdvancedSharingModel {
    def watchedDirectories = []
    def treeRoot
    def negativeTree
    
    Core core
    
    void mvcGroupInit(Map<String,String> args) {
        watchedDirectories.addAll(core.watchedDirectoryManager.watchedDirs.values())
        
        treeRoot = new DefaultMutableTreeNode()
        negativeTree = new DefaultTreeModel(treeRoot)
        copyTree(treeRoot, core.fileManager.negativeTree.root)
    }
    
    private void copyTree(DefaultMutableTreeNode jtreeNode, FileTree.TreeNode fileTreeNode) {
        jtreeNode.setUserObject(fileTreeNode.file?.getName())
        fileTreeNode.children.each { 
            MutableTreeNode newChild = new DefaultMutableTreeNode()
            jtreeNode.add(newChild)
            copyTree(newChild, it)
        }
    }
    
}