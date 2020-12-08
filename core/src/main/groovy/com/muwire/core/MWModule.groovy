package com.muwire.core

interface MWModule {
    public String getName()
    public void init(Core core)
    public void start()
    public void stop()
}

