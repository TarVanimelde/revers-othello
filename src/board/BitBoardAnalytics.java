package board;

public class BitBoardAnalytics {
    /*
       A precomputed array of the adjacent tiles of each position on the board.
       Tile (x, y) is at the (x + y * 8)th index.
    */
    private static long[] adjacentTiles;
    private static boolean adjacentTilesComputed = false;

    private boolean cornerPlayed = false, edgePlayed = false;
    private long stableTiles = 0L; // Bit representation of all tiles whose owner can no longer change.
    private boolean leftEdgeFilled = false; // Have all positions on the left edge been played?
    private boolean rightEdgeFilled = false; // Have all positions on the right edge been played?
    private boolean topEdgeFilled = false; // Have all positions on the top edge been played?
    private boolean bottomEdgeFilled = false; // Have all positions on the bottom edge been played?

    /*
        A bitmask of all edge tiles of the board:
        1 1 1 1 1 1 1 1
        1 0 0 0 0 0 0 1
        1 0 0 0 0 0 0 1
        1 0 0 0 0 0 0 1
        1 0 0 0 0 0 0 1
        1 0 0 0 0 0 0 1
        1 0 0 0 0 0 0 1
        1 1 1 1 1 1 1 1
     */
    private static final long EDGE_MASK = 0xFF818181818181FFL;

    /*
        A bitmask of all corners of the board:
        1 0 0 0 0 0 0 1
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        0 0 0 0 0 0 0 0
        1 0 0 0 0 0 0 1
     */
    private static final long CORNER_MASK = 0x8100000000000081L;

    public BitBoardAnalytics() {
        initializeAdjacentTiles();
    }

    public BitBoardAnalytics(BitBoardAnalytics old) {
        stableTiles = old.stableTiles;
        cornerPlayed = old.cornerPlayed;
        edgePlayed = old.edgePlayed;
        leftEdgeFilled = old.leftEdgeFilled;
        rightEdgeFilled = old.rightEdgeFilled;
        topEdgeFilled = old.topEdgeFilled;
        bottomEdgeFilled = old.bottomEdgeFilled;

    }

    private long bitRep(final int row, final int col) {
        return 0x1L << ((row<<3) + col);
    }

