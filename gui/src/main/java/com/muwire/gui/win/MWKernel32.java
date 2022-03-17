package com.muwire.gui.win;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface MWKernel32 extends StdCallLibrary {
    
    MWKernel32 INSTANCE = Native.load("kernel32", MWKernel32.class, W32APIOptions.DEFAULT_OPTIONS);
    
    WinDef.BOOL SetPriorityClass(WinNT.HANDLE hProcess, WinDef.DWORD mask);
    WinNT.HANDLE GetCurrentProcess();
    
    WinDef.BOOL SetProcessWorkingSetSizeEx(WinNT.HANDLE hProcess, 
                                           BaseTSD.SIZE_T minSize,
                                           BaseTSD.SIZE_T maxSize,
                                           WinDef.DWORD flags);
}
