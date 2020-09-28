package com.muwire.gui.wizard

import java.awt.GridBagConstraints
import static com.muwire.gui.Translator.trans

import javax.swing.JFileChooser

import com.muwire.core.MuWireSettings

class DirectoriesStep extends WizardStep {
    
    def downloadLocationField
    def incompleteLocationField
    def downloadLocationButton
    def incompleteLocationButton

    public DirectoriesStep(WizardDefaults defaults) {
        super("directories", defaults)
    }

    @Override
    protected void buildUI(FactoryBuilderSupport builder) {
        
        builder.panel(constraints : getConstraint()) {
            gridBagLayout()
            label(text : trans("SELECT_DOWNLOAD_AND_INCOMPLETE_DIRECTORIES"),
                constraints : gbc(gridx: 0, gridy: 0, gridwidth : 2, insets: [10,0,0,0]))
            label(text : trans("DIRECTORIES_WILL_BE_CREATED"),
                constraints : gbc(gridx:0, gridy: 1, gridwidth: 2, insets: [0,0,10,0]))

            label(text : trans("DIRECTORY_FOR_DOWNLOADED_FILES"), constraints : gbc(gridx:0, gridy: 2))
            downloadLocationField = textField(text : defaults.downloadLocation, 
                constraints : gbc(gridx : 0, gridy : 3, fill : GridBagConstraints.HORIZONTAL, weightx: 100))
            downloadLocationButton = button(text : trans("CHOOSE"), constraints : gbc(gridx: 1, gridy: 3), actionPerformed : showDownloadChooser)
            label(text : trans("DIRECTORY_FOR_INCOMPLETE_FILES"), constraints : gbc(gridx:0, gridy: 4))
            incompleteLocationField = textField(text : defaults.incompleteLocation, 
                constraints : gbc(gridx:0, gridy:5, fill : GridBagConstraints.HORIZONTAL, weightx: 100))
            incompleteLocationButton = button(text : trans("CHOOSE"), constraints : gbc(gridx: 1, gridy: 5), actionPerformed : showIncompleteChooser)
        }
    }        

    @Override
    protected List<String> validate() {
        def rv = []
        if (!canWrite(downloadLocationField.text))
            rv << trans("DOWNLOAD_LOCATION_NOT_WRITEABLE")
        if (!canWrite(incompleteLocationField.text))
            rv << trans("INCOMPLETE_LOCATION_NOT_WRITEABLE")
        rv
    }
    
    private static boolean canWrite(String location) {
        File f = new File(location)
        if (f.exists())
            return f.isDirectory() && f.canWrite()
        canWrite(f.getParentFile().getAbsolutePath())
    }

    @Override
    protected void apply(MuWireSettings muSettings, Properties i2pSettings) {
        muSettings.downloadLocation = new File(downloadLocationField.text)
        muSettings.incompleteLocation = new File(incompleteLocationField.text)
        
        muSettings.downloadLocation.mkdirs()
        muSettings.incompleteLocation.mkdirs()
    }
    
    def showDownloadChooser = {
        String text = chooseFile("SELECT_DIRECTORY_DOWNLOAD_FILES")
        if (text != null)
            downloadLocationField.text = text
    }
    
    def showIncompleteChooser = {
        String text = chooseFile("SELECT_DIRECTORY_INCOMPLETE_FILES")
        if (text != null)
            incompleteLocationField.text = text
    }
    
    private String chooseFile(String title) {
        def chooser = new JFileChooser()
        chooser.setFileHidingEnabled(false)
        chooser.setDialogTitle(trans(title))
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        int rv = chooser.showOpenDialog(null)
        if (rv == JFileChooser.APPROVE_OPTION)
            return chooser.getSelectedFile().getAbsolutePath()
        else
            return null
    }
}
