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
        assert pieces.claim() == [0,0,0]
        pieces.markDownloaded(0)
        assert pieces.isComplete()
    }

    @Test
    public void testTwoPieces() {
        pieces = new Pieces(2)
        assert !pieces.isComplete()
        int[] piece = pieces.claim()
        assert piece[0] == 0 || piece[0] == 1
        pieces.markDownloaded(piece[0])
        assert !pieces.isComplete()
        int[] piece2 = pieces.claim()
        assert piece[0] != piece2[0]
        pieces.markDownloaded(piece2[0])
        assert pieces.isComplete()
    }

    @Test
    public void testClaimAvailable() {
        pieces = new Pieces(2)
        int[] claimed = pieces.claim([0].toSet())
        assert claimed == [0,0,0]
        assert [0,0,1] == pieces.claim([0].toSet())
    }

    @Test
    public void testClaimNoneAvailable() {
        pieces = new Pieces(20)
        int[] claimed = pieces.claim()
        assert [0,0,0] == pieces.claim(claimed.toSet())
    }
}
