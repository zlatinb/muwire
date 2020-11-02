package com.muwire.gui

import griffon.core.artifact.GriffonView
import static com.muwire.gui.Translator.trans

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.DataHelper

import javax.swing.JDialog
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class CollectionWizardView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CollectionWizardModel model

    def dialog
    def mainFrame
    def mainPanel
    
    def nameTextField // TODO: disable "Review" button if empty
    def commentTextArea
    def filesTable
    JTree jTree
    
    void initUI() {
        
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        dialog = new JDialog(mainFrame, trans("COLLECTION_WIZARD"), true)

        mainPanel = builder.panel {
            cardLayout()
            panel(constraints : "configuration") {
                gridLayout(rows : 2, cols : 1)
                panel {
                    borderLayout()
                    panel(constraints : BorderLayout.NORTH) {
                        borderLayout()
                        label(text : trans("COLLECTION_NAME"), constraints : BorderLayout.WEST)
                        nameTextField = textField(constraints : BorderLayout.CENTER)
                    }
                    panel(constraints : BorderLayout.CENTER) {
                        borderLayout()
                        label(text: trans("COLLECTION_DESCRIPTION"), constraints : BorderLayout.NORTH)
                        scrollPane(constraints : BorderLayout.CENTER) {
                            commentTextArea = textArea(editable : true, columns : 100, lineWrap : true, wrapStyleWord : true)
                        }
                    }
                }
                panel {
                    borderLayout()
                    panel(constraints : BorderLayout.NORTH) {
                        label(text : trans("COLLECTION_TOTAL_FILES") + ":" + model.files.size())
                        label(text : trans("COLLECTION_TOTAL_SIZE") + ":" + DataHelper.formatSize2Decimal(model.totalSize(), false) + trans("BYTES_SHORT"))
                    }
                    scrollPane(constraints : BorderLayout.CENTER) {
                        filesTable = table(id : "files-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                            tableModel(list : model.files) {
                                closureColumn(header : trans("NAME"), type : String, read : {it.getCachedPath()})
                                closureColumn(header : trans("SIZE"), type : Long, preferredWidth: 30, read : {it.getCachedLength()})
                                closureColumn(header : trans("COMMENT"), type : Boolean, preferredWidth : 20, read : {it.getComment() != null})
                            }
                        }
                    }
                    panel(constraints : BorderLayout.SOUTH) {
                        button(text : trans("CANCEL"), cancelAction)
                        button(text : trans("REVIEW"), reviewAction)
                    }
                }
            }
            panel(constraints : "review") {
                borderLayout()
                label(text : trans("COLLECTION_REVIEW_TITLE"), constraints : BorderLayout.NORTH)
                scrollPane(constraints : BorderLayout.CENTER) {
                    jTree = new JTree(model.tree)
                    jTree.setCellRenderer(new PathTreeRenderer())
                    tree(id : "preview-tree", rowHeight : rowHeight, rootVisible : true, expandsSelectedPaths : true, jTree)
                }
                panel(constraints : BorderLayout.SOUTH) {
                    button(text : trans("CANCEL"), cancelAction)
                    button(text : trans("PREVIOUS"), previousAction)
                    button(text : trans("COPY_HASH_TO_CLIPBOARD"), copyHashAction)
                    button(text : trans("SAVE"), saveAction)
                }
            }
        }        
    }
    
    void switchToReview() {
        mainPanel.getLayout().show(mainPanel, "review")
        TreeUtil.expand(jTree)
    }
    
    void switchToConfiguration() {
        mainPanel.getLayout().show(mainPanel, "configuration")
    }
    
    void mvcGroupInit(Map<String,String> args) {
        filesTable.setDefaultRenderer(Long.class, new SizeRenderer())
        
        dialog.getContentPane().add(mainPanel)
        dialog.pack()
        dialog.setLocationRelativeTo(mainFrame)
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE)
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        dialog.show()
    }
}