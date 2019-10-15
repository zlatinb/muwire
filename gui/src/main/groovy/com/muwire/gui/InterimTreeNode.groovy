package com.muwire.gui

class InterimTreeNode {
    private final File file
    InterimTreeNode(File file) {
        this.file = file
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof InterimTreeNode))
            return false
        file == o.file
    }
    
    public String toString() {
        file.getName()
    }
}
