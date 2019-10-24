package com.muwire.clilanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.BorderLayout
import com.googlecode.lanterna.gui2.Button
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.GridLayout.Alignment
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.Panels
import com.googlecode.lanterna.gui2.TextGUI
import com.googlecode.lanterna.gui2.Window
import com.googlecode.lanterna.gui2.TextBox
import com.muwire.core.Core

class MainWindowView extends BasicWindow {
    
    private final Core core
    private final TextGUI textGUI

    private final Label connectionCount
    private final TextBox searchTextBox
    
    private final DownloadsModel downloadsModel
    private final UploadsModel uploadsModel
    private final FilesModel filesModel
    
    public MainWindowView(String title, Core core, TextGUI textGUI) {
        super(title);
        
        this.core = core
        this.textGUI = textGUI
        
        downloadsModel = new DownloadsModel(textGUI.getGUIThread(),core)
        uploadsModel = new UploadsModel(textGUI.getGUIThread(), core)
        filesModel = new FilesModel(textGUI.getGUIThread(),core)
        
        setHints([Window.Hint.EXPANDED])
        Panel contentPanel = new Panel()
        setComponent(contentPanel)
        
        BorderLayout borderLayout = new BorderLayout()
        contentPanel.setLayoutManager(borderLayout)
        
        Panel buttonsPanel = new Panel()
        contentPanel.addComponent(buttonsPanel, BorderLayout.Location.TOP)
        
        GridLayout gridLayout = new GridLayout(7)
        buttonsPanel.setLayoutManager(gridLayout)
        
        searchTextBox = new TextBox(new TerminalSize(40, 1))
        Button searchButton = new Button("Search", { search() })
        Button downloadsButton = new Button("Downloads", {download()})
        Button uploadsButton = new Button("Uploads", {upload()})
        Button filesButton = new Button("Files", { files() })
        Button trustButton = new Button("Trust", {println "trust"})
        Button quitButton = new Button("Quit", {close()})
        
        buttonsPanel.addComponent(searchTextBox, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        buttonsPanel.addComponent(searchButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        buttonsPanel.addComponent(downloadsButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        buttonsPanel.addComponent(uploadsButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        buttonsPanel.addComponent(filesButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        buttonsPanel.addComponent(trustButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        buttonsPanel.addComponent(quitButton, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        Panel bottomPanel = new Panel()
        contentPanel.addComponent(bottomPanel, BorderLayout.Location.BOTTOM)
        BorderLayout bottomLayout = new BorderLayout()
        bottomPanel.setLayoutManager(bottomLayout)
        
        Label persona = new Label(core.me.getHumanReadableName())
        bottomPanel.addComponent(persona, BorderLayout.Location.LEFT)
        
        
        Panel connectionsPanel = new Panel()
        connectionsPanel.setLayoutManager(new GridLayout(2))
        Label connections = new Label("Connections:")
        connectionsPanel.addComponent(connections, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        connectionCount = new Label("0")
        connectionsPanel.addComponent(connectionCount, GridLayout.createLayoutData(Alignment.CENTER, Alignment.CENTER))
        
        bottomPanel.addComponent(connectionsPanel, BorderLayout.Location.RIGHT)
            
        Button refreshButton = new Button("Refresh", {refresh()})
        bottomPanel.addComponent(refreshButton, BorderLayout.Location.CENTER)
        refreshButton.takeFocus()
    }
    
    private void refresh() {
        connectionCount.setText(String.valueOf(core.connectionManager.connections.size()))
    }
    
    private void search() {
        String query = searchTextBox.getText()
        SearchModel model = new SearchModel(query, core)
        textGUI.addWindowAndWait(new SearchView(model,core, textGUI))
    }
    
    private void download() {
        textGUI.addWindowAndWait(new DownloadsView(core, downloadsModel, textGUI))
    }
    
    private void upload() {
        textGUI.addWindowAndWait(new UploadsView(uploadsModel))
    }
    
    private void files() {
        textGUI.addWindowAndWait(new FilesView(filesModel, textGUI, core))
    }
}
