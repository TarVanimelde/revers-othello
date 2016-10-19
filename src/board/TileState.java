package board;

public enum TileState {
	EMPTY, DARK, LIGHT;
	
	public TileState opposite() {
		return (this == DARK) ? LIGHT : DARK;
	}
}