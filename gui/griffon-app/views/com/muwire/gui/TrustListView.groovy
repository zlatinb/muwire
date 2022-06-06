package com.muwire.gui

import com.muwire.core.Persona
import com.muwire.gui.profile.PersonaOrProfile
import com.muwire.gui.profile.PersonaOrProfileCellRenderer
import com.muwire.gui.profile.PersonaOrProfileComparator
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.inject.Inject
import javax.swing.JFrame
import javax.swing.JTable
import java.awt.Dimension

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
    @Inject
    GriffonApplication application

    JFrame window
    JFrame mainFrame
    JTable contactsTable

    void initUI() {
        int rowHeight = application.context.get("row-height")
        mainFrame = application.windowManager.findWindow("main-frame")
        
        int dimX = Math.max(600, (int)(mainFrame.getWidth() / 2))
        int dimY = Math.max(500, (int)(mainFrame.getHeight() / 2))
        
        window = builder.frame(visible: false, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
                iconImage: builder.imageIcon("/MuWire-48x48.png").image,
                title: model.trustList.persona.getHumanReadableName()) {
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
                borderLayout()
                scrollPane (constraints : BorderLayout.CENTER){
                    contactsTable = table(id : "contacts-table", autoCreateRowSorter : true, rowHeight : rowHeight) {
                        tableModel(list : model.contacts) {
                            closureColumn(header: trans("CONTACTS"), preferredWidth: 200, type : PersonaOrProfile, read : {it})
                            closureColumn(header: trans("TRUST_STATUS"), preferredWidth: 20,  type: String, read: {
                                trans(it.level.name())
                            })
                            closureColumn(header: trans("REASON"), preferredWidth: 200,  type : String, read : {it.reason})
                            closureColumn(header: trans("YOUR_TRUST"), preferredWidth: 20,  type : String, read : {
                                Persona p = it.persona
                                trans(model.trustService.getLevel(p.destination).name())
                            })
                        }
                    }
                }
                panel (constraints : BorderLayout.SOUTH) {
                    gridBagLayout()
                    button(text : trans("VIEW_PROFILE"), constraints : gbc(gridx : 0, gridy : 0), viewProfileAction)
                    button(text : trans("CLOSE"), constraints : gbc(gridx : 1, gridy : 0), closeAction)
                }
            }
        }
        window.setPreferredSize([dimX, dimY] as Dimension)
    }

    void mvcGroupInit(Map<String,String> args) {
        
        def popRenderer = new PersonaOrProfileCellRenderer(application.context.get("ui-settings"))
        def popComparator = new PersonaOrProfileComparator()

        contactsTable.setDefaultRenderer(PersonaOrProfile.class, popRenderer)
        contactsTable.rowSorter.setComparator(0, popComparator)
        contactsTable.rowSorter.setSortsOnUpdates(true)
        contactsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        window.pack()
        window.setLocationRelativeTo(mainFrame)
        window.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                mvcGroup.destroy()
            }
        })
        window.setVisible(true)
    }

    int getSelectedRow() {
        int selectedRow = contactsTable.getSelectedRow()
        if (selectedRow < 0)
            return -1
        contactsTable.rowSorter.convertRowIndexToModel(selectedRow)
    }

    void updateTable() {
        contactsTable.model.fireTableDataChanged()
    }
}