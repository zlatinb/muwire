package com.muwire.gui.win

import com.sun.jna.platform.win32.WinNT.HANDLE

class PrioritySetter {
    private static final HANDLE myProcess;
    static {
        myProcess = MWKernel32.INSTANCE.GetCurrentProcess()
    }
    
    static void enterBackgroundMode() {
        MWKernel32.INSTANCE.SetPriorityClass(myProcess, 0x00100000)
    }
    
    static void exitBackgroundMode() {
        MWKernel32.INSTANCE.SetPriorityClass(myProcess, 0x00200000)
    }
}
