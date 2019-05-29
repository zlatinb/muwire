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
            while(bitSet.get(start) && ++start < nPieces);
            return start
        }
    }
    
    synchronized void markDownloaded(int piece) {
        bitSet.set(piece)
    }
    
    synchronized boolean isComplete() {
        bitSet.cardinality() == nPieces
    }
    
    synchronized int donePieces() {
        bitSet.cardinality()
    }
}
