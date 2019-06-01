package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

import java.awt.BorderLayout

import javax.annotation.Nonnull

@ArtifactProviderFor(GriffonView)
class SearchTabView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    SearchTabModel model

    def pane 
    def parent
    def searchTerms
    
    void initUI() {
        builder.with {
            def resultsTable
            def pane = scrollPane {
                resultsTable = table(id : "results-table") {
                    tableModel(list: model.results) {
                        closureColumn(header: "Name", type: String, read : {row -> row.name})
                        closureColumn(header: "Size", preferredWidth: 150, type: Long, read : {row -> row.size})
                        closureColumn(header: "Sender", type: String, read : {row -> row.sender.getHumanReadableName()})
                        closureColumn(header: "Trust", type: String, read : {row ->
                          model.core.trustService.getLevel(row.sender.destination)
                        })
                    }
                }
            }
            this.pane = pane
            this.pane.putClientProperty("mvc-group", mvcGroup)
            this.pane.putClientProperty("results-table",resultsTable)
            
            def selectionModel = resultsTable.getSelectionModel()
            selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
            selectionModel.addListSelectionListener( {
                mvcGroup.parentGroup.model.searchButtonsEnabled = true
            })
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        searchTerms = args["search-terms"]
        parent = mvcGroup.parentGroup.view.builder.getVariable("result-tabs")
        parent.addTab(searchTerms, pane)
        int index = parent.indexOfTab(searchTerms)
        
        def tabPanel
        builder.with { 
            tabPanel = panel {
                borderLayout()
                panel {
                    label(text : searchTerms, constraints : BorderLayout.CENTER)
                }
                button(icon : imageIcon("/close_tab.png"), preferredSize : [20,20], constraints : BorderLayout.EAST, // TODO: in osx is probably WEST
                    actionPerformed : closeTab )
            }
        }
        
        parent.setTabComponentAt(index, tabPanel)
    }
    
    def closeTab = {
        int index = parent.indexOfTab(searchTerms)
        parent.removeTabAt(index)
        mvcGroup.parentGroup.model.searchButtonsEnabled = false
        mvcGroup.destroy()
    }
}