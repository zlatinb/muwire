package com.muwire.gui

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode

import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollection

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import net.i2p.data.SigningPrivateKey
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class CollectionWizardModel {
    Set<SharedFile> uniqueFiles
    List<SharedFile> files
    SigningPrivateKey spk
    Persona me
    EventBus eventBus
    
    long timestamp
    @Observable String root
    @Observable String comment
    @Observable int numFiles
    @Observable long totalSize
    
    DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode()
    TreeModel tree = new DefaultTreeModel(treeRoot)
    FileCollection collection
    
    void mvcGroupInit(Map<String,String> args) {
        uniqueFiles = new HashSet<>(files)
        numFiles = uniqueFiles.size()
        uniqueFiles.each {
            totalSize += it.getCachedLength()
        }
    }
}