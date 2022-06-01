package com.muwire.gui.resultdetails

import com.muwire.core.Persona
import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.PersonaComparator
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaOrProfileCellRenderer
import com.muwire.gui.profile.PersonaOrProfileComparator
import griffon.core.GriffonApplication
import net.i2p.data.Destination

import javax.inject.Inject
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.border.Border
import java.awt.BorderLayout
import java.awt.Dimension
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
    @Inject
    GriffonApplication application
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ResultDetailsFrameModel model
    
    JFrame window

    JPanel certificatesPanel
    JPanel collectionsPanel
    
    def certificatesGroup, collectionsGroup
    
    def mainFrame
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        
        int frameHeight = 150
        window = builder.frame(visible : false, locationRelativeTo: mainFrame,
            defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
            iconImage: builder.imageIcon("/MuWire-48x48.png").image,
            title: trans("DETAILS_FOR", model.resultEvent.name)) {
            borderLayout()
            panel(constraints: BorderLayout.CENTER) {
                gridBagLayout()
                int gridy = 0
                panel(border: titledBorder(title: trans("BASIC_DETAILS"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                        constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100)) {
                    gridLayout(rows: 1, cols: 2)
                    panel {
                        label(text: trans("SIZE"))
                        label(text: String.valueOf(model.resultEvent.size))
                    }
                    panel {
                        label(text: trans("PIECE_SIZE"))
                        label(text: String.valueOf(0x1 << model.resultEvent.pieceSize))
                    }
                }
                if (model.resultEvent.comment != null) {
                    frameHeight += 200
                    panel(border: titledBorder(title: trans("COMMENT"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                            constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100, weighty: 100)) {
                        gridLayout(rows: 1, cols: 1)
                        scrollPane {
                            textArea(text: model.resultEvent.comment, editable: false, lineWrap: true, wrapStyleWord: true)
                        }
                    }
                }
                if (model.senders.size() > 1) {
                    frameHeight += 200
                    panel(border: titledBorder(title: trans("SENDERS"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                            constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100, weighty: 100)) {
                        gridLayout(rows: 1, cols: 1)
                        scrollPane {
                            table(id: "senders-table", autoCreateRowSorter: true, rowHeight: rowHeight) {
                                tableModel(list: model.senders.toList()) {
                                    closureColumn(header: trans("SENDER"), type: PersonaOrProfile, read: { it })
                                    closureColumn(header: trans("TRUST_NOUN"), read: { PersonaOrProfile pop ->
                                        Destination destination = pop.getPersona().destination
                                        trans(model.core.trustService.getLevel(destination).name())
                                    })
                                }
                            }
                        }
                    }
                }
                if (model.resultEvent.certificates > 0) {
                    frameHeight += 300
                    certificatesPanel = panel(border: titledBorder(title: trans("CERTIFICATES"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                            constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100, weighty: 100)) {
                        gridLayout(rows: 1, cols: 1)
                    }
                }
                if (model.resultEvent.collections.size() > 0) {
                    frameHeight += 300
                    collectionsPanel = panel(border: titledBorder(title: trans("COLLECTIONS"), border: etchedBorder(), titlePosition: TitledBorder.TOP),
                            constraints: gbc(gridx: 0, gridy: gridy++, fill: GridBagConstraints.BOTH, weightx: 100, weighty: 100)) {
                        gridLayout(rows: 1, cols: 1)
                    }
                }
            }
            panel(constraints: BorderLayout.SOUTH) {
                button(text : trans("CLOSE"), closeAction)
            }
        }
        
        window.setPreferredSize([800, frameHeight] as Dimension)
    }
    
    void mvcGroupInit(Map<String, String> args) {
        if (model.senders.size() > 1) {
            JTable sendersTable = builder.getVariable("senders-table")
            sendersTable.setDefaultRenderer(PersonaOrProfile.class, new PersonaOrProfileCellRenderer())
            sendersTable.rowSorter.setComparator(0, new PersonaOrProfileComparator())
        }
        if (certificatesPanel != null) {
            String mvcId = mvcGroup.mvcId + "_certificates"
            def params = [:]
            params.core = model.core
            params.resultEvent = model.resultEvent
            params.uuid = model.uuid
            certificatesGroup = mvcGroup.createMVCGroup("certificate-tab", mvcId, params)
            certificatesPanel.add(certificatesGroup.view.p, null)
        }
        if (collectionsPanel != null) {
            String mvcId = mvcGroup.mvcId + "_collections"
            def params = [:]
            params.core = model.core
            params.resultEvent = model.resultEvent
            params.uuid = model.uuid != null ? UUID.fromString(model.uuid) : null
            collectionsGroup = mvcGroup.createMVCGroup("mini-collection-tab", mvcId, params)
            collectionsPanel.add(collectionsGroup.view.p, null)
        }
        
        window.addWindowListener( new WindowAdapter() {
            void windowClosed(WindowEvent event) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setLocationRelativeTo(mainFrame)
        window.setVisible(true)
    }
    
    void mvcGroupDestroy() {
        certificatesGroup?.destroy()
        collectionsGroup?.destroy()
    }
}
