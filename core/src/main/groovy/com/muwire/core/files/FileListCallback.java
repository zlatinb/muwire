package com.muwire.core.files;

import java.io.File;

public interface FileListCallback<T> {

    public void onFile(File f, T value);
    
    public void onDirectory(File f);
}
