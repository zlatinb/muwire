package com.muwire.core

abstract class Service {

    volatile boolean loaded

    abstract void load()

    void waitForLoad() {
        while (!loaded)
            Thread.sleep(10)
    }
}
