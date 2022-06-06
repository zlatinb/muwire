package com.muwire.gui.resultdetails

import com.muwire.core.Persona
import com.muwire.core.filecert.Certificate
import com.muwire.gui.DateRenderer
import com.muwire.gui.HTMLSanitizer
import com.muwire.gui.PersonaCellRenderer
import com.muwire.gui.PersonaComparator
import griffon.core.GriffonApplication
import griffon.core.artifact.GriffonView
import griffon.inject.MVCMember
import griffon.metadata.ArtifactProviderFor

import javax.annotation.Nonnull
import javax.inject.Inject
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import java.awt.BorderLayout

import static com.muwire.gui.Translator.trans

@ArtifactProviderFor(GriffonView)
class CertificateTabView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder
    @MVCMember @Nonnull
    CertificateTabModel model
    @Inject
    GriffonApplication application

    JPanel p
    JTable certsTable
    
    void initUI() {
        int rowHeight = application.context.get("row-height")
        
        p = builder.panel {
            cardLayout()
            panel(constraints: "fetch-certificates") {
                label(text: trans("SENDER_HAS_CERTIFICATES", model.resultEvent.certificates))
                button(text: trans("VIEW_CERTIFICATES"), fetchCertificatesAction)
            }
            panel(constraints: "view-certificates") {
                borderLayout()
                panel(constraints: BorderLayout.NORTH) {
                    label(text: bind {trans(model.status.name())})
                    label(text: bind {model.fetched + "/" + model.count})
                }
                scrollPane(constraints: BorderLayout.CENTER) {
                    certsTable = table(autoCreateRowSorter: true, rowHeight: rowHeight) {
                        tableModel(list: model.certificates) {
                            closureColumn(header: trans("ISSUER"), preferredWidth: 150, type: Persona,
                                    read:{it.issuer})
                            closureColumn(header: trans("TRUST_STATUS"), preferredWidth: 30, type:String,
                                    read:{trans(model.core.trustService.getLevel(it.issuer.destination).name())})
                            closureColumn(header: trans("NAME"), preferredWidth: 450,
                                    read: { Certificate c -> HTMLSanitizer.sanitize(c.name.name) })
                            closureColumn(header: trans("ISSUED"), preferredWidth: 50, type: Long,
                                    read : {it.timestamp})
                            closureColumn(header: trans("COMMENTS"), preferredWidth: 20, type: Boolean,
                                    read: {it.comment != null})
                        }
                    }
                }
                panel(constraints: BorderLayout.SOUTH) {
                    button(text: trans("IMPORT"), enabled: bind {model.importActionEnabled}, importCertsAction)
                    button(text: trans("VIEW_COMMENT"), enabled: bind {model.viewCommentActionEnabled}, viewCommentAction)
                }
            }
        }
    }
    
    void mvcGroupInit(Map<String,String> args) {
        certsTable.setDefaultRenderer(Persona.class, new PersonaCellRenderer(application.context.get("ui-settings")))
        certsTable.setDefaultRenderer(Long.class, new DateRenderer())
        certsTable.rowSorter.setComparator(0, new PersonaComparator())
        certsTable.rowSorter.setSortsOnUpdates(true)

        def selectionModel = certsTable.getSelectionModel()
        selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        selectionModel.addListSelectionListener({
            int [] rows = certsTable.getSelectedRows()
            model.importActionEnabled = rows.length > 0
            model.viewCommentActionEnabled = false
            if (rows.length == 1) {
                rows[0] = certsTable.rowSorter.convertRowIndexToModel(rows[0])
                model.viewCommentActionEnabled = (model.certificates[rows[0]].comment != null)
            }
        })
    }

    List<Certificate> selectedCertificates() {
        int[] rows = certsTable.getSelectedRows()
        if (rows.length == 0)
            return Collections.emptyList()
        for (int i = 0; i < rows.length; i++) {
            rows[i] = certsTable.rowSorter.convertRowIndexToModel(rows[i])
        }
        List<Certificate> rv = []
        for (int i : rows)
            rv << model.certificates[i]
        rv
    }
    
    void switchToTable() {
        p.getLayout().show(p, "view-certificates")
    }
    
    void refresh() {
        int [] selectedRows = certsTable.getSelectedRows()
        for (int i = 0; i < selectedRows.length; i++)
            selectedRows[i] = certsTable.rowSorter.convertRowIndexToModel(selectedRows[i])
        certsTable.model.fireTableDataChanged()
        for (int i = 0; i < selectedRows.length; i++)
            selectedRows[i] = certsTable.rowSorter.convertRowIndexToView(selectedRows[i])
        for (int row : selectedRows)
            certsTable.addRowSelectionInterval(row, row)
    }
    
}
