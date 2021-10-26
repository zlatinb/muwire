package com.muwire.gui

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor
import net.i2p.data.DataHelper

import javax.annotation.Nonnull
import java.awt.BorderLayout
import java.awt.GridBagConstraints

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class ResultDetailsView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ResultDetailsModel model
    
    
    def parent
    def p
    
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        p = builder.panel {
            borderLayout()
            panel(constraints: BorderLayout.NORTH) {
                label(text : HTMLSanitizer.sanitize(model.fileName))
                label(text : DataHelper.formatSize2(model.results[0].size, false) + trans("BYTES_SHORT"))
            }
            panel(constraints: BorderLayout.CENTER) {
                gridBagLayout()
                int gridy = 0
                if (!model.localFiles.isEmpty()) {
                    panel (border: etchedBorder(), constraints: gbc(gridx: 0, gridy: gridy++, weightx: 100, fill: GridBagConstraints.HORIZONTAL)) {
                        borderLayout()
                        label(text: trans("YOU_ALREADY_HAVE_FILE", model.localFiles.size()),
                            constraints: BorderLayout.NORTH)
                        scrollPane(constraints: BorderLayout.CENTER) {
                            list(items : model.localFiles.collect {it.getCachedPath()})
                        }
                    }
                }
                panel(border: etchedBorder(), constraints: gbc(gridx: 0, gridy: gridy++, weightx: 100, fill: GridBagConstraints.HORIZONTAL)) {
                    borderLayout()
                    label(text: trans("RESULTS_CAME_FROM"), constraints: BorderLayout.NORTH)
                    scrollPane(constraints: BorderLayout.CENTER) {
                        table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                            tableModel(list: model.results) {
                                closureColumn(header: trans("SENDER"), preferredWidth: 200, read : {it.sender.getHumanReadableName()})
                                closureColumn(header: trans("NAME"), read : {HTMLSanitizer.sanitize(it.getFullPath())})
                            }
                        }
                    }
                }
                
                // fill up bottom space
                panel(constraints: gbc(gridx: 0, gridy: gridy++, weighty: 100))
            }
        }
        
    }
    
    void mvcGroupInit(Map<String,String> args) {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.resultDetails.add(model.key)
        parent = mainFrameGroup.view.builder.getVariable("result-tabs")
        parent.addTab(model.key, p)
        
        int index = parent.indexOfComponent(p)
        parent.setSelectedIndex(index)
        
        def tabPanel = builder.panel {
            borderLayout()
            panel(constraints : BorderLayout.CENTER) {
                label(text : model.fileName)
            }
            button(icon: imageIcon("/close_tab.png"), preferredSize: [20,20], constraints: BorderLayout.EAST,
                    actionPerformed: closeTab)
        }
        
        parent.setTabComponentAt(index, tabPanel)
        mainFrameGroup.view.showSearchWindow.call()
        
        // TODO: other stuff.
    }
    
    def closeTab = {
        def mainFrameGroup = application.mvcGroupManager.findGroup("MainFrame")
        mainFrameGroup.model.resultDetails.remove(model.key)
        
        int index = parent.indexOfTab(model.key)
        parent.removeTabAt(index)
        mvcGroup.destroy()
    }
}
