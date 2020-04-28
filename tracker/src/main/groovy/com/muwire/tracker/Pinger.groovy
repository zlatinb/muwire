package com.muwire.tracker

import org.springframework.stereotype.Component

import com.muwire.core.Core

@Component
class Pinger {
    private final Core core
    
    Pinger(Core core) {
        this.core = core
    }
}
