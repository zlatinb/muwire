package com.muwire.core

import java.util.concurrent.atomic.AtomicLong

class Event {

    private static final AtomicLong SEQ_NO = new AtomicLong();
    final long seqNo
    final long timestamp

    Event() {
        seqNo = SEQ_NO.getAndIncrement()
        timestamp = System.currentTimeMillis()
    }

    @Override
    public String toString() {
        "seqNo $seqNo timestamp $timestamp"
    }
}
