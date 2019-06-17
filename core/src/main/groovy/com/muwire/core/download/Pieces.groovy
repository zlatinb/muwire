package com.muwire.core.download

class Pieces {
    private final BitSet done, claimed
    private final int nPieces
    private final float ratio
    private final Random random = new Random()
    
    Pieces(int nPieces) {
        this(nPieces, 1.0f)
    }
    
    Pieces(int nPieces, float ratio) {
        this.nPieces = nPieces
        this.ratio = ratio
        done = new BitSet(nPieces)
        claimed = new BitSet(nPieces)
    }
    
    synchronized int claim() {
        int claimedCardinality = claimed.cardinality()
        if (claimedCardinality == nPieces)
            return -1
        
        // if fuller than ratio just do sequential
        if ( (1.0f * claimedCardinality) / nPieces > ratio) {
            int rv = claimed.nextClearBit(0)
            claimed.set(rv)
            return rv
        }
        
        while(true) {
            int start = random.nextInt(nPieces)
            if (claimed.get(start))
                continue
            claimed.set(start)
            return start
        }
    }
    
    synchronized def getDownloaded() {
        def rv = []
        for (int i = done.nextSetBit(0); i >= 0; i = done.nextSetBit(i+1)) {
            rv << i
        }
        rv
    }
    
    synchronized void markDownloaded(int piece) {
        done.set(piece)
        claimed.set(piece)
    }
    
    synchronized void unclaim(int piece) {
        claimed.clear(piece)
    }
    
    synchronized boolean isComplete() {
        done.cardinality() == nPieces
    }
    
    synchronized int donePieces() {
        done.cardinality()
    }
}
