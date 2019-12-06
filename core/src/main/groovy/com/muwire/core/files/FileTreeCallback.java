package com.muwire.core.files;

import java.io.File;

public interface FileTreeCallback<T> {
    public void onDirectoryEnter(File file);
    public void onDirectoryLeave();
    public void onFile(File file, T value);
}
