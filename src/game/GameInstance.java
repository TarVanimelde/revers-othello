package game;
public class GameInstance {
	public static void main(String[] args) {
		boolean humanPlayerLight = false; // Is the human player the light player?
		int size = 8; // size of the board
		// Parse command-line arguments:
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-l")) {
				humanPlayerLight = true;
			} else if (args[i].equals("-n")) {
				size = Integer.valueOf(args[i+1]);
				if (size % 2 != 0 || !(size >= 4 && size <= 26)) {
					System.out.println("Board size must be an even number between 4 and 26 (inclusive).");
					System.exit(0);
				}
			}
		}
		// Play the game:
		OthelloGame game = new OthelloGame(size, humanPlayerLight);
		game.play();
		game.printOutcome();
	}
}