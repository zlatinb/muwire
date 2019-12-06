package com.muwire.webui;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import com.muwire.core.Core;
import com.muwire.core.SharedFile;
import com.muwire.core.files.FileDownloadedEvent;
import com.muwire.core.files.FileHashedEvent;
import com.muwire.core.files.FileListCallback;
import com.muwire.core.files.FileLoadedEvent;
import com.muwire.core.files.FileTree;

public class FileManager {

    private final Core core;
    private final FileTree<SharedFile> fileTree = new FileTree<>();
    
    public FileManager(Core core) {
        this.core = core;
    }
    
    public void onFileLoadedEvent(FileLoadedEvent e) {
        fileTree.add(e.getLoadedFile().getFile(), e.getLoadedFile());
    }
    
    public void onFileHashedEvent(FileHashedEvent e) {
        fileTree.add(e.getSharedFile().getFile(), e.getSharedFile());
    }
    
    public void onFileDownloadedEvent(FileDownloadedEvent e) {
        if (core.getMuOptions().getShareDownloadedFiles())
            fileTree.add(e.getDownloadedFile().getFile(), e.getDownloadedFile());
    }
    
    void list(File parent, FileListCallback<SharedFile> callback) {
        fileTree.list(parent, callback);
    }
}
