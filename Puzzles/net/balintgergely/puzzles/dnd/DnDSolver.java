package net.balintgergely.puzzles.dnd;

import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.OptionalLong;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.PrintStream;

public class DnDSolver {
	private static final int[][] CONFIG_ARRAY;
	private static final int CONFIG_ARRAY_CHARACTERISTICS =
		Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED |
		Spliterator.IMMUTABLE | Spliterator.NONNULL |
		Spliterator.DISTINCT | Spliterator.SORTED;
	static {
		CONFIG_ARRAY = new int[9][];
		for(int i = 0;i <= 8;i++){
			final int bitCount = i;
			CONFIG_ARRAY[i] = IntStream.range(0,0x100)
				.filter(k -> Integer.bitCount(k) == bitCount)
				.toArray();
		}
	}
	/**
	 * Convert coordinates to an offset.
	 */
	public static int offset(int x,int y){
		return x + y * 8;
	}
	/**
	 * Convert coordinates to a cell.
	 */
	public static long cell(int x,int y){
		return 0x8000000000000000L >>> offset(x, y);
	}
	/**
	 * Tell if the cell at the given coordinates is set or not.
	 */
	public static boolean get(long data,int x,int y){
		return (data & cell(x,y)) != 0;
	}
	/**
	 * Produce a mask for the given row.
	 */
	public static long rowMask(int row){
		return 0xFF00000000000000L >>> (row * 8);
	}
	/**
	 * Produce a mask for the given column.
	 */
	public static long columnMask(int column){
		return 0x8080808080808080l >>> column;
	}
	/**
	 * Counts the number of walls in the specified column.
	 */
	public static byte countWallsInColumn(long data,int column){
		data = (data >>> (7 - column)) & 0x0101010101010101L;
		                              //  1 1 1 1 1 1 1 1
		data = (data + (data >>> 8)); //  - 2 - 2 - 2 - 2
		data = (data + (data >>> 16));//  - - - 4 - - - 4
		data = (data + (data >>> 32));//  - - - - - - - 8
		return (byte)(data & 0xF);
	}
	/**
	 * Tests the column count rule and the occupied cell rule.
	 */
	private static boolean testRow(DnDPuzzle puzzle,long walls,int row){
		for(int x = 0;x < 8;x++){
			int y = row;
			if(get(walls, x, y) && puzzle.getCell(x, y) != DnDPuzzle.CELL_EMPTY){
				return false;
			}
			int wallCount = countWallsInColumn(walls, x);
			int wallTarget = puzzle.getColumn(x);
			if(wallTarget < wallCount){
				return false;
			}
		}
		return true;
	}
	/**
	 * Sets each value in the input 8x8 matrix that is adjacent to an already set value.
	 */
	private static long spreadOneTile(long c){
		long lc = (c <<  1) & ~columnMask(7);
		long uc = (c <<  8);
		long dc = (c >>> 8);
		long rc = (c >>> 1) & ~columnMask(0);
		return lc | uc | dc | rc;
	}
	/**
	 * Tests the dead end rule.
	 */
	private static boolean testDeadEnds(DnDPuzzle puzzle,long walls,int row){
		for(int x = 0;x < 8;x++){
			int y = row;
			if(get(walls, x, y)){
				continue;
			}
			long c = cell(x, y);
			long w = spreadOneTile(c) & ~c;
			int exitCount = Long.bitCount(w & ~walls);
			if(exitCount == 0){
				return false;
			}
			boolean mustBeDeadEnd = puzzle.getCell(x, y) == DnDPuzzle.CELL_ENEMY;
			if(mustBeDeadEnd != (exitCount == 1)){
				return false;
			}
		}
		return true;
	}
	/**
	 * Tests the interconnected rule.
	 */
	private static boolean testInterconnected(long walls){
		long space = ~walls;
		long c = Long.highestOneBit(space);
		while(true){
			long nc = c | (spreadOneTile(c) & space);
			if(nc == c){
				return c == space;
			}else{
				c = nc;
			}
		}
	}
	/**
	 * A matrix with a 3x3 square of set bits at the top left corner.
	 */
	private static final long TREASURE_ROOM_MASK    = 0xE0E0E00000000000L;
	/**
	 * A matrix with a 2x2 square of set bits at the top left corner.
	 */
	private static final long ILLEGAL_CORRIDOR_MASK = 0xC0C0000000000000L;
	/**
	 * Computes the locations of the potential walls for the specified treasure room.
	 */
	private static long wallMaskForTreasureRoom(long treasureRoom){
		return spreadOneTile(treasureRoom) & ~treasureRoom;
	}
	/**
	 * Tests the treasure room rule and the no 2x2 spaces rule.
	 */
	private static boolean testTreasureAndHallway(DnDPuzzle puzzle,long walls){
		long space = ~walls;
		long treasureRooms = 0L;
		for(int y = 0;y < 6;y++){
			for(int x = 0;x < 6;x++){
				long room = TREASURE_ROOM_MASK >>> offset(x, y);
				if((space & room) == room){
					long roomWalls = wallMaskForTreasureRoom(room);
					int exitCount = Long.bitCount(space & roomWalls);
					if(exitCount != 1){
						return false;
					}
					int treasureCount = 0;
					for(int dy = 0;dy < 3;dy++){
						for(int dx = 0;dx < 3;dx++){
							if(puzzle.getCell(x + dx, y + dy) == DnDPuzzle.CELL_CHEST){
								treasureCount++;
							}
						}
					}
					if(treasureCount != 1){
						return false;
					}
					treasureRooms = treasureRooms | room;
				}
			}
		}
		for(int y = 0;y < 8;y++){
			for(int x = 0;x < 8;x++){
				if(puzzle.getCell(x, y) == DnDPuzzle.CELL_CHEST){
					if((cell(x,y) & treasureRooms) == 0){
						return false;
					}
				}
			}
		}
		space = space & ~treasureRooms;
		for(int y = 0;y < 7;y++){
			for(int x = 0;x < 7;x++){
				long icm = ILLEGAL_CORRIDOR_MASK >>> offset(x, y);
				if((space & icm) == icm){
					return false;
				}
			}
		}
		return true;
	}
	private static DnDPuzzle inputDnDPuzzle(PrintStream comm,Scanner sc){
		byte[] rows = new byte[8];
		byte[] cols = new byte[8];
		byte[] cells = new byte[8 * 8];
		comm.println("Please input the column headers");
		{
			String line = sc.nextLine();
			for(int i = 0;i < 8;i++){
				cols[i] = Byte.parseByte(line.substring(i,i+1));
			}
		}
		comm.println("Please input the row headers");
		{
			String line = sc.nextLine();
			for(int i = 0;i < 8;i++){
				rows[i] = Byte.parseByte(line.substring(i,i+1));
			}
		}
		comm.println("Input 8 characters per row. Use 'X' for enemies and 'T' for treasure chests.");
		for(int y = 0;y < 8;y++){
			String line = sc.nextLine();
			for(int x = 0;x < 8;x++){
				switch(line.charAt(x)){
					case 'X':
					case 'x': cells[offset(x, y)] = DnDPuzzle.CELL_ENEMY; break;
					case 'T': 
					case 't': cells[offset(x, y)] = DnDPuzzle.CELL_CHEST; break;
				}
			}
		}
		return new DnDPuzzle(rows, cols, cells);
	}
	private static void printPuzzle(PrintStream out,DnDPuzzle puzzle){
		out.print(" ");
		for(int x = 0;x < 8;x++){
			out.print(" " + puzzle.getColumn(x));
		}
		out.println();
		out.println();
		for(int y = 0;y < 8;y++){
			out.print(puzzle.getRow(y));
			for(int x = 0;x < 8;x++){
				switch(puzzle.getCell(x, y)){
					case DnDPuzzle.CELL_EMPTY: out.print("  "); break;
					case DnDPuzzle.CELL_CHEST: out.print(" T"); break;
					case DnDPuzzle.CELL_ENEMY: out.print(" X"); break;
				}
			}
			out.println();
		}
	}
	private static void printSolution(PrintStream out,long walls){
		for(int y = 0;y < 8;y++){
			for(int x = 0;x < 8;x++){
				out.print(get(walls, x, y) ? '#' : ' ');
			}
			out.println();
		}
	}
	@SuppressWarnings("unused")
	private static LongStream addCheckpoint(LongStream input,String name){
		AtomicInteger checkpoint = new AtomicInteger();
		input = input.peek(k -> {checkpoint.incrementAndGet();});
		input = input.onClose(() -> {System.out.println(name+": "+checkpoint.get());});
		return input;
	}
	@SuppressWarnings("unused")
	private static LongStream expect(LongStream input,long value){
		input = input.filter(k -> k == value);
		boolean success = input.findAny().isPresent();
		input.close();
		if(!success){
			System.out.println("Expectation failed!");
			throw new RuntimeException();
		}
		return LongStream.of(value);
	}
	public static void main(String[] atgs){
		
		DnDPuzzle puzzle;
		try(Scanner sc = new Scanner(System.in)){
			puzzle = inputDnDPuzzle(System.out, sc);
		}

		printPuzzle(System.out, puzzle);

		LongStream solver = LongStream.of(0L).unordered().parallel();

		for(int row = 0;row < 8;row++){
			final int finalRow = row;
			final int[] cf = CONFIG_ARRAY[puzzle.getRow(row)];
			final int offset = (7 - row) * 8;
			solver = solver.flatMap(l ->
				StreamSupport.intStream(Spliterators.spliterator(cf,CONFIG_ARRAY_CHARACTERISTICS),false)
				.unordered()
				.mapToLong(r -> l | (((long)r) << offset)));

			solver = solver.filter(k -> testRow(puzzle, k, finalRow));

			if(0 < row){
				solver = solver.filter(k -> testDeadEnds(puzzle, k, finalRow - 1));
			}
		}
		solver = solver.filter(k -> testDeadEnds(puzzle, k, 7));

		solver = solver.filter(l -> testInterconnected(l));

		solver = solver.filter(l -> testTreasureAndHallway(puzzle, l));

		OptionalLong result = solver.findAny();

		solver.close();

		if(result.isPresent()){
			long solution = result.getAsLong();
			printSolution(System.out, solution);
		}else{
			System.out.println("Solution not found.");
		}
	}
}
