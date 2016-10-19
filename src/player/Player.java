package player;
import board.Move;
import board.OthelloBoard;
import board.TileState;

public abstract class Player {
	private int score = 2;
	private final TileState color;
	
	public Player(TileState color) {
		this.color = color;
	}
	
	public void addToScore(int add) {
		if (score + add >= 0) {
            score = score + add;
        }
	}
	
	public TileState color() {
		return color;
	}
	
	@Override
    public boolean equals(Object obj) {
       if (!(obj instanceof Player)) {
		   return false;
	   }

        Player rhs = (Player) obj;
        return rhs.color == color && rhs.score == score;
    }
	
	public int getScore() {
		return score;
	}
	
	@Override
    public int hashCode() {
		if (isLightPlayer()) {
            return score;
        } else {
            return score + 1;
        }
    }
	
	private boolean isLightPlayer() {
		return color == TileState.LIGHT;
	}
	
	public abstract Move nextMove(OthelloBoard board);

    public void setScore(int score) {
		this.score = score;
	}
	
}
