package com.muwire.core.util

import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

import net.i2p.util.Log

class JULLog extends Log {

    private static final Map<Integer, Level> I2P_TO_JUL = new HashMap<>()
    static {
        I2P_TO_JUL.put(Log.DEBUG, Level.FINE)
        I2P_TO_JUL.put(Log.INFO, Level.INFO)
        I2P_TO_JUL.put(Log.WARN, Level.WARNING)
        I2P_TO_JUL.put(Log.ERROR, Level.SEVERE)
        I2P_TO_JUL.put(Log.CRIT, Level.SEVERE)
    }

    private final Logger delegate
    private final Level level

    public JULLog(Class<?> cls) {
        super(cls)
        delegate = Logger.getLogger(cls.getName())
        level = findLevel(delegate)
    }

    public JULLog(String name) {
        super(name);
        delegate = Logger.getLogger(name)
        level = findLevel(delegate)
    }

    private static Level findLevel(Logger log) {
        while (log.getLevel() == null)
            log = log.getParent()
        log.getLevel()
    }

    @Override
    public void log(int priority, String msg) {
        delegate.log(I2P_TO_JUL.get(priority), msg)
    }

    @Override
    public void log(int priority, String msg, Throwable t) {
        delegate.log(I2P_TO_JUL.get(priority), msg, t)
    }

    @Override
    public boolean shouldLog(int priority) {
        delegate.isLoggable(I2P_TO_JUL.get(priority))
    }

    @Override
    public boolean shouldDebug() {
        level.intValue().intValue() <= Level.FINE.intValue()
    }

    @Override
    public boolean shouldInfo() {
        level.intValue().intValue() <= Level.INFO.intValue()
    }

    @Override
    public boolean shouldWarn() {
        level.intValue().intValue() <= Level.WARNING.intValue()
    }

    @Override
    public boolean shouldError() {
        level.intValue().intValue() <= Level.SEVERE.intValue()
    }
}
