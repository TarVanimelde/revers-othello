package player;

import board.Move;
import board.MovePool;
import board.OthelloBoard;
import board.TileState;

import java.util.Scanner;

/*
    Simple class for parsing commandline moves from a human player.
 */
public class HumanPlayer extends Player {
	public HumanPlayer(TileState color) {
		super(color);
	}

	@Override
	public Move nextMove(OthelloBoard board) {
		@SuppressWarnings("resource")
		Scanner input = new Scanner(System.in);
		while (true) { //Keep scanning until you choose a valid move
			System.out.print("> ");
			String move = input.next();
			move = move.trim().toUpperCase();
			System.err.println(move);
			if (move.matches("\\p{Upper}\\d")) {
				int row = move.charAt(1) - '1';
				int col = move.charAt(0) - 'A';
				Move m = MovePool.move(row, col);
				if (board.isValidMove(m, this.color())) {
					return m;
				} else {
					System.out.println("Invalid move, try again:");
				}
			} else if (move.matches("\\p{Upper}\\d\\d")) {
				int row = Integer.parseInt(move.substring(1)) - 1;
				int col = move.charAt(0) - 'A';
				Move m = MovePool.move(row, col);
				if (board.isValidMove(m, this.color())) {
					return m;
				} else {
					System.out.println("Invalid move, try again:");
				}
			} else {
				System.out.println("Could not parse move, try again:");
			} 	
		}
	}
}