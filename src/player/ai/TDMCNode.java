package player.ai;

import board.Move;
import board.OthelloBitBoard;
import board.OthelloBoard;
import board.TileState;

import java.util.Arrays;
import java.util.List;

public class TDMCNode extends Node {
	//Weight source: An Othello Evaluation Function Based on Temporal Difference
	//Learning using Probability of Winning , Osaki, Shibahara, Tajima, and Kotani
	private static final double[] INITIAL_WEIGHTS = new double[] {
			0,  0,        0,         0,        0,        0,        0,       0,
			0, -0.02231,  0.05583,   0.02004,  0.02004,  0.05583, -0.02231, 0,
			0,  0.05583,  0.10126,  -0.10927, -0.10927,  0.10126,  0.05583, 0,
			0,  0.02004, -0.10927,  -0.10155, -0.10155, -0.10927,  0.02004, 0,
			0,  0.02004, -0.10927,  -0.10155, -0.10155, -0.10927,  0.02004, 0,
			0,  0.05583,  0.10126,  -0.10927, -0.10927,  0.10126,  0.05583, 0,
			0, -0.02231,  0.05583,   0.02004,  0.02004,  0.05583, -0.02231, 0,
			0,  0,        0,         0,        0,        0,        0,       0
	};

	private static final double[] MIDDLE_WEIGHTS = new double[] {
			6.32711, -3.32813,  0.33907, -2.00512, -2.00512,  0.33907, -3.32813,  6.32711,
			-3.32813, -1.52928, -1.87550, -0.18176, -0.18176, -1.87550, -1.52928, -3.32813,
			0.33907, -1.87550,  1.06939,  0.62415,  0.62415,  1.06939, -1.87550,  0.33907,
			-2.00512, -0.18176,  0.62415,  0.10539,  0.10539,  0.62415, -0.18176, -2.00512,
			-2.00512, -0.18176,  0.62415,  0.10539,  0.10539,  0.62415, -0.18176, -2.00512,
			0.33907, -1.87550,  1.06939,  0.62415,  0.62415,  1.06939, -1.87550,  0.33907,
			-3.32813, -1.52928, -1.87550, -0.18176, -0.18176, -1.87550, -1.52928, -3.32813,
			6.32711, -3.32813,  0.33907, -2.00512, -2.00512,  0.33907, -3.32813,  6.32711
	};

	private static final double[] END_WEIGHTS = new double[] {
			5.50062, -0.17812, -2.58948, -0.59007, -0.59007, -2.58948, -0.17812,  5.50062,
			-0.17812,  0.96804, -2.16084, -2.01723, -2.01723, -2.16084,  0.96804, -0.17812,
			-2.58948, -2.16084,  0.49062, -1.07055, -1.07055,  0.49062, -2.16084, -2.58948,
			-0.59007, -2.01723, -1.07055,  0.73486,  0.73486, -1.07055, -2.01723, -0.59007,
			-0.59007, -2.01723, -1.07055,  0.73486,  0.73486, -1.07055, -2.01723, -0.59007,
			-2.58948, -2.16084,  0.49062, -1.07055, -1.07055,  0.49062, -2.16084, -2.58948,
			-0.17812,  0.96804, -2.16084, -2.01723, -2.01723, -2.16084,  0.96804, -0.17812,
			5.50062, -0.17812, -2.58948, -0.59007, -0.59007, -2.58948, -0.17812,  5.50062
	};

	//Weight source: Coevolutionary Temporal Difference Learning for Othello, Table III
//	private static final double[] WPC_EVOLVED_WEIGHTS = new double[] {
//		 1.02, -0.27,  0.55, -0.10,  0.08,  0.47, -0.38,  1.00,
//		-0.13, -0.52, -0.18, -0.07, -0.18, -0.29, -0.68, -0.44,
//		 0.55, -0.24,  0.02, -0.01, -0.01,  0.10, -0.13,  0.77,
//		-0.10, -0.10,  0.01, -0.01,  0.00, -0.01, -0.09, -0.05,
//		 0.05, -0.17,  0.02, -0.04, -0.03,  0.03, -0.09, -0.05,
//		 0.56, -0.25,  0.05,  0.02, -0.02,  0.17, -0.35,  0.42,
//		-0.25, -0.71, -0.24, -0.23, -0.08, -0.29, -0.63, -0.24,
//		 0.93, -0.44,  0.55,  0.22, -0.15,  0.74, -0.57,  0.97
//	};

