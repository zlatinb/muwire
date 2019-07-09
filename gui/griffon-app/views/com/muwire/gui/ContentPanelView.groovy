package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

import com.muwire.core.content.RegexMatcher

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class ContentPanelView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ContentPanelModel model

    def dialog
    def mainFrame
    def mainPanel
    
    def rulesTable
    def ruleTextField
    def lastRulesSortEvent
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, "Content Control Panel", true)
        
        mainPanel = builder.panel {
            gridLayout(rows:2, cols:1)
            panel {
                borderLayout()
                scrollPane (constraints : BorderLayout.CENTER) {
                    rulesTable = table(id : "rules-table", autoCreateRowSorter : true) {
                        tableModel(list : model.rules) {
                            closureColumn(header: "Term", type:String, read: {row -> row.getTerm()})
                            closureColumn(header: "Regex?", type:Boolean, read: {row -> row instanceof RegexMatcher})
                            closureColumn(header: "Hits", type:Integer, read : {row -> row.matches.size()})
                        }
                    }
                }
                panel (constraints : BorderLayout.SOUTH) {
                    borderLayout()
                    ruleTextField = textField(constraints: BorderLayout.CENTER, action: addRuleAction)
                    panel (constraints: BorderLayout.EAST) {
                        buttonGroup(id : "ruleType")
                        radioButton(text: "Keyword", selected : true, buttonGroup: ruleType, keywordAction)
                        radioButton(text: "Regex", selected : false, buttonGroup: ruleType, regexAction)
                        button(text : "Add Rule", addRuleAction)
                        button(text : "Delete Rule", enabled : bind {model.deleteButtonEnabled}, deleteRuleAction)
                    }
                }
            }
            panel {
                // TODO: hits table
            }
        }
    }
    
    int getSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (lastRulesSortEvent != null) 
            selectedRow = rulesTable.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    void mvcGroupInit(Map<String,String> args) {
        
        rulesTable.rowSorter.addRowSorterListener({evt -> lastRulesSortEvent = evt})
        rulesTable.rowSorter.setSortsOnUpdates(true)
        def selectionModel = rulesTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = getSelectedRule()
            if (selectedRow < 0) {
                model.deleteButtonEnabled = false
                return
            } else {
                model.deleteButtonEnabled = true
                // TODO: populate hits table
            }
        })
        
        
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