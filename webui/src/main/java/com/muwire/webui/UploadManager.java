package com.muwire.webui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.muwire.core.Core;
import com.muwire.core.upload.UploadEvent;
import com.muwire.core.upload.UploadFinishedEvent;
import com.muwire.core.upload.Uploader;

public class UploadManager {
    
    private final Core core;
    private final List<UploaderWrapper> uploads = Collections.synchronizedList(new ArrayList<>());
    
    public UploadManager(Core core) {
        this.core = core;
    }
    
    public List<UploaderWrapper> getUploads() {
        return uploads;
    }
    
    public void onUploadEvent(UploadEvent e) {
        UploaderWrapper wrapper = null;
        synchronized(uploads) {
            for(UploaderWrapper uw : uploads) {
                if (uw.uploader.equals(e.getUploader())) {
                    wrapper = uw;
                    break;
                }
            }
        }
        if (wrapper != null) {
            wrapper.uploader = e.getUploader();
            wrapper.requests++;
            wrapper.finished = false;
        } else {
            wrapper = new UploaderWrapper();
            wrapper.uploader = e.getUploader();
            uploads.add(wrapper);
        }
    }
    
    public void onUploadFinishedEvent(UploadFinishedEvent e) {
        UploaderWrapper wrapper = null;
        synchronized(uploads) {
            for(UploaderWrapper uw : uploads) {
                if (uw.uploader.equals(e.getUploader())) {
                    wrapper = uw;
                    break;
                }
            }
        }
        if (wrapper != null)
            wrapper.finished = true;
    }
    
    public void clearFinished() {
        synchronized(uploads) {
            for(Iterator<UploaderWrapper> iter = uploads.iterator(); iter.hasNext();) {
                UploaderWrapper wrapper = iter.next();
                if (wrapper.finished)
                    iter.remove();
            }
        }
    }
    
    public class UploaderWrapper {
        private volatile Uploader uploader;
        private volatile int requests;
        private volatile boolean finished;
        
        private volatile int[] speedArray = new int[0];
        private volatile int speedPos;
        private volatile long lastSpeedRead = System.currentTimeMillis();
        
        public Uploader getUploader() {
            return uploader;
        }
        
        public int getRequests() {
            return requests;
        }
        
        public boolean isFinished() {
            return finished;
        }
        
        public int speed() {
            
            if (speedArray.length != core.getMuOptions().getSpeedSmoothSeconds()) {
                speedArray = new int[core.getMuOptions().getSpeedSmoothSeconds()];
                speedPos = 0;
            }
            
            final long now = System.currentTimeMillis();
            if (now < lastSpeedRead + 50)
                return speedAverage();
            
            int read = uploader.dataSinceLastRead();
            int speed = (int) (1000.0d * read / (now - lastSpeedRead));
            lastSpeedRead = now;
            
            speedArray[speedPos++] = speed;
            if (speedPos == speedArray.length)
                speedPos = 0;
            return speedAverage();
        }
        
        private int speedAverage() {
            int total = 0;
            for (int reading : speedArray)
                total += reading;
            return total / speedArray.length;
        }
    }
}
