package board;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * General Othello board implementation that works for any size of othello board.
 * For a board of size eight, the OthelloBitBoard implementation is significantly
 * faster.
 *
 */
public class OthelloRegularBoard extends OthelloBoard {
	private static Hashtable<Move, List<Move>> adjacentTiles;
	
	private TileState[][] board;
	private boolean[][] stableTiles; //1 if a tile is stable, 0 if not.
	private final int boardSize; //The board is nxn: this value is n
	private int width = 4; //The game starts in the center of the board; width will always be the min of boardSize and the furthest from the center*2 move played + 1
	private List<Move> validLightMoves = new ArrayList<>(),
			validDarkMoves = new ArrayList<>();//,
	private int lightScore = 2, darkScore = 2;
	private boolean edgePlayed = false, //Has an edge been played?
			cornerPlayed = false; //Has a corner been played?
	
	
	/**
	 * Whether each given edge is filled
	 */
	private boolean leftColumnFilled = false, //Has the left edge been filled?
	rightColumnFilled = false, //Has the right edge been filled?
	topRowFilled = false, //Has the top edge been filled?
	bottomRowFilled = false; //Has the bottom edge been filled?

	//This constructor is only called once
	public OthelloRegularBoard(final int size) {
		super(size, true);
		board = new TileState[size][size];
		stableTiles = new boolean[size][size];
		this.boardSize = size;
		for (TileState[] row : board) {
			Arrays.fill(row, TileState.EMPTY);
		}
		
		board[size/2 - 1][size/2 - 1] = TileState.LIGHT;
		board[size/2][size/2] = TileState.LIGHT;
		board[size/2][size/2 - 1] = TileState.DARK;
		board[size/2 - 1][size/2] = TileState.DARK;
		
		computeAdjacentTiles();
		updateValidMoves();
	}

	public OthelloRegularBoard(final OthelloRegularBoard oldBoard) {
		super(oldBoard.boardSize);
		boardSize = oldBoard.boardSize;
		width = oldBoard.width;
		board = new TileState[boardSize][boardSize];
		stableTiles = new boolean[boardSize][boardSize];
		for (int row = 0; row < boardSize; row++) {
			System.arraycopy(oldBoard.stableTiles[row], 0, stableTiles[row], 0, boardSize);
			System.arraycopy(oldBoard.board[row], 0, board[row], 0, boardSize);
		}
		validLightMoves.addAll(oldBoard.validLightMoves);
		validDarkMoves.addAll(oldBoard.validDarkMoves);
		lightScore = oldBoard.lightScore;
		darkScore = oldBoard.darkScore;
		leftColumnFilled = oldBoard.leftColumnFilled;
		rightColumnFilled = oldBoard.rightColumnFilled;
		topRowFilled = oldBoard.topRowFilled;
		bottomRowFilled = oldBoard.bottomRowFilled;
		cornerPlayed = oldBoard.cornerPlayed;
		edgePlayed = oldBoard.edgePlayed;
	}

	/**
	 * Returns the list of valid moves available to the player with colour playerColour.
	 */
	private List<Move> calculateValidMoves(final TileState playerColor) {
		TileState opponentColor = playerColor.opposite();
		Set<Move> validMoves = new HashSet<>(16);
		for (int row = Math.max(boardSize/2 - width/2, 0); row < Math.min(boardSize/2 + width/2, boardSize); row++) {
			for (int col = Math.max(boardSize/2 - width/2, 0); col < Math.min(boardSize/2 + width/2, boardSize); col++) {
				for (int[] direction : DIRECTIONS) {
					if (board[row][col] != TileState.EMPTY)
						continue; //Can't place token on a non-empty space
					int x = row, y = col;
					int opponentTokens = 0; //How many opponent tokens along this direction
					boolean finishedDirection = false, foundValidMove = false;
					while (!finishedDirection) {
						x += direction[0];
						y += direction[1];
						if (!outsideBounds(x, y)) {
							if (board[x][y] == opponentColor) {
								opponentTokens++;
							} else if (board[x][y] == playerColor && opponentTokens > 0) {

								validMoves.add(MovePool.move(row, col));
								finishedDirection = true;
								foundValidMove = true;
							} else { //Empty
								break;
							}
						} else {
							finishedDirection = true;
						}
					}
					if (foundValidMove)
						break;
				}
			}
		}
		List<Move> moves = new ArrayList<>(validMoves.size());
		moves.addAll(validMoves);
		return moves;
	}

