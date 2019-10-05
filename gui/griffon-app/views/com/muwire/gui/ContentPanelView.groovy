package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.content.Matcher
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
    def hitsTable
    def lastHitsSortEvent
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        dialog = new JDialog(mainFrame, "Content Control Panel", true)
        
        mainPanel = builder.panel {
            gridLayout(rows:1, cols:2)
            panel {
                borderLayout()
                panel (constraints : BorderLayout.NORTH) {
                    label(text : "Rules")
                }
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
            panel (border : etchedBorder()){
                borderLayout()
                panel (constraints : BorderLayout.NORTH) {
                    label(text : "Hits")
                }
                scrollPane(constraints : BorderLayout.CENTER) {
                     hitsTable = table(id : "hits-table", autoCreateRowSorter : true) {
                         tableModel(list : model.hits) {
                             closureColumn(header : "Searcher", type : String, read : {row -> row.persona.getHumanReadableName()})
                             closureColumn(header : "Keywords", type : String, read : {row -> row.keywords.join(" ")})
                             closureColumn(header : "Date", type : String, read : {row -> String.valueOf(new Date(row.timestamp))})
                         }
                     }   
                }
                panel (constraints : BorderLayout.SOUTH) {
                    button(text : "Refresh", refreshAction)
                    button(text : "Clear Hits", clearHitsAction)
                    button(text : "Trust", enabled : bind {model.trustButtonsEnabled}, trustAction)
                    button(text : "Distrust", enabled : bind {model.trustButtonsEnabled}, distrustAction)
                }
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
    
    int getSelectedHit() {
        int selectedRow = hitsTable.getSelectedRow()
        if (selectedRow < 0)
            return -1
        if (lastHitsSortEvent != null) 
            selectedRow = hitsTable.rowSorter.convertRowIndexToModel(selectedRow)
        selectedRow
    }
    
    void mvcGroupInit(Map<String,String> args) {
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        rulesTable.setDefaultRenderer(Integer.class, centerRenderer)
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
                model.hits.clear()
                Matcher matcher = model.rules[selectedRow]
                model.hits.addAll(matcher.matches)
                hitsTable.model.fireTableDataChanged()
            }
        })
        
        hitsTable.rowSorter.addRowSorterListener({evt -> lastHitsSortEvent = evt})
        hitsTable.rowSorter.setSortsOnUpdates(true)
        selectionModel = hitsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectionModel.addListSelectionListener({
            int selectedRow = getSelectedHit()
            model.trustButtonsEnabled = selectedRow >= 0
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