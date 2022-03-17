package com.muwire.gui.win

import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinNT.HANDLE

class PrioritySetter {
    private static final HANDLE myProcess;
    static {
        myProcess = MWKernel32.INSTANCE.GetCurrentProcess()
    }
    
    static void enterBackgroundMode() {
        // 1. set BELOW_NORMAL
        MWKernel32.INSTANCE.SetPriorityClass(myProcess, new WinDef.DWORD(0x00004000L))
        // 2. enter background mode
        MWKernel32.INSTANCE.SetPriorityClass(myProcess, new WinDef.DWORD(0x00100000))
        // 3. increase the process working set to something reasonable 
        def sizeT = new BaseTSD.SIZE_T(0x1L << 29) // 512 MB
        MWKernel32.INSTANCE.SetProcessWorkingSetSizeEx(myProcess, sizeT, sizeT, new WinDef.DWORD(0x00000008L))
    }
    
    static void exitBackgroundMode() {
        // 1. exit background mode
        MWKernel32.INSTANCE.SetPriorityClass(myProcess, new WinDef.DWORD(0x00200000L))
        // 2. set priority to normal
        MWKernel32.INSTANCE.SetPriorityClass(myProcess, new WinDef.DWORD(0x00000020L))
    }
}
