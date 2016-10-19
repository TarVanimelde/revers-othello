package board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An 8x8 board for Othello using the bits of longs to represent the board.
 * @author TarVanimelde
 *
 */
public class OthelloBitBoard extends OthelloBoard {
    /*
        Bitmasks used for fast determination of valid moves:
     */
	private static final long DOWN_MASK =  ~0xFF00000000000000L,
			UP_MASK =    ~0x00000000000000FFL,
			LEFT_MASK =  ~0x0101010101010101L,
			RIGHT_MASK = ~0x8080808080808080L;

	private long lightTiles; // Bitmask of all tiles currently held by the light player.
    private long darkTiles; // Bitmask of all tiles currently held by the dark player.
    /*
        The set of legal moves available to each player on the current board configuration.
     */
	private long legalDarkMoves, legalLightMoves;

    private BitBoardAnalytics analytics;

	public OthelloBitBoard() {
		super(8, true);
        analytics = new BitBoardAnalytics();
        analytics.initializeAdjacentTiles();
		lightTiles = 0x0000001008000000L;
		darkTiles =  0x0000000810000000L;
		legalDarkMoves = 0L;
		legalLightMoves = 0L;
		updateLegalMoves();
		precedingMove = null;
	}

	public OthelloBitBoard(final OthelloBitBoard old) {
		super(old.boardSize);
        analytics = new BitBoardAnalytics(old.analytics);
		lightTiles = old.lightTiles;
		darkTiles = old.darkTiles;
		legalDarkMoves = old.legalDarkMoves;
		legalLightMoves = old.legalLightMoves;
		darkScore = old.darkScore;
		lightScore = old.lightScore;
		precedingMove = old.precedingMove;
	}

	/**
	 * Returns the bitboard representation of the position (row, col).
	 */
	private long bitRepOf(final int row, final int col) {
		return 0x1L << ((row<<3) + col);
	}

	public boolean equals(Object rhs) {
		if (rhs == null || !(rhs instanceof OthelloBitBoard)) {
            return false;
        }
		OthelloBitBoard b = (OthelloBitBoard)rhs;
		return darkTiles == b.darkTiles && lightTiles == b.lightTiles;
	}
	
	/**
	 * Given a move, which is assumed to be valid, changes owner of
     * the appropriate tiles.
	 * TODO: could use bitops instead for efficiency gains.
	 * @param m: the move
	 * @param playerColor: color of the player making the move.
	 * @return The number of tiles that changed colour.
	 */
	private int updateTileOwners(final Move m, final TileState playerColor) {
		long playerTiles = (playerColor == TileState.DARK) ? darkTiles : lightTiles;
		long opponentTiles = (playerColor == TileState.DARK) ? lightTiles : darkTiles;
		long tilesToUpdate = 0L; // A mask of tiles that change colour.
        // Find tiles to change owner in each direction:
		for (int[] direction : DIRECTIONS) {
			int x = m.row(), y = m.col();
			long candidateTiles = 0L; // A mask of positions that might be changing colour.
			boolean opponentTokens = false; // Found an opponent-owned tile along this direction.
            boolean directionFinished = false; // Finished scanning this direction from x,y.
			while (!directionFinished) {
				x += direction[0];
				y += direction[1];
				long pos = bitRepOf(x, y); // A bit representation of row=x and col=y
				if (!outsideBoard(x, y)) {
					if ((pos & opponentTiles) != 0) {
						opponentTokens = true;
						candidateTiles |= pos;
					} else if ((pos & playerTiles) != 0 && opponentTokens) {
						tilesToUpdate |= candidateTiles;
						directionFinished = true;
					} else { // Either empty or adjacent to move and the same colour as the move.
						directionFinished = true;
					}
				} else {
					directionFinished = true;
				}
			}
		}
		
		playerTiles |= tilesToUpdate; // Add the tiles that changed owner to the player.
		opponentTiles &= ~tilesToUpdate; // Remove the tiles that changed owned from the opponent.
		if (playerColor == TileState.DARK) {
			darkTiles = playerTiles;
			lightTiles = opponentTiles;
		} else {
			lightTiles = playerTiles;
			darkTiles = opponentTiles;
		}
		
		return Long.bitCount(tilesToUpdate);
	}

