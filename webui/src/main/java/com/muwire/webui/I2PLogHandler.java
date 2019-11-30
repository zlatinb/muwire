package com.muwire.webui;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;

class I2PLogHandler extends Handler {

    private final I2PAppContext ctx;
    
    I2PLogHandler(I2PAppContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public void publish(LogRecord record) {
        String name = record.getLoggerName();
        Log log = ctx.logManager().getLog(name);
        int priority = logPriority(record.getLevel());
        if (log.shouldLog(priority)) {
            if (record.getThrown() == null)
                log.log(priority, record.getMessage());
            else
                log.log(priority, record.getMessage(), record.getThrown());
        }
    }

    @Override
    public void flush() {
        ctx.logManager().flush();
    }

    @Override
    public void close() throws SecurityException {
        // ok what happens here?
    }
    
    private static int logPriority(Level level) {
        // this can be more exhaustive later
        if (level == Level.FINE)
            return Log.DEBUG;
        if (level == Level.INFO)
            return Log.INFO;
        if (level == Level.WARNING)
            return Log.WARN;
        if (level == Level.SEVERE)
            return Log.ERROR;
        return Log.CRIT;
    }
}
