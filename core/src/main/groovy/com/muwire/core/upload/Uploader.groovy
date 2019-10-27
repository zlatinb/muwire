package com.muwire.core.upload

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption

import com.muwire.core.connection.Endpoint

abstract class Uploader {
    protected final Endpoint endpoint
    protected ByteBuffer mapped
    
    private long lastSpeedRead
    protected int dataSinceLastRead

    private final ArrayList<Integer> speedArr = [0,0,0,0,0]
    private int speedPos, speedAvg
    
    Uploader(Endpoint endpoint) {
        this.endpoint = endpoint
    }

    abstract void respond()

    public synchronized int getPosition() {
        if (mapped == null)
            return -1
        mapped.position()
    }

    abstract String getName();

    /**
     * @return an integer between 0 and 100
     */
    abstract int getProgress();

    abstract String getDownloader();

    abstract int getDonePieces();

    abstract int getTotalPieces();

    abstract long getTotalSize();
    
    synchronized int speed() {
        final long now = System.currentTimeMillis()
        long interval = Math.max(1000, now - lastSpeedRead)
        lastSpeedRead = now;
        int currSpeed = (int) (dataSinceLastRead * 1000.0 / interval)
        dataSinceLastRead = 0

        // normalize to speedArr.size
        currSpeed /= speedArr.size()

        // compute new speedAvg and update speedArr
        if ( speedArr[speedPos] > speedAvg ) {
            speedAvg = 0
        } else {
            speedAvg -= speedArr[speedPos]
        }
        speedAvg += currSpeed
        speedArr[speedPos] = currSpeed
        // this might be necessary due to rounding errors
        if (speedAvg < 0)
            speedAvg = 0

        // rolling index over the speedArr
        speedPos++
        if (speedPos >= speedArr.size())
            speedPos=0

        speedAvg
    }
}
