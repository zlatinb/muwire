package com.muwire.core.connection

import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import groovy.util.logging.Log
import net.i2p.data.Destination

@Log
class Endpoint implements Closeable {
    final Destination destination
    final InputStream inputStream
    final OutputStream outputStream
    final def toClose

    private final AtomicBoolean closed = new AtomicBoolean()

    Endpoint(Destination destination, InputStream inputStream, OutputStream outputStream, def toClose) {
        this.destination = destination
        this.inputStream = inputStream
        this.outputStream = outputStream
        this.toClose = toClose
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            log.log(Level.WARNING,"Close loop detected for ${destination.toBase32()}", new Exception())
            return
        }
        if (inputStream != null) {
            try {inputStream.close()} catch (Exception ignore) {}
        }
        if (outputStream != null) {
            try {outputStream.close()} catch (Exception ignore) {}
        }
        if (toClose != null) {
            try {toClose.reset()} catch (Exception ignore) {}
        }
    }

    @Override
    public String toString() {
        "destination: ${destination.toBase32()}"
    }
}
