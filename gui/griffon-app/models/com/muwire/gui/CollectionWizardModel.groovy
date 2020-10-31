package com.muwire.gui

import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.tree.TreeNode

import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollection

import griffon.core.artifact.GriffonModel
import griffon.transform.Observable
import net.i2p.data.SigningPrivateKey
import griffon.metadata.ArtifactProviderFor

@ArtifactProviderFor(GriffonModel)
class CollectionWizardModel {
    List<SharedFile> files
    SigningPrivateKey spk
    Persona me
    
    long timestamp
    @Observable String root
    @Observable String comment
    
    DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode()
    TreeModel tree = new DefaultTreeModel(treeRoot)
    FileCollection collection
    
    long totalSize() {
        long rv = 0
        files.each { 
            rv += it.getCachedLength()
        }
        rv
    }   
}