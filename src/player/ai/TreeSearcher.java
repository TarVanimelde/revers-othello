package player.ai;

import board.Move;
import board.OthelloBitBoard;
import board.TileState;

import java.util.List;
import java.util.concurrent.Callable;

public class TreeSearcher implements Callable<TreeSearchResult> {
	private final int depthGoal; //How deep this searcher wants to search
	private final OthelloBitBoard board; //The game state represented by the root of the tree
	private final TileState rootPlayerColor; //Color of the player selecting a move

	public TreeSearcher(OthelloBitBoard board, TileState playerColor, int depth) {
		this.depthGoal = depth;
		this.board = board;
		this.rootPlayerColor = playerColor;
	}

	/**
	 * Implementation of NegaMax with Alpha-Beta pruning.
	 * 
	 * @param node
	 *            The State we are currently parsing.
	 * @param alpha
	 *            The alpha bound for alpha-beta pruning.
	 * @param beta
	 *            The beta bound for alpha-beta pruning.
	 * @param depth
	 *            The current depth we are at.
	 * @param playerColor
	 *            The player making a move.
	 * @return The best point count we can get on this branch of the state space
	 *         to the specified depth.
	 */
	private double AlphaBetaWithMemory(TDMCNode node, double alpha, double beta,
			int depth, TileState playerColor) {
		if (Thread.interrupted()) {
            return 0;
        }
		if (depth == 0 || node.isTerminal()) {
			if (playerColor == rootPlayerColor) {
				return node.eval();
			} else {
				return -node.eval();
			}
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

		List<TDMCNode> children = node.getChildren();
		// Do shorter depth search first to order moves on the longer search
		for (int depthToSearch : depthsToSearch) {
			for (TDMCNode child : children) {
				if (Thread.interrupted()) {
                    return 0; /* Results are no longer relevant. */
                }
				double newValue;
				newValue = -AlphaBetaWithMemory(child, -beta, -alpha, depthToSearch - 1, child.getCurrentPlayerColor());
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

	@Override
	public TreeSearchResult call() {
		return new TreeSearchResult(depthGoal, findBestMove(board, rootPlayerColor));
	}

	/**
	 * Finds the optimal move among possible moves using the heuristic TDMCNode.eval(),
	 * tree traversal is done with a specialized alpha-beta pruning minmax algorithm.
	 */
	private Move findBestMove(OthelloBitBoard state, TileState playerColor) {
		TDMCNode root = new TDMCNode(state, playerColor);
		List<TDMCNode> children = root.getChildren();
		Move bestMove = null;
		double bestScore = Double.NEGATIVE_INFINITY;

		for (TDMCNode child : children) {
			if (Thread.interrupted()) {
                return bestMove; /* Out of time, return the best found so far. */
            }
			double score;
			//score = idiotsAlphaBeta(child, depthGoal, bestScore, Double.POSITIVE_INFINITY);
			//score = naiveMinMax(child, depthGoal);
			score = MTDF(child, child.eval(), depthGoal, rootPlayerColor);
			//System.out.println(score);
			if (bestMove == null || score > bestScore) {
				bestScore = score;
				bestMove = child.getMove();
				//System.out.println("Move: " + bestMove + ", Score: " + bestScore);
			} /*else if (Math.abs(bestScore - score) < 0.0001 && random.nextDouble() > 0.5){ //approximately the same: randomly choose one
				bestScore = score;
				bestMove = child.getMove();
			}*/
//			else {
//				//System.out.println("Move not picked: " + child.getMove() + ", Score: " + score);
//			}
		}
		return bestMove;
	}

	private double idiotsAlphaBeta(final TDMCNode node, final int depth, double lowerBound, double upperBound) {
		// Leaf node, evaluate and return score relative to this' player
		if (Thread.interrupted()) {
            return 0;
        }
		if (depth <= 0 || node.isTerminal()) {
			//System.out.printf("Last move: %s, value: %.3f\n", node.getMove(), node.eval());
			return node.eval();
		}
		List<TDMCNode> children = node.getChildren();
		TileState nextTurnPlayerColor = children.get(0).getCurrentPlayerColor();
		if (nextTurnPlayerColor == rootPlayerColor) { // Maximizing
			double max = lowerBound;
			for (TDMCNode child : children) {
				if (Thread.interrupted()) {
                    return 0;
                }
				double childVal = idiotsAlphaBeta(child, depth - 1, max, upperBound);
				//System.out.println("MaxChoices: " + max + ", " + childVal);
				max = Math.max(max, childVal);
				//System.out.println("Max: " + max);
				if (max > upperBound) {
                    return upperBound;
                }
			}
			return max;
		} else { // Minimizing player
			double min = upperBound;	
			for (TDMCNode child : children) {
				if (Thread.interrupted()) {
                    return 0;
                }
				double childVal = idiotsAlphaBeta(child, depth - 1, lowerBound, min);
				//System.out.println("MinChoices: " + min + ", " + childVal);
				min = Math.min(min, childVal);
				//System.out.println("Min: " + min);
				if (min > lowerBound) {
                    return lowerBound;
                }
			}
			return min;
		}
	}

	private double MTDF(TDMCNode root, double firstGuess, int depth, TileState playerColor) {
		double g = firstGuess;
		double beta;
		double upperbound = Double.POSITIVE_INFINITY;
		double lowerbound = Double.NEGATIVE_INFINITY;
		while (lowerbound < upperbound) {
			if (Thread.interrupted()) {
                return 0;
            }
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
}

/**
 * The result of a search depth, which contains the optimal move
 * found by the search and the depth to which the search went.
 */
class TreeSearchResult {
	private final int depth;
	private final Move move;

	public TreeSearchResult(final int d, final Move m) {
		depth = d;
		move = m;		
	}

	public int getDepth() {
		return depth;
	}

	public Move getMove() {
		return move;
	}
}