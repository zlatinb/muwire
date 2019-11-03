package com.muwire.gui

import javax.swing.AbstractListModel
import javax.swing.MutableComboBoxModel

class SearchFieldModel extends AbstractListModel implements MutableComboBoxModel {
    private final UISettings uiSettings
    private final File settingsFile
    private final List<String> objects = new ArrayList<>()
    private String selectedObject
    
    SearchFieldModel(UISettings uiSettings, File home) {
        super()
        this.uiSettings = uiSettings
        this.settingsFile = new File(home, "gui.properties")
        uiSettings.searchHistory.each { objects.add(it) }
        fireIntervalAdded(this, 0, objects.size() - 1)
    }
    
    public void addElement(Object string) {
        if (uiSettings.storeSearchHistory) {
            if (!uiSettings.searchHistory.add(string))
                return
            settingsFile.withOutputStream { uiSettings.write(it) }
        }
        objects.add(string);
        fireIntervalAdded(this,objects.size()-1, objects.size()-1);
        if ( objects.size() == 1 && selectedObject == null && string != null ) {
            setSelectedItem( string );
        }
    }
    
    boolean onKeyStroke(String selected) {
        selectedObject = selected
        if (selected == null || selected.length() == 0) {
            objects.clear()
            uiSettings.searchHistory.each { objects.add(it) }
            return true
        }
        
        objects.clear()

        Set<String> matching = new HashSet<>(uiSettings.searchHistory)
        matching.retainAll { it.contains(selected) }
        
        matching.each {
            objects.add(it)
        }
        Collections.sort(objects)
        if (!objects.isEmpty()) {
            fireIntervalAdded(this, 0, objects.size() - 1)
            return true
        }
        false
    }

    @Override
    public void setSelectedItem(Object anObject) {
        if ((selectedObject != null && !selectedObject.equals( anObject )) ||
            selectedObject == null && anObject != null) {
            selectedObject = anObject;
            fireContentsChanged(this, -1, -1);
        }
    }

    @Override
    public Object getSelectedItem() {
        selectedObject
    }

    @Override
    public int getSize() {
        objects.size()
    }

    @Override
    public Object getElementAt(int index) {
        if ( index >= 0 && index < objects.size() )
            return objects.get(index);
        else
            return null;
    }

    @Override
    public void removeElement(Object obj) {
        
    }

    @Override
    public void insertElementAt(Object item, int index) {
        
    }

    @Override
    public void removeElementAt(int index) {
        
    }
}