    /**
     * Calculate the adjacent tiles for all tiles in the board. This will be run only once per game.
     */
    public void initializeAdjacentTiles() {
        // Don't run this method more than once:
        if (adjacentTilesComputed) {
            return;
        }
        adjacentTiles = new long[64];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int arrPos = row*8 + col;
                if (isCorner(row, col)) {
                    // Top left corner:
                    if (row == 0 && col == 0) {
                        adjacentTiles[arrPos] = 0x40C0000000000000L;
                    }
                    // Top right corner:
                    else if (row == 0 && col == 7) {
                        adjacentTiles[arrPos] = 0x0203000000000000L;
                    }
                    // Bottm left corner:
                    else if (row == 7 && col == 0L) {
                        adjacentTiles[arrPos] = 0x000000000000C040L;
                    }
                    // Bottom right corner:
                    else {
                        adjacentTiles[arrPos] = 0x0000000000000302L;
                    }
                } else if (isEdge(row, col)) {
                    long pos = bitRep(row, col);
                    // Top edge:
                    if ((pos & 0xFF00000000000000L) != 0L) {
                        adjacentTiles[arrPos] = bitRep(row, col-1) | bitRep(row, col+1)
                                | bitRep(row+1, col-1) | bitRep(row+1, col) | bitRep(row+1, col+1);
                    }
                    // Bottom edge:
                    else if ((pos & 0x00000000000000FFL) != 0L) {
                        adjacentTiles[arrPos] = bitRep(row, col+1) | bitRep(row, col-1)
                                | bitRep(row-1, col+1) | bitRep(row-1, col) | bitRep(row-1, col-1);
                    }
                    // Left edge:
                    else if ((pos & 0x8080808080808080L) != 0L) {
                        adjacentTiles[arrPos] = bitRep(row-1, col) | bitRep(row-1, col+1)
                                | bitRep(row, col+1)
                                | bitRep(row+1, col) | bitRep(row+1, col+1);
                    }
                    // Right edge:
                    else {
                        adjacentTiles[arrPos] = bitRep(row-1, col-1) | bitRep(row-1, col)
                                | bitRep(row, col-1)
                                | bitRep(row+1, col-1) | bitRep(row+1, col);
                    }
                }
				/* An interior tile, i.e. one of the ones in
				    0 0 0 0 0 0 0 0
				    0 1 1 1 1 1 1 0
				    0 1 1 1 1 1 1 0
				    0 1 1 1 1 1 1 0
				    0 1 1 1 1 1 1 0
				    0 1 1 1 1 1 1 0
				    0 1 1 1 1 1 1 0
				    0 0 0 0 0 0 0 0
				 */
                else {
                    adjacentTiles[arrPos] = bitRep(row-1, col-1) | bitRep(row-1, col) | bitRep(row-1, col+1)
                            | bitRep(row, col-1) | bitRep(row, col+1)
                            | bitRep(row+1, col-1) | bitRep(row+1, col) | bitRep(row+1, col+1);
                }
            }
        }
        adjacentTilesComputed = true;
    }

    /**
     * Checks whether any edges have been completely filled by tiles. If an edge
     * has been filled, then the entire edge is added to the stable tile mask.
     */
    private void updateEdgeStability(final long darkBoard, final long lightBoard) {
        if (topEdgeFilled && bottomEdgeFilled && leftEdgeFilled && rightEdgeFilled) {
            return;
        }

        long board = darkBoard | lightBoard; // A bitmask of all nonempty tiles.

        /*
            Checks if all tiles on the top edge of the board are filled:
            1 1 1 1 1 1 1 1
            0 0 0 0 0 0 0 0
            0 0 0 0 0 0 0 0
            0 0 0 0 0 0 0 0
            0 0 0 0 0 0 0 0
            0 0 0 0 0 0 0 0
            0 0 0 0 0 0 0 0
            0 0 0 0 0 0 0 0
         */
        final long topEdge = 0xFF00000000000000L;
        topEdgeFilled = (board & topEdge) == topEdge;
        if (topEdgeFilled) {
            stableTiles |= topEdge;
        }

        final long bottomEdge = 0x00000000000000FFL;
        bottomEdgeFilled = (board & bottomEdge) == bottomEdge;
        if (bottomEdgeFilled) {
            stableTiles |= bottomEdge;
        }

        final long leftEdge = 0x8080808080808080L;
        leftEdgeFilled = (board & leftEdge) == leftEdge;
        if (leftEdgeFilled) {
            stableTiles |= leftEdge;
        }

        final long rightEdge = 0x0101010101010101L;
        rightEdgeFilled = (board & rightEdge) == rightEdge;
        if (rightEdgeFilled) {
            stableTiles |= rightEdge;
        }
    }


    public boolean isCorner(final int row, final int col) {
        return isCorner(bitRep(row, col));
    }

    public boolean isCorner(final long position) {
        return (position & (CORNER_MASK)) != 0L;
    }

    public boolean isEdge(final int row, int col) {
        return isEdge(bitRep(row, col));
    }

    public boolean isEdge(final long pos) {
        return (pos & EDGE_MASK) != 0L;
    }

    public int getStableTileCount(final TileState playerColor, final long darkBoard, final long lightBoard) {
        if (!cornerPlayed) {// no stable tiles are possible yet
            return 0;
        }
        long playerBoard = playerColor == TileState.DARK ? darkBoard : lightBoard;
        return Long.bitCount(stableTiles & playerBoard);
    }

    /**
     * Updates the set of tiles that can no longer change colour for both players.
     */
    public void updateStableBoard(final long darkBoard, final long lightBoard) {
        updateStableBoard(TileState.DARK, darkBoard, lightBoard);
        updateStableBoard(TileState.LIGHT, darkBoard, lightBoard);
    }

    /*
    Returns a bit representation of all the tiles owned by the
    player with colour playerColor that are adjacent (i.e. one
    step in any direction) to the given position. It is assumed
    that pos has exactly one bit set to one.
 */
    public long getAdjacentTiles(final long pos, TileState playerColor, final long darkBoard, final long lightBoard) {
        switch (playerColor) {
            case DARK:
                return adjacentTiles[Long.numberOfLeadingZeros(pos)] & darkBoard;
            case LIGHT:
                return adjacentTiles[Long.numberOfLeadingZeros(pos)] & lightBoard;
            default: // Empty tiles.
                return adjacentTiles[Long.numberOfLeadingZeros(pos)] & (~(lightBoard | darkBoard));
        }
    }

    /**
     * Updates the set of tiles that can no longer change colour with colour playerColour.
     */
    private void updateStableBoard(final TileState playerColor, final long darkBoard, final long lightBoard) {
        if (!cornerPlayed) {// No stable tiles are possible if no corners have been played.
            return;
        }
        updateEdgeStability(darkBoard, lightBoard); // Special handling for if an entire edge is filled
        long playerBoard = playerColor == TileState.DARK ? darkBoard : lightBoard;
		/*
		 * Go through the entire board. After determining that a candidate position
		 * is stable, restart the search through the board (since the additional
		 * stable tile could mean other tiles are now stable too).
		 */
        for (long i = 0; i < 64; i++) {
            long candidate = 0x1L << i;
            // Ignore tiles of the wrong color:
            if ((playerBoard & candidate) == 0L) {
                continue;
            }
            // Candidate already known to be stable:
            else if ((stableTiles & candidate) != 0L) {
                continue;
            }

            // A candidate is stable if it is a corner position:
            if (isCorner(candidate)) {
                stableTiles |= candidate;
                i = 0;
            }
            /*
                A candidate is stable if it is an edge position and adjacent to a
                stable edge tile with the same colour:
             */
            else if (isEdge(candidate)) {
                boolean stableEdge = Long.bitCount(
                        (getAdjacentTiles(candidate, playerColor, darkBoard, lightBoard) & stableTiles)
                                & EDGE_MASK
                ) > 0; //only consider edge tiles
                if (stableEdge) {
                    stableTiles |= candidate;
                    i = 0;
                }
            }
            // A candidate is stable if there are sufficient adjacent stable tiles of the same colour:
            else {
                long stableNeighbors = Long.bitCount(getAdjacentTiles(candidate, playerColor, darkBoard, lightBoard) & stableTiles);
                if (stableNeighbors == 3) {
					/* Four adjacent stable neighbours of the same colour guarantees the candidate
					 * is stable. There is a special configuration where 3 stable neighbors is
					 * sufficient to say a candidate for stability is stable. Isomorphic under
					 * rotation, with C the candidate position and S are three tiles known to
					 * be stable with the same color as C:
					case 1:
					 S S
					 S C
					case 2:
					 S S
					 C S
					case 3:
					 C S
					 S S
					case 4:
					 S C
					 S S
					Don't need to worry about going over board edge, since the current position
					is guaranteed to not be an edge. */
                    long config1 = (0x1L << (i-1L)) | (0x1L << (i-9L)) | (0x1L << (i-8L)),
                            config2 = (0x1L << (i-8L)) | (0x1L << (i-7L)) | (0x1L << (i+1L)),
                            config3 = (0x1L << (i+1L)) | (0x1L << (i+9L)) | (0x1L << (i+8L)),
                            config4 = (0x1L << (i+8L)) | (0x1L << (i+7L)) | (0x1L << (i-1L));

                    if ((config1 & stableTiles) == config1
                            || (config2 & stableTiles) == config2
                            || (config3 & stableTiles) == config3
                            || (config4 & stableTiles) == config4) {
                        stableTiles |= candidate;
                        i = 0;
                    }
                }
                // If a candidate is adjacent to at least 4 stable tiles of the same color, then it is also stable:
                else if (stableNeighbors >= 4) {
                    stableTiles |= candidate;
                    i = 0;
                }
            }

        }
    }

    public boolean edgePlayed() {
        return edgePlayed;
    }

    public boolean cornerPlayed() {
        return cornerPlayed;
    }

    public void setCornerPlayed() {
        cornerPlayed = true;
    }

    public void setEdgePlayed() {
        edgePlayed = true;
    }
}
