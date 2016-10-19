package board;
import java.util.ArrayList;
import java.util.List;

public abstract class OthelloBoard {
	private static List<Move> corners; //A convenient list of all the corner positions
	protected static final int[][] DIRECTIONS = new int[][] {
		{0, -1},	//North
		{1, -1},	//Northeast
		{1, 0},		//East
		{1, 1},		//Southeast
		{0, 1},		//South
		{-1, 1},	//Southwest
		{-1, 0},	//West
		{-1, -1}    //Northwest
	};
	
	protected final int boardSize;
	protected Move precedingMove;
	protected int darkScore = 2, lightScore = 2;
	
	public OthelloBoard(int size) {
		boardSize = size;
	}
	
	public OthelloBoard(int size, boolean initializer) {
		boardSize = size;
		corners = new ArrayList<>(4);
		corners.add(MovePool.pool[0]);
		corners.add(MovePool.pool[boardSize - 1]);
		corners.add(MovePool.pool[(boardSize-1)*boardSize]);
		corners.add(MovePool.pool[MovePool.pool.length - 1]);
	}
		
	public abstract List<Move> getAdjacentTiles(final Move m, final TileState color);
	
	public int getBoardSize() {
		return boardSize;
	}
	
	/**
	 * Return a list of corner tiles.
	 */
	public List<Move> getCorners() {
		return corners;
	}
	
	public TileState getHighestScorer() {
		return darkScore > lightScore ? TileState.DARK: TileState.LIGHT;
	}

	/**
	 * Get the last move played on the board.
	 */
	public Move getLastMove() {
		return precedingMove;
	}
	
	public int getScoreOf(final TileState color) {
		switch (color) {
		case DARK:
			return darkScore;
		case LIGHT:
			return lightScore;
		default: //EMPTY
			return boardSize*boardSize - (darkScore + lightScore);
		}
	}
	
	public abstract int getStableTileCount(final TileState playerColor);
	
	public abstract TileState getStateOf(final Move tile);

	public int getTurnNumber() {
		return darkScore + lightScore - 4;
	}
	
	public abstract List<Move> getValidMoves(final TileState playerColor);
	public abstract boolean isCorner(int row, int col);
	public abstract boolean isEdge(int row, int col);
	public abstract boolean isValidMove(final Move m, final TileState playerColor);

	/**
	 * Modifies the board with the specified.
	 * @param m the tile being played.
	 * @param playerColor the color of the player making the move.
	 * @return The number of tiles added to the player's score (equal to
     * one plus the number of tiles flipped). If the move is not legal,
     * then 0 is returned instead.
	 */
	public abstract int makeMove(final Move m, final TileState playerColor);
	protected boolean outsideBoard(final int x) {
		return (x < 0 || x >= boardSize);
	}
	protected boolean outsideBoard(final int row, final int col) {
		return (outsideBoard(row) || outsideBoard(col));
	}
    /*
     * Prints a simple representation of the board's state to System.out.
     */
	public abstract void print();
}
