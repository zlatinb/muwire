package com.muwire.gui.chat

import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.CellEditorListener
import javax.swing.event.ChangeEvent
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class ChatFavoritesView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    ChatFavoritesModel model

    JFrame window
    private JTable favoritesTable
    
    def mainFrame
    
    void initUI() {
        mainFrame = application.windowManager.findWindow("main-frame")
        int rowHeight = application.context.get("row-height")
        
        window = builder.frame(visible : false, locationRelativeTo: mainFrame,
            defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
            iconImage: builder.imageIcon("/MuWire-48x48.png").image,
            title: trans("CHAT_SERVERS_TITLE")) {
            borderLayout()
            scrollPane(constraints : BorderLayout.CENTER) {
                favoritesTable = table(autoCreateRowSorter : true, rowHeight : rowHeight) {
                    tableModel(list: model.chatFavorites.favorites) {
                        closureColumn(header: trans("SERVER"), type: String, 
                                read : {ChatFavorite cf -> cf.address.getHumanReadableName()})
                        closureColumn(header: trans("CHAT_SERVERS_STARTUP_CONNECT"), preferredWidth: 50, 
                                type: Boolean,
                                read : {ChatFavorite cf -> cf.autoConnect},
                                write: checkBoxEditor)
                    }
                }
            }
            panel (constraints: BorderLayout.SOUTH) {
                button(text : trans("CHAT_SERVERS_ADD"), toolTipText : trans("TOOLTIP_CHAT_SERVERS_ADD"), 
                        addAction)
                button(text : trans("CHAT_SERVERS_DELETE"), toolTipText: trans("TOOLTIP_CHAT_SERVERS_DELETE"),
                        deleteAction)
                button(text : trans("CHAT_SERVERS_CONNECT"), toolTipText: trans("TOOLTIP_CHAT_SERVERS_CONNECT"),
                        connectAction)
                button(text : trans("CLOSE"), closeAction)
            }
        }
    }
    
    void refreshTable() {
        favoritesTable.model.fireTableDataChanged()
    }
    
    List<ChatFavorite> selectedFavorites() {
        int [] rows = favoritesTable.getSelectedRows()
        if (rows.length == 0)
            return Collections.emptyList()
        
        for (int i = 0; i < rows.length; i++)
            rows[i] = favoritesTable.rowSorter.convertRowIndexToModel(rows[i])
        
        List<ChatFavorite> rv = []
        for (int i : rows)
            rv << model.chatFavorites.favorites[i]
        rv
    }
    
    void mvcGroupInit(Map<String, String> args) {
        
        favoritesTable.selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        
        window.addWindowListener( new WindowAdapter() {
            void windowClosed(WindowEvent event) {
                mvcGroup.destroy()
            }
        })
        window.pack()
        window.setLocationRelativeTo(mainFrame)
        window.setVisible(true)
    }
    
    def checkBoxEditor = {ChatFavorite cf, Boolean newValue ->
        cf.autoConnect = newValue    
        model.chatFavorites.save()
    }
}