	//Special function to check if the edges filled, and if so, mark them stable
	private void checkIfEdgesFilled() {
		if (topRowFilled && bottomRowFilled && leftColumnFilled && rightColumnFilled)
			return;
		
		//Adds all tiles in a row to stableTiles
		Consumer<Integer> fillRow = (Integer row) -> Arrays.fill(stableTiles[row], true);
		//Adds all tiles in a column to stableTiles
		Consumer<Integer> fillColumn = (Integer col) -> IntStream.range(0, boardSize)
				.forEach(row -> stableTiles[row][col] = true);
		//Are all the tiles in a row non-empty?
		Predicate<Integer> rowFilled = (Integer row) -> IntStream.range(0, boardSize)
				.allMatch(col -> board[row][col] != TileState.EMPTY);
		//Are all the tiles in a column non-empty?
		Predicate<Integer> columnFilled = (Integer col) -> IntStream.range(0, boardSize)
				.allMatch(row -> board[row][col] != TileState.EMPTY);
		if (!topRowFilled) {
			topRowFilled = rowFilled.test(0);		
			if (topRowFilled)
				fillRow.accept(0);
		}
		if (!bottomRowFilled) {
			bottomRowFilled = rowFilled.test(boardSize - 1);
			if (bottomRowFilled)
				fillRow.accept(boardSize - 1);
		}
		if (!leftColumnFilled) {
			leftColumnFilled = columnFilled.test(0);
			if (leftColumnFilled)
				fillColumn.accept(0);
		}
		if (!rightColumnFilled) {
			rightColumnFilled = columnFilled.test(boardSize - 1);
			if (rightColumnFilled)
				fillColumn.accept(boardSize - 1);
		}
	}

	/** 
	 * Compute the set of adjacent tiles for each tile in the board.
	 */
	private void computeAdjacentTiles() {
		Function<Move, List<Move>> mapper = (Move tile) ->  Arrays.stream(DIRECTIONS)
				.map(direction -> {
					if (!outsideBoard(tile.row() + direction[0], tile.col() + direction[1]))
						return MovePool.move(tile.row() + direction[0], tile.col() + direction[1]);
					else
						return null;
				})
				.filter(move -> move != null)
				.filter(candidate -> !outsideBoard(candidate))
				.collect(Collectors.toCollection(ArrayList::new));
		

		adjacentTiles = new Hashtable<>(boardSize*boardSize);
		IntStream.range(0, boardSize*boardSize)
			.mapToObj(tileNumber -> MovePool.move(tileNumber/boardSize, tileNumber%boardSize))
			.forEach(tile -> adjacentTiles.put(tile, mapper.apply(tile)));
	}

	public boolean equals(Object rhs) {
		if (rhs == null || !(rhs instanceof OthelloRegularBoard)) {
			return false;
		}
		OthelloRegularBoard b = (OthelloRegularBoard)rhs;
		if (b.darkScore != darkScore || b.lightScore != lightScore) {
            return false;
        }

		return Arrays.deepEquals(board, b.board);
	}

	private int flipStates(final Move m, final TileState playerColor) {
		TileState opponentColor = playerColor.opposite();
		int flipped = 0;
		ArrayList<Move> toFlip = new ArrayList<>(boardSize);
		for (int[] direction : DIRECTIONS) {
			int x = m.row(), y = m.col();
			boolean directionFinished = false, foundFlippable = false;
			int opponentTokens = 0; //How many opponent tokens along this direction
			while (!directionFinished) {
				x += direction[0];
				y += direction[1];
				if (!outsideBounds(x, y)) {
					if (board[x][y] == opponentColor) {
						opponentTokens++;
						toFlip.add(MovePool.move(x,y));
					} else if (board[x][y] == playerColor && opponentTokens > 0) {
						directionFinished = true;
						foundFlippable = true;
					} else { //Empty or adjacent to move and same token color
						directionFinished = true;
					}
				} else {
					directionFinished = true;
				}
			}
			if (foundFlippable) {
				for (Move flip : toFlip) {
					board[flip.row()][flip.col()] = playerColor;
					flipped++;
				}
			}
			toFlip.clear();
		}
		return flipped;
	}

