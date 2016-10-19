package player.ai;

import board.Move;
import board.OthelloBoard;
import board.OthelloRegularBoard;
import board.TileState;
import player.Player;

import java.util.List;
import java.util.Random;

public class BasicAIPlayer extends Player {
	private final Random random = new Random();

	public BasicAIPlayer(TileState color) {
		super(color);
	}

	private double alphabeta(BasicNode node, int depth, double alpha, double beta, boolean maximizingPlayer) {
		if (depth <= 0 || node.isTerminal())
			return node.eval();

		if (maximizingPlayer) {
			double v = Double.NEGATIVE_INFINITY;
			for (BasicNode child : node.getChildren()) {
				v = Math.max(v,  alphabeta(child, depth - 1, alpha, beta, false));
				alpha = Math.max(alpha, v);
				if (beta <= alpha) {
                    break;
                }
			}
			return v;

		} else {
			double v = Double.POSITIVE_INFINITY;
			for (BasicNode child : node.getChildren()) {
				v = Math.min(v, alphabeta(child, depth - 1, alpha, beta, true));
				beta = Math.min(beta, v);
				if (beta <= alpha)
					break;
			}
			return v;
		}
	}

	private double AlphaBetaWithMemory(BasicNode node, double alpha, double beta,
			int depth, TileState playerColor) { // throws OutOfTimeException {
		// Is this state/our search done?
		if (depth == 0 || node.isTerminal()) {
			if (playerColor == this.color()) {
				return node.eval();
			} else {
				return -node.eval();
			}
			//int value = color * h;
			//return saveAndReturnState(state, alpha, beta, depth, value, color);
		}

		double bestValue = Double.NEGATIVE_INFINITY;

		// Partial move ordering. Check value up to depth D-3 and order by that
		int[] depthsToSearch;
		if (depth > 4) {
			depthsToSearch = new int[2];
			depthsToSearch[0] = depth - 2;
			depthsToSearch[1] = depth;
		} else {
			depthsToSearch = new int[1];
			depthsToSearch[0] = depth;
		}

		List<BasicNode> children = node.getChildren();
		// Do our shorter depth search first to order moves on the longer search
		for (int depthToSearch : depthsToSearch) {
                for (BasicNode child : children) {
                    double newValue = -AlphaBetaWithMemory(child, -beta, -alpha, depthToSearch - 1, playerColor.opposite());
                    if (newValue > bestValue) {
                        bestValue = newValue;
                    }
                    if (bestValue > alpha) {
                        alpha = bestValue;
                    }
                    if (bestValue >= beta) {
                        break;
                    }
                }
        }
		return bestValue;
	}


	private Move estimateBestMove(OthelloRegularBoard board, TileState playerColor) {
		return minMaxMove(board, playerColor);
	}

	private Move iterative_deepening(BasicNode root) {
		List<BasicNode> children = root.getChildren();
		Move bestMove = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (BasicNode child : children) {
			double score = MTDF(child, child.eval(), 5, this.color());
			if (bestMove == null || score > bestScore) {
				bestScore = score;
				bestMove = child.getMove();
			} else if (Math.abs(bestScore - score) < 0.0001 && random.nextDouble() > 0.5){ //approximately the same
				bestScore = score;
				bestMove = child.getMove();
			}
		}
		return bestMove;
	}
	
	private Move minMaxMove(OthelloRegularBoard state, TileState playerColor) {
        BasicNode root = new BasicNode(state, playerColor);
        List<BasicNode> children = root.getChildren();
        if (children.size() == 1) {
            return children.get(0).getMove();
        } else if (state.getScoreOf(TileState.LIGHT) + state.getScoreOf(TileState.DARK) == 4) {//doesn't matter what you do on the first turn
            return children.get(new Random().nextInt(children.size())).getMove();
        }
		return iterative_deepening(root);
	}

	private double MTDF(BasicNode root, double firstGuess, int depth, TileState playerColor) {	//throws OutOfTimeException {
		//mtdCalls++;
		double g = firstGuess;
		double beta;
		double upperbound = Double.POSITIVE_INFINITY;
		double lowerbound = Double.NEGATIVE_INFINITY;

		while (lowerbound < upperbound) {
			if (g == lowerbound) {
				beta = g + 1;
			} else {
				beta = g;
			}
			// Traditional NegaMax call, just with different bounds
			g = -AlphaBetaWithMemory(root, beta - 1, beta, depth, playerColor);
			if (g < beta) {
				upperbound = g;
			} else {
				lowerbound = g;
			}
		}
		return g;
	}

	@Override
	public Move nextMove(OthelloBoard board) {
		Move best = estimateBestMove((OthelloRegularBoard)board, this.color());
		return best;
	}
}

