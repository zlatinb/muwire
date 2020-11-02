package com.muwire.gui

import griffon.core.artifact.GriffonController
import griffon.core.controller.ControllerAction
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.Base64

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

import javax.annotation.Nonnull
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode

import com.muwire.core.SharedFile
import com.muwire.core.collections.FileCollectionBuilder
import com.muwire.core.collections.PathTree
import com.muwire.core.collections.PathTree.Callback
import com.muwire.core.collections.UICollectionCreatedEvent

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
        if (model.root == null || model.root.trim().length() == 0) {
            view.warnMissingName()
            return
        }
        model.root = model.root.trim()
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
        
        TreeUtil.copy(model.treeRoot, model.collection.tree.root)    
        model.tree.nodeStructureChanged(model.treeRoot)
        
        view.switchToReview()
    }
    
    @ControllerAction
    void previous() {
        model.treeRoot.removeAllChildren()
        model.tree.nodeStructureChanged(model.treeRoot)
        view.switchToConfiguration()
    }
    
    @ControllerAction
    void copyHash() {
        String b64 = Base64.encode(model.collection.getInfoHash().getRoot())
        StringSelection selection = new StringSelection(b64)
        def clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
        clipboard.setContents(selection, null)
    }
    
    @ControllerAction
    void save() {
        model.eventBus.publish(new UICollectionCreatedEvent(collection : model.collection))
        cancel()
    }
}