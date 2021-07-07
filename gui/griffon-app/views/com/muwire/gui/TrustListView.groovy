package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import static com.muwire.gui.Translator.trans
import javax.swing.JDialog
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class TrustListView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    TrustListModel model

    def dialog
    def mainFrame
    def mainPanel

    def sortEvents = [:]

    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        dialog = new JDialog(mainFrame, model.trustList.persona.getHumanReadableName(), true)
        mainPanel = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.NORTH) {
                borderLayout()
                panel (constraints : BorderLayout.NORTH) {
                    label(text: trans("TRUST_LIST_OF",model.trustList.persona.getHumanReadableName()))
                }
                panel (constraints: BorderLayout.SOUTH) {
                    label(text : trans("LAST_UPDATED") + " "+ new Date(model.trustList.timestamp))
                }
            }
            panel(constraints : BorderLayout.CENTER) {
                gridLayout(rows : 1, cols : 2)
                panel {
                    borderLayout()
                    scrollPane (constraints : BorderLayout.CENTER){
                        table(id : "trusted-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                            tableModel(list : model.trusted) {
                                closureColumn(header: trans("TRUSTED_USERS"), type : String, read : {it.persona.getHumanReadableName()})
                                closureColumn(header: trans("REASON"), type : String, read : {HTMLSanitizer.sanitize(it.reason)})
                                closureColumn(header: trans("YOUR_TRUST"), type : String, read : {trans(model.trustService.getLevel(it.persona.destination).name())})
                            }
                        }
                    }
                    panel (constraints : BorderLayout.SOUTH) {
                        gridBagLayout()
                        button(text : trans("TRUST_VERB"), constraints : gbc(gridx : 0, gridy : 0), trustFromTrustedAction)
                        button(text : trans("DISTRUST"), constraints : gbc(gridx : 1, gridy : 0), distrustFromTrustedAction)
                    }
                }
                panel {
                    borderLayout()
                    scrollPane (constraints : BorderLayout.CENTER ){
                        table(id : "distrusted-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                            tableModel(list : model.distrusted) {
                                closureColumn(header: trans("DISTRUSTED_USERS"), type : String, read : {it.persona.getHumanReadableName()})
                                closureColumn(header: trans("REASON"), type:String, read : {HTMLSanitizer.sanitize(it.reason)})
                                closureColumn(header: trans("YOUR_TRUST"), type : String, read : {trans(model.trustService.getLevel(it.persona.destination).name())})
                            }
                        }
                    }
                    panel(constraints : BorderLayout.SOUTH) {
                        gridBagLayout()
                        button(text : trans("TRUST_VERB"), constraints : gbc(gridx : 0, gridy : 0), trustFromDistrustedAction)
                        button(text : trans("DISTRUST"), constraints : gbc(gridx : 1, gridy : 0), distrustFromDistrustedAction)
                    }
                }
            }
        }
    }

    void mvcGroupInit(Map<String,String> args) {

        def trustedTable = builder.getVariable("trusted-table")
        trustedTable.rowSorter.addRowSorterListener({evt -> sortEvents["trusted-table"] = evt})
        trustedTable.rowSorter.setSortsOnUpdates(true)
        trustedTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        def distrustedTable = builder.getVariable("distrusted-table")
        distrustedTable.rowSorter.addRowSorterListener({evt -> sortEvents["distrusted-table"] = evt})
        distrustedTable.rowSorter.setSortsOnUpdates(true)
        distrustedTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

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

    int getSelectedRow(String tableName) {
        def table = builder.getVariable(tableName)
        int selectedRow = table.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (sortEvents.get(tableName) != null)
            selectedRow = table.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }

    void fireUpdate(String tableName) {
        def table = builder.getVariable(tableName)
        table.model.fireTableDataChanged()
    }
}