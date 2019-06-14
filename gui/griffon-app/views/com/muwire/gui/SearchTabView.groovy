package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.core.mvc.MVCGroup
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.DataHelper

import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

import com.muwire.core.util.DataUtil

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
    def resultsTable
    def lastSortEvent
    
    void initUI() {
        builder.with {
            def resultsTable
            def pane = scrollPane {
                resultsTable = table(id : "results-table", autoCreateRowSorter : true) {
                    tableModel(list: model.results) {
                        closureColumn(header: "Name", preferredWidth: 350, type: String, read : {row -> row.name.replace('<','_')})
                        closureColumn(header: "Size", preferredWidth: 50, type: Long, read : {row -> row.size})
                        closureColumn(header: "Sources", preferredWidth: 10, type : Integer, read : { row -> model.hashBucket[row.infohash].size()})
                        closureColumn(header: "Sender", preferredWidth: 170, type: String, read : {row -> row.sender.getHumanReadableName()})
                        closureColumn(header: "Trust", preferredWidth: 50, type: String, read : {row ->
                          model.core.trustService.getLevel(row.sender.destination).toString()
                        })
                    }
                }
            }
            
            this.pane = pane
            this.pane.putClientProperty("mvc-group", mvcGroup)
            this.pane.putClientProperty("results-table",resultsTable)

            this.resultsTable = resultsTable
                        
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
        int index = parent.indexOfComponent(pane)
        parent.setSelectedIndex(index)
        
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
        
        def centerRenderer = new DefaultTableCellRenderer()
        centerRenderer.setHorizontalAlignment(JLabel.CENTER)
        resultsTable.columnModel.getColumn(1).setCellRenderer(centerRenderer)
        resultsTable.setDefaultRenderer(Integer.class,centerRenderer)
        resultsTable.columnModel.getColumn(4).setCellRenderer(centerRenderer)
        
        def sizeRenderer = new DefaultTableCellRenderer() {
            @Override
            JComponent getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
                Long l = (Long) value
                String formatted = DataHelper.formatSize2Decimal(l, false)+"B"
                return new JLabel(formatted)
            }
        }
        sizeRenderer.setHorizontalAlignment(JLabel.CENTER)
        resultsTable.columnModel.getColumn(1).setCellRenderer(sizeRenderer)
        
        
        resultsTable.rowSorter.addRowSorterListener({ evt -> lastSortEvent = evt})
    }
    
    def closeTab = {
        int index = parent.indexOfTab(searchTerms)
        parent.removeTabAt(index)
        mvcGroup.parentGroup.model.searchButtonsEnabled = false
        mvcGroup.destroy()
    }
}