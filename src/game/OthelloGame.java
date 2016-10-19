package game;

import board.*;
import player.ai.BasicAIPlayer;
import player.Player;
import player.ai.TDMCAIPlayer;

public class OthelloGame {
	private final Player darkPlayer, lightPlayer;
	private Player currentPlayer;
	private OthelloBoard board;

	public OthelloGame(int size, boolean humanPlayerLight) {
		Move.setBoardSize(size); 
		MovePool.initialize(size); // Object pooling for performance
		if (humanPlayerLight) {
			// Uncomment one of these for the AI to play against another AI:
			//lightPlayer = new BasicAIPlayer(TileState.LIGHT);
			lightPlayer = new TDMCAIPlayer(TileState.LIGHT);
			//lightPlayer = new HumanPlayer(TileState.LIGHT);
			if (size == 8) {
				darkPlayer = new TDMCAIPlayer(TileState.DARK);
				board = new OthelloBitBoard();
			} else {
				darkPlayer = new BasicAIPlayer(TileState.DARK);
				board = new OthelloRegularBoard(size);
			}
		} else {
			// Uncomment one of these for the AI to play against another AI:
			//darkPlayer = new BasicAIPlayer(TileState.DARK);
			darkPlayer = new TDMCAIPlayer(TileState.DARK);
			//darkPlayer = new HumanPlayer(TileState.DARK);
			if (size == 8) {
				lightPlayer = new TDMCAIPlayer(TileState.LIGHT);
				board = new OthelloBitBoard();
			} else {
				lightPlayer = new BasicAIPlayer(TileState.LIGHT);
				board = new OthelloRegularBoard(size);
			}
		}
		currentPlayer = darkPlayer;
	}

	private boolean gameFinished() {
		/*
			The game is over if both players are unable to make moves.
		 */
		return board.getValidMoves(darkPlayer.color()).isEmpty()
				&& board.getValidMoves(lightPlayer.color()).isEmpty();
	}

	/**
	 * Play a game of Othello. Alternates between dark and light players until the game is over.
	 */
	public void play() {
		print();
		while (!gameFinished()) {
			// If currentPlayer has no valid moves, swap currentPlayer:
			if (board.getValidMoves(currentPlayer.color()).isEmpty()) {
				if (currentPlayer.color() == TileState.DARK) {
					System.out.println("Dark player has no valid moves.");
				} else {
					System.out.println("Light player has no valid moves.");
				}
				updateCurrentPlayer();
			}
			Move move = currentPlayer.nextMove(board);
			board.makeMove(move, currentPlayer.color());
			System.out.println("Move played: " + move);
			currentPlayer.setScore(board.getScoreOf(currentPlayer.color()));
			updateCurrentPlayer();
			currentPlayer.setScore(board.getScoreOf(currentPlayer.color()));
			print();
		}
	}

	private void print() {
		board.print();
		System.out.printf("Score: Light %d - Dark %d\n", lightPlayer.getScore(), darkPlayer.getScore());
	}

	public void printOutcome() {
		System.out.println("Neither player has any valid moves remaining.");
		if (darkPlayer.getScore() > lightPlayer.getScore())
			System.out.println("Dark player wins!");
		else if (darkPlayer.getScore() == lightPlayer.getScore())
			System.out.println("It's a tie!");
		else
			System.out.println("Light player wins!");
	}

	/**
	 * Changes the current player from light to dark or vice versa.
	 */
	private void updateCurrentPlayer() {
		if (currentPlayer.equals(darkPlayer)) {
			System.out.println("Light player's turn");
			currentPlayer = lightPlayer;
		} else {
			System.out.println("Dark player's turn");
			currentPlayer = darkPlayer;
		}
	}
}