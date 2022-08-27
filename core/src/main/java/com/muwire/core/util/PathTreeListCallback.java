package com.muwire.core.util;

import java.nio.file.Path;

public interface PathTreeListCallback<T> {
    void onLeaf(Path path, T value);
    void onDirectory(Path path);
}
