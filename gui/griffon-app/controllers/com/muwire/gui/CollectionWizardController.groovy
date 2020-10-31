package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollectionBuilder
import com.muwire.core.collections.PathTree
import com.muwire.core.collections.PathTree.Callback

@ArtifactProviderFor(GriffonController)
class CollectionWizardController {
    @MVCMember @Nonnull
    CollectionWizardModel model
    @MVCMember @Nonnull
    CollectionWizardView view

    @ControllerAction
    void cancel() {
        view.dialog.setVisible(false)
        mvcGroup.destroy()
    }
    
    @ControllerAction
    void review() {
        model.timestamp = System.currentTimeMillis()
        model.root = view.nameTextField.text
        model.comment = view.commentTextArea.text
        if (model.comment == null)
            model.comment = ""
            
        def builder = new FileCollectionBuilder()
        builder.with {
            setAuthor(model.me)
            setComment(model.comment)
            setRoot(model.root)
            setTimestamp(model.timestamp)
            setSPK(model.spk)
            for (SharedFile sf : model.files)
                addFile(sf)
        }
        model.collection = builder.build()
        
        copyTree(model.treeRoot, model.collection.tree.root)    
        model.tree.nodeStructureChanged(model.treeRoot)
        
        view.switchToReview()
    }
    
    private static void copyTree(DefaultMutableTreeNode jtreeNode, PathTree.PathNode pathNode) {
        jtreeNode.setUserObject(pathNode.path)
        pathNode.children.each { 
            MutableTreeNode newChild = new DefaultMutableTreeNode()
            jtreeNode.add(newChild)
            copyTree(newChild, it)
        }
    }
    
    @ControllerAction
    void previous() {
        model.treeRoot.removeAllChildren()
        model.tree.nodeStructureChanged(model.treeRoot)
        view.switchToConfiguration()
    }
    
    @ControllerAction
    void save() {
        // TODO: emit event
        cancel()
    }
}