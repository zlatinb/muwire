package com.muwire.gui

class InterimTreeNode {
    private final File file
    private final String toString
    InterimTreeNode(File file) {
        this.file = file
        this.toString = HTMLSanitizer.sanitize(file.getName())
    }
    
    public File getFile() {
        return file;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof InterimTreeNode))
            return false
        file == o.file
    }
    
    public String toString() {
        toString
    }
}
