package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.table.Table
import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.filecert.Certificate
import com.muwire.core.filecert.UIImportCertificateEvent

class ViewCertificatesView extends BasicWindow {
    private final ViewCertificatesModel model
    private final TextGUI textGUI
    private final Core core
    private final Table table
    private final TerminalSize terminalSize
    private final LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER, true, false)
    
    ViewCertificatesView(ViewCertificatesModel model, TextGUI textGUI, Core core, TerminalSize terminalSize) {
        super("Certificates")
        this.model = model
        this.core = core
        this.textGUI = textGUI
        this.terminalSize = terminalSize
        
        setHints([Window.Hint.EXPANDED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(1))
        
        Label statusLabel = new Label("")
        Label percentageLabel = new Label("")
        model.setStatusLabel(statusLabel)
        model.setPercentageLabel(percentageLabel)
        
        Panel topPanel = new Panel()
        topPanel.setLayoutManager(new GridLayout(2))
        topPanel.addComponent(statusLabel, layoutData)
        topPanel.addComponent(percentageLabel, layoutData)
        contentPanel.addComponent(topPanel, layoutData)
        
        table = new Table("Issuer","File Name","Comment","Timestamp")
        table.with { 
            setCellSelection(false)
            setTableModel(model.model)
            setVisibleRows(terminalSize.getRows())
            setSelectAction({rowSelected()})
        }
        contentPanel.addComponent(table, layoutData)
        
        Button closeButton = new Button("Close",{
            model.unregister()
            close()
        })
        contentPanel.addComponent(closeButton, layoutData)
        setComponent(contentPanel)
    }
    
    private void rowSelected() {
        int selectedRow = table.getSelectedRow()
        def row = model.model.getRow(selectedRow)
        Certificate certificate = row[0].certificate
        
        Window prompt = new BasicWindow("Import Certificate?")
        prompt.setHints([Window.Hint.CENTERED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(3))
        Button importButton = new Button("Import", {importCert(certificate)})
        
        Button viewCommentButton = new Button("View Comment", {viewComment(certificate)})
        
        Button closeButton = new Button("Close", {prompt.close()})
        contentPanel.addComponent(importButton, layoutData)
        if (certificate.comment != null)
            contentPanel.addComponent(viewCommentButton, layoutData)
        contentPanel.addComponent(closeButton, layoutData)
        
        prompt.setComponent(contentPanel)
        importButton.takeFocus()
        textGUI.addWindowAndWait(prompt)
    }
    
    private void importCert(Certificate certificate) {
        core.eventBus.publish(new UIImportCertificateEvent(certificate : certificate))
        MessageDialog.showMessageDialog(textGUI, "Certificate(s) Imported", "", MessageDialogButton.OK)
    }
    
    private void viewComment(Certificate certificate) {
        ViewCommentView view = new ViewCommentView(certificate.comment.name, "Certificate Comment", terminalSize)
        textGUI.addWindowAndWait(view)
    }
}
