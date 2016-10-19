package board;

public class Move {
	private static int size = 8;
	public static void setBoardSize(final int boardSize) {
    	size = boardSize;
    }
	private final int row, col;
	private final long pos;
	
	public Move(final int row, final int column) {
		this.row = row;
		this.col = column;
		this.pos = 0x1L << ((row << 3) + col);
	}
	
	public Move(final long move) {
		int p = Long.numberOfLeadingZeros(move);
		this.row = p/8;
		this.col = p%8;
		this.pos = move;
	}
	
	/**
	 * Returns the bit representation of this as a long. Only works for 8x8 boards.
	 */
	public long bitRep() {
		return pos;
	}
	
	public int col() {
		return col;
	}
	
	@Override
    public boolean equals(Object obj) {
       if (!(obj instanceof Move))
            return false;
        if (obj == this)
            return true;

        Move rhs = (Move) obj;
        return rhs.row == row && rhs.col == col;
    }

    @Override
    public int hashCode() {
		return row*size + col;
    }
    
    public int row() {
		return row;
	}
    
    public String toString() {
		return (char)('a' + col) + "" + (row + 1);
	}
}
