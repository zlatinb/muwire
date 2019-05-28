package com.muwire.core.download

class Pieces {
    private final BitSet bitSet
    private final int nPieces
    private final Random random = new Random()
    
    Pieces(int nPieces) {
        this.nPieces = nPieces
        bitSet = new BitSet(nPieces)
    }
    
    synchronized int getRandomPiece() {
        if (isComplete())
            return -1
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
}
