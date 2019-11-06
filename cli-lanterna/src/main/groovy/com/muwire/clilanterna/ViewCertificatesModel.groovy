package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel
import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.filecert.Certificate
import com.muwire.core.filecert.CertificateFetchEvent
import com.muwire.core.filecert.CertificateFetchStatus
import com.muwire.core.filecert.CertificateFetchedEvent
import com.muwire.core.filecert.UIFetchCertificatesEvent
import com.muwire.core.search.UIResultEvent

class ViewCertificatesModel {
    private final UIResultEvent result
    private final Core core
    private final TextGUIThread guiThread
    
    private final TableModel model = new TableModel("Issuer","Trust Status","File Name","Comment","Timestamp")
    
    private int totalCerts
    
    private Label status
    private Label percentage
    
    ViewCertificatesModel(UIResultEvent result, Core core, TextGUIThread guiThread) {
        this.result = result
        this.core = core
        this.guiThread = guiThread
        
        core.eventBus.with { 
            register(CertificateFetchEvent.class,this)
            register(CertificateFetchedEvent.class, this)
            publish(new UIFetchCertificatesEvent(host : result.sender, infoHash : result.infohash))
        }
    }
    
    void unregister() {
        core.eventBus.unregister(CertificateFetchEvent.class, this)
        core.eventBus.unregister(CertificateFetchedEvent.class, this)
    }
    
    void onCertificateFetchEvent(CertificateFetchEvent e) {
        guiThread.invokeLater {
            status.setText(e.status.toString())
            if (e.status == CertificateFetchStatus.FETCHING)
                totalCerts = e.count
        }
    }
    
    void onCertificateFetchedEvent(CertificateFetchedEvent e) {
        guiThread.invokeLater {
            Date date = new Date(e.certificate.timestamp)
            model.addRow(new CertificateWrapper(e.certificate), core.trustService.getLevel(e.certificate.issuer.destination),
                e.certificate.name.name, e.certificate.comment != null, date)
            
            String percentageString = ""
            if (totalCerts > 0) {
                double percentage = Math.round((model.getRowCount() * 100 / totalCerts).toDouble())
                percentageString = String.valueOf(percentage) + "%"
            }
            percentage.setText(percentageString)
        }
    }
    
    void setStatusLabel(Label status) {
        this.status = status
    }
    
    void setPercentageLabel(Label percentage) {
        this.percentage = percentage
    }
}
