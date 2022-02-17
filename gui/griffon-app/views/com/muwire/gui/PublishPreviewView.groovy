package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.JDialog
import javax.swing.JTable
import javax.swing.border.Border
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class PublishPreviewView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    PublishPreviewModel model
    
    def mainFrame
    JDialog dialog
    def p

    JTable toPublishTable, alreadyPublishedTable
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        String title = trans("PUBLISH_PREVIEW_TITLE")
        dialog = new JDialog(mainFrame, title, true)
        dialog.setResizable(true)
        
        int rows = 1
        if (model.toPublish.length > 0 && model.alreadyPublished.length > 0)
            rows = 2
        p = builder.panel(preferredSize: [800, rows * 300]) {
            borderLayout()
            panel(constraints: BorderLayout.CENTER) {
                gridLayout(rows: rows, cols: 1)
                if (model.toPublish.length > 0) {
                    panel(border: etchedBorder()) {
                        borderLayout()
                        panel(constraints: BorderLayout.NORTH) {
                            label(text: trans("PUBLISH_PREVIEW_TO_PUBLISH"))
                        }
                        scrollPane(constraints: BorderLayout.CENTER) {
                            toPublishTable = table(autoCreateRowSorter: true, rowHeight: rowHeight) {
                                tableModel(list: model.toPublish) {
                                    closureColumn(header: trans("NAME"), preferredWidth: 500, type: String, read: { HTMLSanitizer.sanitize(it.getCachedPath()) })
                                    closureColumn(header: trans("SIZE"), preferredWidth: 100, type: Long, read: { it.getCachedLength() })
                                }
                            }
                        }

                    }
                }
                if (model.alreadyPublished.length > 0) {
                    panel(border: etchedBorder()) {
                        borderLayout()
                        panel(constraints: BorderLayout.NORTH) {
                        label(text: trans("PUBLISH_PREVIEW_ALREADY_PUBLISHED"))
                    }
                    scrollPane(constraints: BorderLayout.CENTER) {
                        alreadyPublishedTable = table(autoCreateRowSorter : true, rowHeight: rowHeight) {
                            tableModel(list : model.alreadyPublished) {
                                closureColumn(header : trans("NAME"), preferredWidth: 500, type : String, read : { HTMLSanitizer.sanitize(it.getCachedPath())})
                                closureColumn(header : trans("SIZE"), preferredWidth: 100, type : Long, read : {it.getCachedLength() })
                                closureColumn(header: trans("DATE"), preferredWidth: 200, type: Long, read: { it.getPublishedTimestamp() })
                            }
                        }
                    }
                    }
                }
            }
            panel(constraints: BorderLayout.SOUTH) {
                button(text : trans("PUBLISH"), enabled : model.toPublish.length > 0, publishAction)
                button(text : trans("CANCEL"), cancelAction)
            }
        }
        
        if (toPublishTable != null)
            toPublishTable.columnModel.getColumn(1).setCellRenderer(new SizeRenderer())
        if (alreadyPublishedTable != null) {
            alreadyPublishedTable.columnModel.with {
                getColumn(1).setCellRenderer(new SizeRenderer())
                getColumn(2).setCellRenderer(new DateRenderer())
            }
        }
    }

    void mvcGroupInit(Map<String,String> args) {
        dialog.with {
            getContentPane().add(p)
            pack()
            setLocationRelativeTo(mainFrame)
            setDefaultCloseOperation(DISPOSE_ON_CLOSE)
            show()
        }
    }
}