	@Override
	public List<Move> getAdjacentTiles(Move m, TileState color) {
		long adjacent = analytics.getAdjacentTiles(m.bitRep(), color, darkTiles, lightTiles);
		Move[] adjArr = new Move[Long.bitCount(adjacent)];
		int counter = 0;
		int countOfOnesSet = 64 - Long.numberOfLeadingZeros(adjacent);
		for (int i = Long.numberOfTrailingZeros(adjacent); i < countOfOnesSet; i++) {
			if (((0x1L << i) & adjacent) != 0L) {
				adjArr[counter] = MovePool.pool[i];
				counter++;
			}
		}
		return Arrays.asList(adjArr);
	}

	/*
	    Returns the bitmask of the board, with all the tiles
	    held by the player with colour playerColor as ones
	    and all else (empty and the other player) as zeroes.
	 */
	public long getBitBoardOf(TileState playerColor) {
		switch (playerColor) {
		case DARK:
			return darkTiles;
		case LIGHT:
			return lightTiles;
		default: // EMPTY
			return ~(darkTiles | lightTiles);
		}
	}

	private long getLegalMovesShiftLeft(final long SHIFT, final long MASK, final long currentBoard, final long opponentBoard, final long emptyBoard) {
		long legal = 0L;
		long potentialMoves = (currentBoard << SHIFT) & MASK & opponentBoard;
		while (potentialMoves != 0L) {
			long tmp = (potentialMoves << SHIFT) & MASK;
			legal |= tmp & emptyBoard;
			potentialMoves = tmp & opponentBoard;
		}
		return legal;
	}

	private long getLegalMovesShiftRight(final long SHIFT, final long MASK, final long currentBoard, final long opponentBoard, final long emptyBoard) {
		long legal = 0L;
		long potentialMoves = (currentBoard >> SHIFT) & MASK & opponentBoard;
		while (potentialMoves != 0L) {
			long tmp = (potentialMoves >> SHIFT) & MASK;
			legal |= tmp & emptyBoard;
			potentialMoves = tmp & opponentBoard;
		}
		return legal;
	}

	@Override
	public int getStableTileCount(TileState playerColor) {
        return analytics.getStableTileCount(playerColor, darkTiles, lightTiles);
	}

	/*
	    Returns the owner of the tile at pos (dark, light, or empty).
	    It is assumed that pos has exactly one bit set to zero.
	 */
	private TileState getStateOf(long pos) {
		if ((pos & darkTiles) != 0L) {
            return TileState.DARK;
        } else if ((pos & lightTiles) != 0L) {
            return TileState.LIGHT;
        } else {
            return TileState.EMPTY;
        }
	}

	@Override
	public TileState getStateOf(Move tile) {
		return getStateOf(tile.bitRep());
	}

	/**
	 * Returns a list of all the moves that the player with
     * colour playerColor can make on the current board.
	 */
	public List<Move> getValidMoves(TileState playerColor) {
		final long validMoveMask = playerColor == TileState.DARK ? legalDarkMoves : legalLightMoves;
		Move[] validMoves = new Move[Long.bitCount(validMoveMask)];
		int counter = 0;
		int highestOneIndex = 64 - Long.numberOfLeadingZeros(validMoveMask);
		for (int i = Long.numberOfTrailingZeros(validMoveMask); i < highestOneIndex; i++) {
			if (((0x1L << i) & validMoveMask) != 0L) {
				validMoves[counter] = MovePool.pool[i];
				counter++;
			}
		}
		return Arrays.asList(validMoves);
	}

	/*
	    Returns the moves that the player with color playerColour
	    can make on the current board as a bitmask.
	 */
	public long getValidMovesAsBits(TileState playerColor) {
		return playerColor == TileState.DARK ? legalDarkMoves : legalLightMoves;
	}

	@Override
	public boolean isCorner(final int row, final int col) {
        return analytics.isCorner(row, col);
	}

	@Override
	public boolean isEdge(final int row, int col) {
        return analytics.isEdge(row, col);
	}

	@Override
	public boolean isValidMove(final Move m, final TileState playerColor) {
		long validMoves = playerColor == TileState.DARK ? legalDarkMoves : legalLightMoves;
		return (validMoves & m.bitRep()) != 0L;
	}

