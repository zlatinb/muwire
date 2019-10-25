package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.LayoutData
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.Window
import com.muwire.core.download.Downloader


class DownloadDetailsView extends BasicWindow {
    private final Downloader downloader
    private final LayoutData layoutData = GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER)
    
    private Label knownSources, activeSources, donePieces
    DownloadDetailsView(Downloader downloader) {
        super("Download details for "+downloader.file.getName())
        this.downloader = downloader
        
        setHints([Window.Hint.CENTERED])
        
        Panel contentPanel = new Panel()
        contentPanel.setLayoutManager(new GridLayout(2))
        
        knownSources = new Label("0")
        activeSources = new Label("0")
        donePieces = new Label("0")
        refresh()
        
        Button refreshButton = new Button("Refresh",{refresh()})
        Button closeButton = new Button("Close", {close()})
        
        contentPanel.with { 
            addComponent(new Label("Target Location"), layoutData)
            addComponent(new Label(downloader.file.getAbsolutePath()), layoutData)
            addComponent(new Label("Piece Size"), layoutData)
            addComponent(new Label(String.valueOf(downloader.pieceSize)), layoutData)
            addComponent(new Label("Total Pieces"), layoutData)
            addComponent(new Label(String.valueOf(downloader.nPieces)), layoutData)
            addComponent(new Label("Done Pieces"), layoutData)
            addComponent(donePieces, layoutData)
            addComponent(new Label("Known Sources"), layoutData)
            addComponent(knownSources, layoutData)
            addComponent(new Label("Active Sources"), layoutData)
            addComponent(activeSources, layoutData)
            addComponent(refreshButton, layoutData)
            addComponent(closeButton, layoutData)
        }
        
        setComponent(contentPanel)
        
    }
    
    private void refresh() {
        int done = downloader.donePieces()
        int known = downloader.activeWorkers.size()
        int active = downloader.activeWorkers()
        
        knownSources.setText(String.valueOf(known))
        activeSources.setText(String.valueOf(active))
        donePieces.setText(String.valueOf(done))
    }
}
