package com.muwire.gui.wizard

import javax.swing.JFileChooser

import com.muwire.core.MuWireSettings

class DirectoriesStep extends WizardStep {
    
    def downloadLocationField
    def incompleteLocationField
    def downloadLocationButton
    def incompleteLocationButton

    public DirectoriesStep(String constraint) {
        super("directories")
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder) {
        File defaultDownloadLocation = new File(System.getProperty("user.home"), "MuWire Downloads")
        File defaultIncompleteLocation = new File(System.getProperty("user.home"), "MuWire Incompletes")
        
        builder.panel(constraints : getConstraint()) {
            gridBagLayout()
            label(text : "Select directories for saving downloaded and incomplete files.  They will be created if they do not already exist",
            constraints : gbc(gridx: 0, gridy: 0, gridwidth : 2))

            label(text : "Directory for saving downloaded files", constraints : gbc(gridx:0, gridy: 1))
            downloadLocationField = textField(text : defaultDownloadLocation.getAbsolutePath(), constraints : gbc(gridx : 0, gridy : 2))
            downloadLocationButton = button(text : "Choose", constraints : gbc(gridx: 1, gridy: 2), actionPerformed : showDownloadChooser)
            label(text : "Directory for storing incomplete files", constraints : gbc(gridx:0, gridy: 3))
            incompleteLocationField = textField(text : defaultIncompleteLocation.getAbsolutePath(), constraints : gbc(gridx:0, gridy:4))
            incompleteLocationButton = button(text : "Choose", constraints : gbc(gridx: 1, gridy: 4), actionPerformed : showIncompleteChooser)
        }
    }        

    @Override
    protected List<String> validate() {
        // TODO Auto-generated method stub
        return null
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
        // TODO Auto-generated method stub

    }
    
    def showDownloadChooser = {
        String text = chooseFile("Select directory for downloaded files")
        if (text != null)
            downloadLocationField.text = text
    }
    
    def showIncompleteChooser = {
        String text = chooseFile("Select directory for incomplete files")
        if (text != null)
            incompleteLocationField.text = text
    }
    
    private String chooseFile(String title) {
        def chooser = new JFileChooser()
        chooser.setFileHidingEnabled(false)
        chooser.setDialogTitle(title)
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile().getAbsolutePath()
        else
            return null
    }
}
