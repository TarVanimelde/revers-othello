package player.ai;
import board.Move;
import board.OthelloBoard;
import board.TileState;

public abstract class Node {
	protected final int boardSize;
	protected final TileState playerColor;
	protected TileState opponentColor;
	
	public Node(final int boardSize, final TileState playerColor) {
		this.boardSize = boardSize;
		this.playerColor = playerColor;
		this.opponentColor = playerColor.opposite();
	}
	
	/**
	 * Returns the heuristic score of the current player given the state of the board.
	 */
	public abstract double eval();
	public abstract OthelloBoard getBoard();
	
	public Move getMove() {
		return getBoard().getLastMove();
	}
	
	public TileState getCurrentPlayerColor() {
		return playerColor;
	}
	
	public boolean isTerminal() {
		OthelloBoard board = getBoard();
		return (board.getValidMoves(TileState.DARK).isEmpty()
				&& board.getValidMoves(TileState.LIGHT).isEmpty());
	}
	
	public String toString() {
		return this.getMove().toString();
	}
}