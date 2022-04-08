package com.muwire.gui.resultdetails

import com.muwire.core.Persona
import com.muwire.core.collections.FileCollection
import com.muwire.gui.DateRenderer
import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.PersonaComparator
import com.muwire.gui.SizeRenderer
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import java.awt.BorderLayout

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class MiniCollectionTabView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    MiniCollectionTabModel model

    JPanel p
    JTable collectionsTable
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        p = builder.panel {
            cardLayout()
            panel(constraints: "fetch-collections") {
                label(text: trans("SENDER_HAS_COLLECTIONS", model.resultEvent.collections.size()))
                button(text: trans("VIEW_COLLECTIONS"), fetchCollectionsAction)
            }
            panel(constraints: "view-collections") {
                borderLayout()
                panel(constraints: BorderLayout.NORTH) {
                    label(text: bind {trans(model.status.name())})
                    label(text: bind {model.fetched + "/" + model.count})
                }
                scrollPane(constraints: BorderLayout.CENTER) {
                    collectionsTable = table(autoCreateRowSorter: true, rowHeight: rowHeight) {
                        tableModel(list : model.collections) {
                            closureColumn(header: trans("NAME"), preferredWidth: 300, type : String, read : { HTMLSanitizer.sanitize(it.name)})
                            closureColumn(header: trans("AUTHOR"), preferredWidth: 200, type : Persona, read : {it.author})
                            closureColumn(header: trans("COLLECTION_TOTAL_FILES"), preferredWidth: 20, type: Integer, read : {it.numFiles()})
                            closureColumn(header: trans("COLLECTION_TOTAL_SIZE"), preferredWidth: 20, type: Long, read : {it.totalSize()})
                            closureColumn(header: trans("COMMENT"), preferredWidth: 20, type: Boolean, read: {it.comment != ""})
                            closureColumn(header: trans("CREATED"), preferredWidth: 50, type: Long, read: {it.timestamp})
                        }
                    }
                }
                panel(constraints: BorderLayout.SOUTH) {
                    button(text : trans("VIEW_COMMENT"), enabled : bind {model.viewCommentActionEnabled}, viewCommentAction)
                    button(text : trans("VIEW_COLLECTIONS"), enabled: bind {model.viewCollectionActionEnabled}, viewCollectionsAction)
                }
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        collectionsTable.setDefaultRenderer(Persona.class, new PersonaCellRenderer())
        collectionsTable.columnModel.getColumn(3).setCellRenderer(new SizeRenderer())
        collectionsTable.columnModel.getColumn(5).setCellRenderer(new DateRenderer())
        collectionsTable.rowSorter.setComparator(1, new PersonaComparator())
        collectionsTable.rowSorter.setSortsOnUpdates(true)
        def selectionModel = collectionsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            int [] rows = collectionsTable.getSelectedRows()
            model.viewCollectionActionEnabled = rows.length > 0
            model.viewCommentActionEnabled = false
            if (rows.length == 1) {
                rows[0] = collectionsTable.rowSorter.convertRowIndexToModel(rows[0])
                model.viewCommentActionEnabled = model.collections[rows[0]].comment != ""
            }
        })
    }
    
    void switchToTable() {
        p.getLayout().show(p, "view-collections")
    }
    
    void refresh() {
        int [] selectedRows = collectionsTable.getSelectedRows()
        for (int i = 0; i < selectedRows.length; i++)
            selectedRows[i] = collectionsTable.rowSorter.convertRowIndexToModel(selectedRows[i])
        collectionsTable.model.fireTableDataChanged()
        for (int i = 0; i < selectedRows.length; i++)
            selectedRows[i] = collectionsTable.rowSorter.convertRowIndexToView(selectedRows[i])
        for (int row : selectedRows)
            collectionsTable.addRowSelectionInterval(row, row)
    }

    List<FileCollection> selectedCollections() {
        int [] rows = collectionsTable.getSelectedRows()
        if (rows.length == 0)
            return Collections.emptyList()
        for (int i = 0;i < rows.length; i++) {
            rows[i] = collectionsTable.rowSorter.convertRowIndexToModel(rows[i])
        }
        List<FileCollection> rv = []
        for (int row : rows)
            rv << model.collections[row]
        rv
    }
}
