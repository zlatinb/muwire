package com.muwire.gui.resultdetails

import net.i2p.data.Destination

import javax.swing.JPanel
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import static com.muwire.gui.Translator.trans

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.JFrame
import javax.swing.border.TitledBorder
import java.awt.GridBagConstraints

@ArtifactProviderFor(GriffonView)
class ResultDetailsFrameView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ResultDetailsFrameModel model
    
    def window

    JPanel certificatesPanel
    JPanel collectionsPanel
    
    def certificatesGroup, collectionsGroup
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        
        window = builder.frame(visible : false, locationRelativeTo: application.windowManager.findWindow("main-frame"),
            defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
            iconImage: builder.imageIcon("/MuWire-48x48.png").image,
            preferredSize: [800,800],
            title: trans("DETAILS_FOR", model.resultEvent.name)) {
            gridBagLayout()
            int gridy = 0
            panel(border: titledBorder(title : trans("BASIC_DETAILS"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100)) {
                gridBagLayout()
                label(text : trans("SIZE"), constraints: gbc(gridx: 0, gridy: 0))
                label(text : String.valueOf(model.resultEvent.size), constraints: gbc(gridx: 1, gridy: 0))
                label(text : trans("PIECE_SIZE"), constraints: gbc(gridx: 0, gridy : 1))
                label(text : String.valueOf(0x1 << model.resultEvent.pieceSize), constraints: gbc(gridx: 1, gridy: 1))
            }
            if (model.resultEvent.comment != null) {
                panel(border: titledBorder(title: trans("COMMENT"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                        constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100, weighty: 100)) {
                    gridLayout(rows: 1, cols: 1)
                    scrollPane {
                        textArea(text: model.resultEvent.comment, editable: false, lineWrap: true, wrapStyleWord: true)
                    }
                }
            }
            if (model.senders.size() > 1) {
                panel(border: titledBorder(title : trans("SENDERS"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                        constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100, weighty: 100)) {
                    gridLayout(rows: 1, cols: 1)
                    scrollPane {
                        table(autoCreateRowSorter : true, rowHeight: rowHeight) {
                            tableModel(list : model.senders.toList()) {
                                closureColumn(header : trans("SENDER"), read : {it.getHumanReadableName()})
                                closureColumn(header : trans("TRUST_NOUN"), read :{
                                    Destination destination = it.destination
                                    trans(model.core.trustService.getLevel(destination).name())
                                })
                            }
                        }
                    }
                }
            }
            if (model.resultEvent.certificates > 0) {
                certificatesPanel = panel(border: titledBorder(title : trans("CERTIFICATES"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                        constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100, weighty: 100)) {
                    gridLayout(rows: 1, cols: 1)
                }
            }
            if (model.resultEvent.collections.size() > 0) {
                collectionsPanel = panel(border: titledBorder(title : trans("COLLECTIONS"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                        constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100, weighty: 100)) {
                    gridLayout(rows: 1, cols: 1)
                }
            }
        }
    }
    
    void mvcGroupInit(Map<String, String> args) {
        if (certificatesPanel != null) {
            String mvcId = mvcGroup.mvcId + "_certificates"
            def params = [:]
            params.core = model.core
            params.resultEvent = model.resultEvent
            certificatesGroup = mvcGroup.createMVCGroup("certificate-tab", mvcId, params)
            certificatesPanel.add(certificatesGroup.view.p, null)
        }
        if (collectionsPanel != null) {
            String mvcId = mvcGroup.mvcId + "_collections"
            def params = [:]
            params.core = model.core
            params.resultEvent = model.resultEvent
            collectionsGroup = mvcGroup.createMVCGroup("mini-collection-tab", mvcId, params)
            collectionsPanel.add(collectionsGroup.view.p, null)
        }
        
        window.addWindowListener( new WindowAdapter() {
            void windowClosed(WindowEvent event) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setVisible(true)
    }
    
    void mvcGroupDestroy() {
        certificatesGroup?.destroy()
        collectionsGroup?.destroy()
    }
}
