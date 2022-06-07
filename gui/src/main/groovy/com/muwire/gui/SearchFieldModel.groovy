package com.muwire.gui

import javax.swing.AbstractListModel
import javax.swing.DefaultComboBoxModel
import javax.swing.MutableComboBoxModel

class SearchFieldModel extends DefaultComboBoxModel implements MutableComboBoxModel {
    private final UISettings uiSettings
    private final File settingsFile

    SearchFieldModel(UISettings uiSettings, File home) {
        super()
        this.uiSettings = uiSettings
        this.settingsFile = new File(home, "gui.properties")
        addAll(uiSettings.searchHistory)
    }

    public void addElement(Object string) {
        if (string == null)
            return
        if (uiSettings.storeSearchHistory) {
            if (!uiSettings.searchHistory.add(string))
                return
            settingsFile.withOutputStream { uiSettings.write(it) }
        }
        super.addElement(string)
    }
    
    boolean onKeyStroke(String selected) {
        if (selected == null || selected.length() == 0) {
            removeAllElements()
            addAll(uiSettings.searchHistory)
            return true
        }

        removeAllElements()
        setSelectedItem(selected)

        List<String> matching = new ArrayList<>(uiSettings.searchHistory)
        matching.retainAll { it.containsIgnoreCase(selected) }

        if (matching.isEmpty())
            return false

        Collections.sort(matching)
        addAll(matching)
        return true
    }
}
