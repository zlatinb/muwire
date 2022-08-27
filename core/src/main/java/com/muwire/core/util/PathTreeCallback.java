package com.muwire.core.util;

import java.nio.file.Path;

public interface PathTreeCallback<T> {
    void onDirectoryEnter(Path path);
    void onDirectoryLeave();
    void onLeaf(Path path, T value);
}