	/**
	 * Return adjacent tiles that have the same color as the tile m
	 */
	public List<Move> getAdjacentTiles(final Move m, final TileState color) {
		return adjacentTiles.get(m).stream()
				.filter(adjTile -> board[adjTile.row()][adjTile.col()] == color)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public TileState[][] getBoard() {
		return board;
	}

	public List<Move> getFrontierTiles(final TileState playerColor) {
		return IntStream.range(0, boardSize*boardSize)
				.filter(pos -> board[pos/boardSize][pos%boardSize] == playerColor)
				.mapToObj(pos -> MovePool.move(pos/boardSize, pos%boardSize))
				.filter(candidate -> getAdjacentTiles(candidate, TileState.EMPTY).size() > 0)
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	@Override
	public int getStableTileCount(final TileState playerColor) {
		if (!cornerPlayed) //no stable tiles possible yet
			return 0;
		checkIfEdgesFilled(); //Special handling for if an entire edge is filled
		
		int numStable = 0;
		for (int row = 0; row < boardSize; row++) {
			for (int col = 0; col < boardSize; col++) {
				if (board[row][col] != playerColor) { //ignore tiles of the wrong color
					continue;
				} else if (stableTiles[row][col]) {
					numStable++;
					continue;
				} else if (isCorner(row, col)) { //automatically stable if it's a corner
					stableTiles[row][col] = true;
					row = 0;
					col = 0;
					numStable = 0;
					continue;
				} else if (isEdge(row, col)) { //if it's an edge, if at least one neighbor of the same color is stable, then it is also stable
					boolean stableEdge = getAdjacentTiles(MovePool.move(row, col), playerColor).stream()
							.anyMatch(adjTile -> isEdge(adjTile) && stableTiles[adjTile.row()][adjTile.col()]);
					if (stableEdge) {
						stableTiles[row][col] = true;
						row = 0;
						col = 0;
						numStable = 0;
						continue;
					} else {
						continue;
					}
				} else {
					long stableNeighbors = getAdjacentTiles(MovePool.move(row, col), playerColor).stream()
							.filter(adjTile -> stableTiles[adjTile.row()][adjTile.col()])
							.count();

					if (stableNeighbors == 3) {
						//special configuration where (isomorphic in rotation) it has the form,
						//where C is the candidate, S is known to be stable with the same color as C:
						//case 1:
						// S S
						// S C
						if (stableTiles[row][col-1] && stableTiles[row-1][col-1] && stableTiles[row-1][col]) {
							stableTiles[row][col] = true;
							row = 0;
							col = 0;
							numStable = 0;
							continue;
						}
						//case 2:
						// S S
						// C S
						else if (stableTiles[row-1][col] && stableTiles[row - 1][col + 1] && stableTiles[row][col + 1]) {
							stableTiles[row][col] = true;
							row = 0;
							col = 0;
							numStable = 0;
							continue;
						}
						//case 3:
						// C S
						// S S
						else if (stableTiles[row][col+1] && stableTiles[row+1][col+1] && stableTiles[row + 1][col]) {
							stableTiles[row][col] = true;
							row = 0;
							col = 0;
							numStable = 0;
							continue;
						}
						//case 4:
						// S C
						// S S
						else if (stableTiles[row + 1][col] && stableTiles[row + 1][col - 1] && stableTiles[row][col - 1]) {
							stableTiles[row][col] = true;
							row = 0;
							col = 0;
							numStable = 0;
							continue;
						}
					} else if (stableNeighbors >= 4) { //if a tile is adjacent to at least 4 stable tiles of the same color, then it is also stable
						stableTiles[row][col] = true;
						row = 0;
						col = 0;
						numStable = 0;
						continue;
					}

				}
			}
		}
		return numStable;
	}

	private TileState getStateOf(final int row, final int col) {
		return board[row][col];
	}
	
	@Override
	public TileState getStateOf(final Move tile) {
		return getStateOf(tile.row(), tile.col());
	}
	
	@Override
	public List<Move> getValidMoves(final TileState playerColor) {
		return playerColor == TileState.DARK ? Collections.unmodifiableList(validDarkMoves)
				: Collections.unmodifiableList(validLightMoves);
	}

	
	@Override
	public boolean isCorner(final int row, final int col) {
		return (row == 0 || row == boardSize - 1)
				&& (col == 0 || col == boardSize - 1);
	}

	public boolean isCorner(final Move tile) {
		return isCorner(tile.row(), tile.col());
	}
	
	public boolean isDiagonal(final int row, final int col) {
		return (row == col || ((boardSize - 1) - col == row));
	}

	@Override
	public boolean isEdge(final int row, final int col) {
		return ((row == 0
				|| col == 0
				|| row == boardSize - 1
				|| col == boardSize - 1));
	}

	private boolean isEdge(final Move tile) {
		return isEdge(tile.row(), tile.col());
	}

	@Override
	public boolean isValidMove(final Move m, final TileState playerColor) {
		switch(playerColor) {
		case DARK:
			return validDarkMoves.contains(m);
		case LIGHT:
			return validLightMoves.contains(m);
		default: //EMPTY
			return false;
		}
	}

	public int makeMove(final Move m, final TileState playerColor) {
		if (!isValidMove(m, playerColor))
			return 0;
		if (!cornerPlayed && isCorner(m)) {
			edgePlayed = true;
			cornerPlayed = true;
		} else if (!edgePlayed && isEdge(m)) {
			edgePlayed = true;
		}
		if (boardSize/2 - width/2 > 0 && (
				m.row()-1 <= boardSize/2 - width/2
				|| m.col()-1 <= boardSize/2 - width/2
				|| m.row()+1 >= boardSize/2 + width/2
				|| m.col()+1 >= boardSize/2 + width/2))
			width += 2;
		precedingMove = m;
		board[m.row()][m.col()] = playerColor;

		int flipped = 1;
		if (playerColor == TileState.LIGHT) {
			flipped += flipStates(m, TileState.LIGHT);
			lightScore += flipped;
			darkScore -= (flipped - 1);
		} else {
			flipped += flipStates(m, TileState.DARK);
			darkScore += flipped;
			lightScore -= (flipped - 1);
		}
		updateValidMoves();

		return flipped;
	}
	
	protected boolean outsideBoard(final int x) {
		return x < 0 || x >= boardSize;
	}

	protected boolean outsideBoard(final int row, final int col) {
		return outsideBoard(row) || outsideBoard(col);
	}

	private boolean outsideBoard(final Move m) {
		return outsideBoard(m.row(), m.col());
	}

	private boolean outsideBounds(final int x) {
		return x < boardSize/2 - width/2 || x > boardSize/2 + width/2 - 1;
	}

	private boolean outsideBounds(final int row, final int col) {
		return outsideBounds(row) || outsideBounds(col);
	}
	
	@Override
	public void print() {
		System.out.print("  ");
		for (char alphabet = 'A'; alphabet <= 'A' + boardSize - 1; alphabet++)
			System.out.print("   " + alphabet);

		String lineSep = "   +";
		for (int i = 0; i < boardSize; i++) {
			lineSep += "---+";
		}
		System.out.println();
		System.out.println(lineSep);
		for (int row = 0; row < boardSize; row++) {
			System.out.printf("%2d |", row+1);
			List<String> rowList = new ArrayList<>(boardSize);
			for (int col = 0; col < boardSize; col++) {

				if (board[row][col] == TileState.DARK) {
					rowList.add(" D |");
				} else if (board[row][col] == TileState.LIGHT) {
					rowList.add(" L |");
				} else {
					rowList.add("   |");
				}
			}
            rowList.forEach(System.out::print);
			System.out.println();
			System.out.println(lineSep);
		}
	}

	private void updateValidMoves() {
		validLightMoves = calculateValidMoves(TileState.LIGHT);
		validDarkMoves = calculateValidMoves(TileState.DARK);
	}

    /**
     * Has an edge position been played yet?
     */
    public boolean edgePlayed() {
        return edgePlayed;
    }

}