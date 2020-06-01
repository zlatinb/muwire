package com.muwire.gui

import java.util.logging.Level
import java.util.logging.LogManager

import griffon.swing.SwingGriffonApplication
import groovy.util.logging.Log

@Log
class Launcher {

    public static void main(String[] args) {
        if (System.getProperty("java.util.logging.config.file") == null) {
            log.info("No config file specified, so turning off most logging")
            def names = LogManager.getLogManager().getLoggerNames()
            while(names.hasMoreElements()) {
                def name = names.nextElement()
                LogManager.getLogManager().getLogger(name).setLevel(Level.SEVERE)
            }
        }
        
        SwingGriffonApplication.main(args)
    }
}
