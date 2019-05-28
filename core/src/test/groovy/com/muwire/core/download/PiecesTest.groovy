package com.muwire.core.download

import org.junit.Test

class PiecesTest {
    
    Pieces pieces
    
    @Test
    public void testEmpty() {
        pieces = new Pieces(20)
        assert !pieces.isComplete()
    }
    
    @Test
    public void testSinglePiece() {
        pieces = new Pieces(1)
        assert !pieces.isComplete()
        assert pieces.getRandomPiece() == 0
        pieces.markDownloaded(0)
        assert pieces.isComplete()
    }
    
    @Test
    public void testTwoPieces() {
        pieces = new Pieces(2)
        assert !pieces.isComplete()
        int piece = pieces.getRandomPiece()
        assert piece == 0 || piece == 1
        pieces.markDownloaded(piece)
        assert !pieces.isComplete()
        int piece2 = pieces.getRandomPiece()
        assert piece != piece2
        pieces.markDownloaded(piece2)
        assert pieces.isComplete()
    }
}
