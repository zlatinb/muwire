package com.muwire.core.util;

import java.nio.file.Path;

public interface PathTreeCallback<T, I> {
    void onDirectoryEnter(Path path, I value);
    void onDirectoryLeave();
    void onLeaf(Path path, T value);
}
