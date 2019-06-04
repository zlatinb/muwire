package com.muwire.core.download

class Pieces {
    private final BitSet bitSet
    private final int nPieces
    private final float ratio
    private final Random random = new Random()
    
    Pieces(int nPieces) {
        this(nPieces, 1.0f)
    }
    
    Pieces(int nPieces, float ratio) {
        this.nPieces = nPieces
        this.ratio = ratio
        bitSet = new BitSet(nPieces)
    }
    
    synchronized int getRandomPiece() {
        int cardinality = bitSet.cardinality()
        if (cardinality == nPieces)
            return -1
        
        // if fuller than ratio just do sequential
        if ( (1.0f * cardinality) / nPieces > ratio) {
            return bitSet.nextClearBit(0)
        }
        
        while(true) {
            int start = random.nextInt(nPieces)
            if (bitSet.get(start))
                continue
            return start
        }
    }
    
    def getDownloaded() {
        def rv = []
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i+1)) {
            rv << i
        }
        rv
    }
    
    synchronized void markDownloaded(int piece) {
        bitSet.set(piece)
    }
    
    synchronized void clear(int piece) {
        bitSet.clear(piece)
    }
    
    synchronized boolean isComplete() {
        bitSet.cardinality() == nPieces
    }
    
    synchronized boolean isMarked(int piece) {
        bitSet.get(piece)
    }
    
    synchronized int donePieces() {
        bitSet.cardinality()
    }
}