	private static final long CORNER_MASK = 0x8100000000000081L;
	private List<TDMCNode> children;
	private boolean childrenComputed = false;
	private final OthelloBitBoard board;

	public TDMCNode(OthelloBitBoard state, TileState playerColor) {
		super(state.getBoardSize(), playerColor);
		board = state;
		if (board.getValidMovesAsBits(playerColor) == 0L) {
			if (board.getValidMovesAsBits(opponentColor) == 0L)
				return;
			playerColor = opponentColor;
			opponentColor = playerColor.opposite();
		}
	}

	private double estimateMobility() {
		return this.getChildren().stream()
				.mapToInt(child -> Long.bitCount(((OthelloBitBoard)child.getBoard()).getValidMovesAsBits(opponentColor)))
				.average()
				.orElse(0);
	}

	public double eval() {
		// Game end conditions:
		if (this.isTerminal() && board.getHighestScorer() == playerColor) {
            return Double.POSITIVE_INFINITY;
        } else if (this.isTerminal() && board.getHighestScorer() == opponentColor) {
            return Double.NEGATIVE_INFINITY;
        }
		Stage currentStage = getStage(board);
		final double[] newPositionWeights = getPositionWeights(currentStage);//NEW_WEIGHTS;
		double value = 0; //Total value for the player of this board configuration
		double legalMoveWeight = 0, mobilityWeight = 0, stableTilesWeight = 0, ownTilesWeight = 0;
		switch (currentStage) {
		case INITIAL:
			break; // Don't use feature weights for initial gameplay
		case MIDDLE:
			legalMoveWeight = -0.82;
			mobilityWeight = -0.063;
			stableTilesWeight = 3.88;
			ownTilesWeight = -0.25;
			break;
		case END:
			legalMoveWeight = 0.38;
			mobilityWeight = -0.16;
			stableTilesWeight = 3.6;
			ownTilesWeight = -0.41;
		}
		long playerBoard = board.getBitBoardOf(playerColor);
		long opponentBoard = board.getBitBoardOf(opponentColor);
		for (int i = 0; i < boardSize*boardSize; i++) {
			long pos = 0x1L << i;
			if ((playerBoard & pos) != 0L) {
				value += newPositionWeights[i];
			} else if ((opponentBoard & pos) != 0L) {
				value -= newPositionWeights[i];
			}
		}
		
		value += legalMoveWeight*(Long.bitCount(board.getValidMovesAsBits(playerColor)));
		value += stableTilesWeight*(board.getStableTileCount(playerColor));
		value += mobilityWeight*estimateMobility();
		value += ownTilesWeight*(board.getScoreOf(playerColor));
		return value;
	}

	@Override
	public OthelloBoard getBoard() {
		return board;
	}

	public List<TDMCNode> getChildren() {	
		if(!childrenComputed) {
			List<Move> validMoves = board.getValidMoves(playerColor);
			TDMCNode[] childrenArr = new TDMCNode[validMoves.size()];
			for (int i = 0; i < validMoves.size(); i++) {
				OthelloBitBoard b = new OthelloBitBoard(board);
				b.makeMove(validMoves.get(i), playerColor);
				childrenArr[i] = new TDMCNode(b, opponentColor);
			}
			children = Arrays.asList(childrenArr);
			childrenComputed = true;
		}
		return children;
	}

	private double[] getPositionWeights(Stage stage) {
		switch (stage) {
		case INITIAL:
			return INITIAL_WEIGHTS;
		case MIDDLE:
			return MIDDLE_WEIGHTS;
		case END:
			return END_WEIGHTS;
		default:
			return MIDDLE_WEIGHTS;
		}
	}

	private Stage getStage(OthelloBitBoard board) {
		if (twoCornersWithSameColor(board)) {
            return Stage.END;
        } else if (board.edgePlayed()) {
            return Stage.MIDDLE;
        } else {
            return Stage.INITIAL;
        }
	}

	/**
	 * Returns true if at least two corners of the board share the same color and are non-empty, false otherwise.
	 */
	private boolean twoCornersWithSameColor(OthelloBitBoard board) {
		long playerBoard = board.getBitBoardOf(playerColor);
		long opponentBoard = board.getBitBoardOf(opponentColor);
		return Long.bitCount(playerBoard & CORNER_MASK) > 1 || Long.bitCount(opponentBoard & CORNER_MASK) > 1;
	}
}