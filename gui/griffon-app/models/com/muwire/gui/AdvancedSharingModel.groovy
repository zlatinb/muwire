package com.muwire.gui

import javax.annotation.Nonnull
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode

import com.muwire.core.Core
import com.muwire.core.files.FileTree
import com.muwire.core.files.directories.WatchedDirectoryConfigurationEvent
import com.muwire.core.files.directories.WatchedDirectorySyncEvent

import griffon.core.artifact.GriffonModel
import griffon.inject.MVCMember
import griffon.transform.Observable
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class AdvancedSharingModel {
    
    @MVCMember @Nonnull
    AdvancedSharingView view
    
    def watchedDirectories = []
    def treeRoot
    def negativeTree
    
    Core core
    
    @Observable boolean syncActionEnabled
    @Observable boolean configureActionEnabled
    
    void mvcGroupInit(Map<String,String> args) {
        watchedDirectories.addAll(core.watchedDirectoryManager.watchedDirs.values())
        core.eventBus.register(WatchedDirectorySyncEvent.class, this)
        core.eventBus.register(WatchedDirectoryConfigurationEvent.class, this)
        
        treeRoot = new DefaultMutableTreeNode()
        negativeTree = new DefaultTreeModel(treeRoot)
        copyTree(treeRoot, core.fileManager.negativeTree.root)
    }
    
    void mvcGroupDestroy() {
        core.eventBus.unregister(WatchedDirectorySyncEvent.class, this)
        core.eventBus.unregister(WatchedDirectoryConfigurationEvent.class, this)
    }
    
    private void copyTree(DefaultMutableTreeNode jtreeNode, FileTree.TreeNode fileTreeNode) {
        jtreeNode.setUserObject(fileTreeNode.file?.getName())
        fileTreeNode.children.each { 
            MutableTreeNode newChild = new DefaultMutableTreeNode()
            jtreeNode.add(newChild)
            copyTree(newChild, it)
        }
    }
    
    void onWatchedDirectorySyncEvent(WatchedDirectorySyncEvent e) {
        runInsideUIAsync {
            view.watchedDirsTable.model.fireTableDataChanged()
        }
    }
    
    void onWatchedDirectoryConfigurationEvent(WatchedDirectoryConfigurationEvent e) {
        runInsideUIAsync {
            view.watchedDirsTable.model.fireTableDataChanged()
        }
    }
    
}