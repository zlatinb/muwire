package com.muwire.webui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.SharedFile;
import com.muwire.core.files.DirectoryUnsharedEvent;
import com.muwire.core.files.FileDownloadedEvent;
import com.muwire.core.files.FileHashedEvent;
import com.muwire.core.files.FileHashingEvent;
import com.muwire.core.files.FileListCallback;
import com.muwire.core.files.FileLoadedEvent;
import com.muwire.core.files.FileSharedEvent;
import com.muwire.core.files.FileTree;
import com.muwire.core.files.FileTreeCallback;
import com.muwire.core.files.FileUnsharedEvent;
import com.muwire.core.files.UICommentEvent;
import com.muwire.core.util.DataUtil;

import net.i2p.data.Base64;

public class FileManager {

    private final Core core;
    private final FileTree<SharedFile> fileTree = new FileTree<>();
    
    private volatile String hashingFile;
    private volatile long revision;
    
    public FileManager(Core core) {
        this.core = core;
    }
    
    public void onFileLoadedEvent(FileLoadedEvent e) {
        fileTree.add(e.getLoadedFile().getFile(), e.getLoadedFile());
        revision++;
    }
    
    public void onFileHashedEvent(FileHashedEvent e) {
        hashingFile = null;
        if (e.getSharedFile() == null) // TODO: think of something better
            return;
        fileTree.add(e.getSharedFile().getFile(), e.getSharedFile());
        revision++;
    }
    
    public void onFileHashingEvent(FileHashingEvent e) {
        hashingFile = e.getHashingFile().getPath();
    }
    
    public void onFileDownloadedEvent(FileDownloadedEvent e) {
        if (core.getMuOptions().getShareDownloadedFiles()) {
            fileTree.add(e.getDownloadedFile().getFile(), e.getDownloadedFile());
            revision++;
        }
    }
    
    public void onFileUnsharedEvent(FileUnsharedEvent e) {
        if (!e.getDeleted())
            return;
        fileTree.remove(e.getUnsharedFile().getFile());
        revision++;
    }
    
    public void onDirectoryUnsharedEvent(DirectoryUnsharedEvent e) {
        if (!e.getDeleted())
            return;
        fileTree.remove(e.getDirectory());
        revision++;
    }
    
    void list(File parent, FileListCallback<SharedFile> callback) {
        fileTree.list(parent, callback);
    }
    
    List<SharedFile> getAllFiles() {
        return getAllFiles(null);
    }
    
    List<SharedFile> getAllFiles(File parent) {
        TraverseCallback cb = new TraverseCallback();
        fileTree.traverse(parent, cb);
        return cb.found;
    }
    
    String getHashingFile() {
        return hashingFile;
    }
    
    int numSharedFiles() {
        return core.getFileManager().getFileToSharedFile().size();
    }
    
    long getRevision() {
        return revision;
    }
    
    void share(String filePath) {
        File file = new File(filePath);
        FileSharedEvent event = new FileSharedEvent();
        event.setFile(file);
        core.getEventBus().publish(event);
    }
    
    void unshareFile(File file) {
        if (file.isFile()) {
            SharedFile sf = core.getFileManager().getFileToSharedFile().get(file);
            if (sf == null)
                return;

            fileTree.remove(file);
            revision++;
            FileUnsharedEvent event = new FileUnsharedEvent();
            event.setUnsharedFile(sf);
            core.getEventBus().publish(event);
        } else {
            
            TraverseCallback cb = new TraverseCallback();
            fileTree.traverse(file, cb);
            
            fileTree.remove(file);
            revision++;
            
            for (SharedFile found : cb.found) {
                FileUnsharedEvent e = new FileUnsharedEvent();
                e.setUnsharedFile(found);
                core.getEventBus().publish(e);
            }
            
            for (File directory : cb.directories) {
                if (core.getMuOptions().getWatchedDirectories().contains(directory.getAbsolutePath())) {
                    DirectoryUnsharedEvent e = new DirectoryUnsharedEvent();
                    e.setDirectory(directory);
                    core.getEventBus().publish(e);
                }
            }
            
            if (core.getMuOptions().getWatchedDirectories().contains(file.getAbsolutePath())) {
                DirectoryUnsharedEvent event = new DirectoryUnsharedEvent();
                event.setDirectory(file);
                core.getEventBus().publish(event);
            }
        }
        Util.pause();
    }
    
    void comment(File file, String comment) {
    
        if (file.isFile()) {
            SharedFile sf = core.getFileManager().getFileToSharedFile().get(file);
            if (sf == null)
                return;
            UICommentEvent e = new UICommentEvent();
            e.setOldComment(sf.getComment());
            sf.setComment(Base64.encode(DataUtil.encodei18nString(comment)));
            e.setSharedFile(sf);
            revision++;
            core.getEventBus().publish(e);
        } else {
            TraverseCallback cb = new TraverseCallback();
            fileTree.traverse(file, cb);
            
            for (SharedFile found : cb.found) {
                comment(found.getFile(), comment);
            }
        }
    }
    
    private static class TraverseCallback implements FileTreeCallback<SharedFile> {
        private final List<SharedFile> found = new ArrayList<>();
        private final List<File> directories = new ArrayList<>();

        @Override
        public void onDirectoryEnter(File file) {
            directories.add(file);
        }

        @Override
        public void onDirectoryLeave() {
        }

        @Override
        public void onFile(File file, SharedFile value) {
            found.add(value);
        }
    }
}
