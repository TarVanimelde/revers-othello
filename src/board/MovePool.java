package board;
public class MovePool {
	public static Move[] pool;
	private static boolean initialized = false;
	private static int size = 8;
	
	public static void initialize(final int boardSize) {
		if (initialized) {
			return;
		}
		size = boardSize;
		pool = new Move[boardSize * boardSize];
		for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                pool[row * boardSize + col] = new Move(row, col);
            }
        }
		initialized = true;
	}
	
	public static Move move(int row, int col) {
		return pool[row*size + col];
	}
}