	@Override
	public int makeMove(final Move m, final TileState playerColor) {
		if (!isValidMove(m, playerColor)) {
			return 0;
		}
		long moveAsBits = m.bitRep();

		// Has a tile on a corner of the board been played?
		if (!analytics.cornerPlayed() && analytics.isCorner(moveAsBits)) {;
			analytics.setEdgePlayed();
            analytics.setCornerPlayed();
		}
		// Has an a tile on an edge (incl. corners) of the board been played?
		else if (!analytics.edgePlayed() && analytics.isEdge(moveAsBits)) {
            analytics.setEdgePlayed();
		}
		precedingMove = m;

		int flipped = 1; // The number of tiles added to the player's possession by their move.
		if (playerColor == TileState.LIGHT) {
			lightTiles |= moveAsBits;
			flipped += updateTileOwners(m, TileState.LIGHT);
			lightScore += flipped;
			darkScore -= (flipped - 1);
		} else {
			darkTiles |= moveAsBits;
			flipped += updateTileOwners(m, TileState.DARK);
			darkScore += flipped;
			lightScore -= (flipped - 1);
		}

		updateLegalMoves();
        analytics.updateStableBoard(darkTiles, lightTiles);

		return flipped;
	}

	@Override
	public void print() {
		System.out.print("  ");
		for (char alphabet = 'a'; alphabet <= 'a' + boardSize - 1; alphabet++) {
            System.out.print("  " + alphabet);
        }
		System.out.println();
		for (int row = 0; row < boardSize; row++) {
			System.out.printf("%2d ", row+1);
			List<String> rowList = new ArrayList<>(boardSize);
			for (int col = 0; col < boardSize; col++) {

				if ((darkTiles & bitRepOf(row, col)) != 0) {
					rowList.add(" D ");
				} else if ((lightTiles & bitRepOf(row, col)) != 0) {
					rowList.add(" L ");
				} else {
					rowList.add("   ");
				}
			}
            rowList.forEach(System.out::print);
			System.out.println();
		}
	}

	private void updateLegalMoves() {
		this.legalDarkMoves = updateLegalMoves(TileState.DARK);
		this.legalLightMoves = updateLegalMoves(TileState.LIGHT);
	}
	
	/**
	 * Returns the legal moves available to the player with colour playerColor as a bit mask.
	 */
	private long updateLegalMoves(final TileState playerColor) {
		long playerTiles = playerColor == TileState.DARK ? darkTiles : lightTiles;
		long opponentTiles = playerColor == TileState.DARK ? lightTiles : darkTiles;
		long emptyTiles = ~(lightTiles | darkTiles);
		long[] legalArr = new long[8];
		
		// UP:
		legalArr[0] = getLegalMovesShiftRight(boardSize, DOWN_MASK, playerTiles, opponentTiles, emptyTiles);
		// DOWN:
		legalArr[1] = getLegalMovesShiftLeft(boardSize, UP_MASK, playerTiles, opponentTiles, emptyTiles);
		// LEFT:
		legalArr[2] = getLegalMovesShiftRight(1L, RIGHT_MASK, playerTiles, opponentTiles, emptyTiles);
		// RIGHT:
		legalArr[3] = getLegalMovesShiftLeft(1L, LEFT_MASK, playerTiles, opponentTiles, emptyTiles);
		// UP LEFT:
		legalArr[4] = getLegalMovesShiftRight(boardSize + 1L, RIGHT_MASK & DOWN_MASK, playerTiles, opponentTiles, emptyTiles);
		// UP RIGHT:
		legalArr[5] = getLegalMovesShiftRight(boardSize - 1L, LEFT_MASK & DOWN_MASK, playerTiles, opponentTiles, emptyTiles);
		// DOWN LEFT:
		legalArr[6] = getLegalMovesShiftLeft(boardSize - 1L, RIGHT_MASK & UP_MASK, playerTiles, opponentTiles, emptyTiles);
		// DOWN RIGHT:
		legalArr[7] = getLegalMovesShiftLeft(boardSize + 1L, LEFT_MASK & UP_MASK, playerTiles, opponentTiles, emptyTiles);
		
		return (legalArr[0] | legalArr[1] | legalArr[2] | legalArr[3]
				| legalArr[4] | legalArr[5] | legalArr[6] | legalArr[7]);
	}

	public boolean edgePlayed() {
        return analytics.edgePlayed();
    }

	public boolean cornerPlayed() {
        return analytics.cornerPlayed();
    }
}
