package player.ai;

import board.Move;
import board.OthelloBitBoard;
import board.OthelloBoard;
import board.TileState;
import player.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TDMCAIPlayer extends Player {
    private Duration totalTimeRemaining = Duration.ofSeconds(119); //How much time the AI has to compute all its remaining moves during the game
	private int greatestDepthLastMove = 5;

	public TDMCAIPlayer(TileState color) {
		super(color);
		totalTimeRemaining = Duration.ofSeconds(119);
	}

	/**
	 * Returns the best move predicted by the search result with greatest depth within the time limit timeToFindMove.
	 */
	private Move timedMinMax(OthelloBitBoard board, TileState playerColor, Instant startTime, Duration timeToFindMove) {
		Move bestMove = new TreeSearcher(board, this.color(), 3).call().getMove(); // Basic 3-depth. If this is running out of time, there are other problems.
		int greatestDepthThisMove = 3;
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);
		ExecutorCompletionService<TreeSearchResult> completionPool = new ExecutorCompletionService<>(executor);
		// Temporary solution: want a shifting window of depths to consider. Currently hard resets to 3 upon failure:
		int lowerBound = Math.max(greatestDepthLastMove - 2, 4);
		int upperBound = lowerBound + 2;
		int remainingSearches = upperBound - lowerBound + 1;
		for (int depth = lowerBound; depth <= upperBound; depth++) {
			completionPool.submit(new TreeSearcher(board, playerColor, depth));
		}
		// While there's still time left, find the complete search with maximal depth:
		try {
			while (!executor.isTerminated() && remainingSearches > 0) {
				Duration timeSpent = Duration.between(startTime, Instant.now());
				Duration timeRemaining = timeToFindMove.minus(timeSpent);
				if (timeRemaining.toMillis() < 250L) {
					executor.shutdownNow();
					break;
				}
				Future<TreeSearchResult> future = completionPool.poll(25, TimeUnit.MILLISECONDS);
				if (future != null) {
					remainingSearches--;
					TreeSearchResult result = future.get();
					if (result.getDepth() > greatestDepthThisMove) {
						greatestDepthThisMove = result.getDepth();
						bestMove = result.getMove();
					}
				}
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		executor.shutdownNow();
		greatestDepthLastMove = greatestDepthThisMove;
        Logger.getLogger(TDMCAIPlayer.class.getName()).log(Level.INFO,
                "Depth: {0}, Time elapsed: {1}ms",
                new Object[]{greatestDepthLastMove, Duration.between(startTime, Instant.now()).toMillis()});
		return bestMove;
		
	}

	@Override
	public Move nextMove(OthelloBoard board) {
		Instant startTime = Instant.now();
		TDMCNode root = new TDMCNode((OthelloBitBoard)board, this.color());
		List<TDMCNode> children = root.getChildren();
        // If there's only one move possible, take it:
		if (children.size() == 1) {
            return children.get(0).getMove();
        }
        // It doesn't matter what you do on the first turn (because of symmetry):
        else if (board.getTurnNumber() == 0) {
            return children.get(new Random().nextInt(children.size())).getMove();
        }
		Duration timeForTurn = allocateTime(board); // How much time the AI is being given to complete this turn.
		Move bestMove = timedMinMax((OthelloBitBoard)board, this.color(), startTime, timeForTurn);
		Duration turnRuntime = Duration.between(startTime, Instant.now()); // How much time the AI actually used to complete the turn
		totalTimeRemaining = totalTimeRemaining.minus(turnRuntime);
        Logger.getLogger(TDMCAIPlayer.class.getName()).log(Level.INFO,
                "Time left for future AI moves: {0}s",
                totalTimeRemaining.getSeconds());
		return bestMove;
	}

	private Duration allocateTime(OthelloBoard board) {
		int currentTurn = board.getTurnNumber();
		int maxTurns = board.getBoardSize()*board.getBoardSize() - 4;
		int turnsRemaining = maxTurns - currentTurn;
		double millisAllocated = totalTimeRemaining.toMillis() * (turnsRemaining * (currentTurn - 10)) / (float)(maxTurns*maxTurns);
		millisAllocated = Math.max(millisAllocated, 700); // Always give at least 700ms.
		millisAllocated = Math.min(millisAllocated, 25000); // Make sure we don't go over the 30s per-turn time limit.
		Logger.getLogger(TDMCAIPlayer.class.getName()).log(Level.INFO,
                "Turn: {0}, Time allocated: {1}ms",
                new Object[] {currentTurn, Math.round(millisAllocated)});
		return Duration.ofMillis(Math.round(millisAllocated));
	}
}