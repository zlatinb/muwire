package com.muwire.gui.win;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.win32.W32APIOptions;

public interface MWKernel32 extends Kernel32 {
    
    MWKernel32 INSTANCE = Native.load("kernel32", MWKernel32.class, W32APIOptions.DEFAULT_OPTIONS);
    
    boolean SetPriorityClass(HANDLE hProcess, int mask);
}
