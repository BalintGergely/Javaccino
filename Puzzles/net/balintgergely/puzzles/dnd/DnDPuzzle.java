package net.balintgergely.puzzles.dnd;

public class DnDPuzzle {
	public static final byte
		CELL_EMPTY = 0,
		CELL_ENEMY = 1,
		CELL_CHEST = 2;
	private byte[] rows;
	private byte[] columns;
	private byte[] cells;
	public DnDPuzzle(byte[] rows,byte[] columns,byte[] cells){
		if(rows.length != 8 || columns.length != 8 || cells.length != 64){
			throw new IllegalArgumentException();
		}
		this.rows = rows.clone();
		this.columns = columns.clone();
		this.cells = cells.clone();
	}
	public byte getCell(int x,int y){
		return cells[y * 8 + x];
	}
	public byte getRow(int y){
		return rows[y];
	}
	public byte getColumn(int x){
		return columns[x];
	}
}
