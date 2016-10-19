package player.ai;

import board.Move;
import board.OthelloBoard;
import board.OthelloRegularBoard;
import board.TileState;

import java.util.ArrayList;
import java.util.List;

public class BasicNode extends Node {
	private final OthelloRegularBoard board;

	public BasicNode(OthelloRegularBoard state, TileState playerColor) {
		super(state.getBoardSize(), playerColor);
		board = state;
	}

	public List<BasicNode> getChildren() {
		ArrayList<BasicNode> children = new ArrayList<>();
		for (Move m : board.getValidMoves(playerColor)) {
			OthelloRegularBoard b = new OthelloRegularBoard(board);
			b.makeMove(m, playerColor);
			children.add(new BasicNode(b, opponentColor));
		}
		return children;
	}

	public double eval() {
		//TODO: placeholder
		//double legalMoveWeight = -0.82, mobilityWeight = -0.063,
		//		stableTilesWeight = 3.88, ownTilesWeight = -0.25;
		double weight = 0;
		//weight += legalMoveWeight*(board.getValidMoves(playerColor).size());
		//weight += stableTilesWeight*(board.getStableTiles(playerColor).size());
		//weight += mobilityWeight*(board.getFrontierTiles(playerColor).size());
		//weight += ownTilesWeight*(board.getScoreOf(playerColor));
		if (board.isCorner(getMove()))
			weight += 30;
		/*IntStream.range(0, board.getBoardSize()*board.getBoardSize())
			.map(tileNum -> {
				int row = tileNum/board.getBoardSize();
				int col = tileNum%board.getBoardSize();
				
				if (board.isCorner(row, col))
					return 5
					else if (board.isEdge(tile))
			});*/
		List<Move> corners = board.getCorners();
		weight += -10*corners.stream().flatMap(corner -> board.getAdjacentTiles(corner, playerColor).stream())
			.filter(tile -> tile.equals(getMove()))
			.count();
		weight += 0.23*(board.getScoreOf(playerColor) - board.getScoreOf(opponentColor));
		return weight;
	}

	@Override
	public OthelloBoard getBoard() {
		return board;
	}

}
