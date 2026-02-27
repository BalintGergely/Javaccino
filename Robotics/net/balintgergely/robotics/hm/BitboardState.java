package net.balintgergely.robotics.hm;

public final class BitboardState {

	// Gameplay notes:
	// The game never generates new layers such that a region of 4 is formed.
	// New layers are generated before being seen and may become part of a match.
	// EXA NINA's attack does not affect the brick held by the EXA.
	// Matching SUPERs will leave the brick held by the EXA, as well as other supers of the same type.
	// Garbage remembers it's original color.
	// Matching loop: Find match, revert garbage, apply gravity, repeat.

	public static final byte
		BRICK_EMPTY    = 0b0000,
		BRICK_GARBAGE  = 0b1000,
		BRICK_R        = 0b0001,
		BRICK_G        = 0b0010,
		BRICK_B        = 0b0011,
		BRICK_M        = 0b0100,
		BRICK_Y        = 0b0101,
		BRICK_SUPER_R  = 0b1001,
		BRICK_SUPER_G  = 0b1010,
		BRICK_SUPER_B  = 0b1011,
		BRICK_SUPER_M  = 0b1100,
		BRICK_SUPER_Y  = 0b1101,
		BRICK_UNKNOWN  = 0b1111;
	
	public static byte[] getBrickTypes(){
		return new byte[]{
			BRICK_EMPTY,
			BRICK_GARBAGE,
			BRICK_R,
			BRICK_G,
			BRICK_B,
			BRICK_M,
			BRICK_Y,
			BRICK_SUPER_R,
			BRICK_SUPER_G,
			BRICK_SUPER_B,
			BRICK_SUPER_M,
			BRICK_SUPER_Y,
			BRICK_UNKNOWN
		};
	}

	public static final int WIDTH = 6, HEIGHT = 10;
	public static final long
		COL_0 = 0b100000_100000_100000_100000_100000_100000_100000_100000_100000_100000_0000L,
		COL_5 = COL_0 >> 5,
		ROW_0 = 0b111111_000000_000000_000000_000000_000000_000000_000000_000000_000000_0000L,
		ROW_1 = ROW_0 >> 6,
		ROW_9 = ROW_0 >> 6 * 9,
		BOARD = 0b111111_111111_111111_111111_111111_111111_111111_111111_111111_111111_0000L,
		HAND  = 0b000000_000000_000000_000000_000000_000000_000000_000000_000000_000000_0001L;
	
	public static final long
		FLAG_TOUCHES_TOP_ROW = 0b000000_000000_000000_000000_000000_000000_000000_000000_000000_000000_1000L;

	private final long b0,b1,b2,b3;

	private static long col(int col){
		return COL_0 >>> col;
	}
	private static long row(int row){
		return ROW_0 >>> (WIDTH * row);
	}

	private BitboardState(long b0,long b1,long b2,long b3){
		this.b0 = b0; this.b1 = b1; this.b2 = b2; this.b3 = b3;
	}

	public static final BitboardState EMPTY = new BitboardState(0, 0, 0, 0);

	public long occupied(){
		return b0 | b1 | b2 | b3;
	}
	public long empty(){
		return ~b0 & ~b1 & ~b2 & ~b3;
	}
	public long unknown(){
		return b0 & b1 & b2 & b3;
	}
	public long known(){
		return occupied() & ~unknown();
	}
	public boolean handOccupied(){
		return (occupied() & ~BOARD) != 0;
	}

	private BitboardState set(long bit,byte encoding){
		long rb0 = this.b0 & ~bit | ((encoding >>> 3) & 1) * bit;
		long rb1 = this.b1 & ~bit | ((encoding >>> 2) & 1) * bit;
		long rb2 = this.b2 & ~bit | ((encoding >>> 1) & 1) * bit;
		long rb3 = this.b3 & ~bit | ((encoding >>> 0) & 1) * bit;
		return new BitboardState(rb0, rb1, rb2, rb3);
	}
	private byte get(long bit){
		return (byte)(
			  ((b0 & bit) == 0 ? 0 : 0b1000)
			| ((b1 & bit) == 0 ? 0 : 0b0100)
			| ((b2 & bit) == 0 ? 0 : 0b0010)
			| ((b3 & bit) == 0 ? 0 : 0b0001));
	}

	public BitboardState setBrick(int x,int y,byte brick){
		return set(row(y) & col(x), brick);
	}
	public byte getBrick(int x,int y){
		return get(row(y) & col(x));
	}
	public BitboardState setHand(byte brick){
		return set(HAND, brick);
	}
	public byte getHand(){
		return get(HAND);
	}

	private static long computeSwap(long m,long bit0,long bit1){
		boolean d = ((m & bit0) != 0) != ((m & bit1) != 0);
		return d ? (m ^ (bit0 | bit1)) : m;
	}
	public BitboardState swap(int col){
		long sw1 = Long.lowestOneBit(occupied() & col(col));
		long sw0 = sw1 << 6;
		long rb0 = computeSwap(b0, sw0, sw1);
		long rb1 = computeSwap(b1, sw0, sw1);
		long rb2 = computeSwap(b2, sw0, sw1);
		long rb3 = computeSwap(b3, sw0, sw1);
		return new BitboardState(rb0,rb1,rb2,rb3);
	}
	private static long computeMove(long m,long bit0,long bit1){
		return (m & ~bit0) | ((m & bit0) == 0 ? 0 : bit1);
	}
	public BitboardState grab(int col){
		long gb = Long.lowestOneBit(occupied() & col(col));
		long rb0 = computeMove(b0,gb,HAND);
		long rb1 = computeMove(b1,gb,HAND);
		long rb2 = computeMove(b2,gb,HAND);
		long rb3 = computeMove(b3,gb,HAND);
		return new BitboardState(rb0, rb1, rb2, rb3);
	}
	public boolean colOccupied(int col){
		return (occupied() & col(col)) != 0;
	}
	public long dropTargetInCol(int col){
		return Long.highestOneBit(~occupied() & col(col));
	}
	public BitboardState drop(int col){
		long tb = Long.highestOneBit(~occupied() & col(col));
		long rb0 = computeMove(b0,HAND,tb);
		long rb1 = computeMove(b1,HAND,tb);
		long rb2 = computeMove(b2,HAND,tb);
		long rb3 = computeMove(b3,HAND,tb);
		return new BitboardState(rb0, rb1, rb2, rb3);
	}
	public long swapTargetsInCol(int col){
		long x = Long.lowestOneBit(occupied() & col(col));
		return x << 6 | x;
	}
	private static long detectMatch2(long board){
		long hmatches = ((board & ~COL_0 & BOARD) << 1) & board;
		long vmatches = ((board & ~ROW_0 & BOARD) << 6) & board;
		return hmatches | hmatches >> 1 | vmatches | vmatches >> 6;
	}
	private long processSuperPop(long supers,long board){
		long sboard = supers & board;
		long superMatch = detectMatch2(sboard);
		if(superMatch != 0){
			return superMatch | (board & ~supers);
		}else{
			return 0;
		}
	}
	public long computeSuperPop(){
		return
			  processSuperPop(b0, ~b1 & ~b2 &  b3)
			| processSuperPop(b0, ~b1 &  b2 & ~b3)
			| processSuperPop(b0, ~b1 &  b2 &  b3)
			| processSuperPop(b0,  b1 & ~b2 & ~b3)
			| processSuperPop(b0,  b1 & ~b2 &  b3);
	}
	private static long flood(long x){
		return
			  (x & (~COL_0 & BOARD)) << 1
			| (x & (~COL_5 & BOARD)) >> 1
			| (x & (~ROW_0 & BOARD)) << 6
			| (x & (~ROW_9 & BOARD)) >> 6;
	}
	private static long processSingleFlow(long board,long flow){
		while(true){
			long newFlow = flow | board & flood(flow);
			if(newFlow == flow){
				break;
			}
			flow = newFlow;
		}
		long mx = 0;
		if((flow & ROW_0) != 0){
			mx |= FLAG_TOUCHES_TOP_ROW;
		}
		if(4 <= Long.bitCount(flow)){
			mx |= flow;
		}
		return mx;
	}
	private static long processSingleMatchAt(long board,long mod){
		mod &= board;
		return mod != 0 ? processSingleFlow(board, mod) : 0;
	}
	public long computeNormalPopAt(long mod){
		return
			  processSingleMatchAt(~b0 & ~b1 & ~b2 &  b3, mod)
			| processSingleMatchAt(~b0 & ~b1 &  b2 & ~b3, mod)
			| processSingleMatchAt(~b0 & ~b1 &  b2 &  b3, mod)
			| processSingleMatchAt(~b0 &  b1 & ~b2 & ~b3, mod)
			| processSingleMatchAt(~b0 &  b1 & ~b2 &  b3, mod);
	}
	private static long processAnyMatch(long board){
		long mx = 0;
		while(board != 0){
			long flow = Long.lowestOneBit(board);
			while(true){
				long newFlow = flow | board & flood(flow);
				if(newFlow == flow){
					break;
				}
				flow = newFlow;
			}
			if((flow & ROW_0) != 0){
				mx |= FLAG_TOUCHES_TOP_ROW;
			}
			if(4 <= Long.bitCount(flow)){
				mx |= flow;
			}
			board = board & ~flow;
		}
		return mx;
	}
	public long computeAnyPop(){
		return
			  processAnyMatch(~b0 & ~b1 & ~b2 &  b3)
			| processAnyMatch(~b0 & ~b1 &  b2 & ~b3)
			| processAnyMatch(~b0 & ~b1 &  b2 &  b3)
			| processAnyMatch(~b0 &  b1 & ~b2 & ~b3)
			| processAnyMatch(~b0 &  b1 & ~b2 &  b3);
	}
	private static final long COL_SHIFT = 0b000001_000001_000001_000001_000001_000001_000001_000001_000001_000001L;
	/**
	 * Pop all blocks specified by the pattern.
	 * All garbage in the vicinity will be transformed into unknown.
	 */
	public BitboardState pop(long pop){
		
		long ga = b0 & ~b1 & ~b2 & ~b3;
		long unknown = ga & flood(pop);

		long b0 = this.b0 | unknown;
		long b1 = this.b1 | unknown;
		long b2 = this.b2 | unknown;
		long b3 = this.b2 | unknown;
			
		long rb0 = b0 & HAND;
		long rb1 = b1 & HAND;
		long rb2 = b2 & HAND;
		long rb3 = b3 & HAND;

		long write = ROW_0;
		for(int i = 0;i < 10;i++){
			long read = row(i) & ~pop;
			rb0 |= write & (b0 & read) * COL_SHIFT;
			rb1 |= write & (b1 & read) * COL_SHIFT;
			rb2 |= write & (b2 & read) * COL_SHIFT;
			rb3 |= write & (b3 & read) * COL_SHIFT;
			long readCols = read * COL_SHIFT;
			write = (write & ~readCols) | ((write & readCols) >> 6);
		}

		return new BitboardState(rb0, rb1, rb2, rb3);
	}
	public int rateState(){
		int specialCount = Long.bitCount(b0 & ~(b1 | b2 | b3));
		int freeLevelCount = (Long.numberOfTrailingZeros((b0 | b1 | b2 | b3) & BOARD) - 4) / 6;
		int emptyOrUnknownCount = Long.bitCount(unknown() | empty());

		int freeLevelScore;
		switch(freeLevelCount){
			case 0: freeLevelScore = 0;
			case 1: freeLevelScore = 100;
			case 2: freeLevelScore = 150;
			case 3: freeLevelScore = 175;
			default: freeLevelScore = 200;
		}

		int emptyHandBonus = handOccupied() ? 5 : 0;

		return emptyOrUnknownCount + freeLevelScore + specialCount * 10 + emptyHandBonus;
	}
	@Override
	public boolean equals(Object obj){
		return obj instanceof BitboardState that && this.b0 == that.b0 && this.b1 == that.b1 && this.b2 == that.b2 && this.b3 == that.b3;
	}
	@Override
	public int hashCode(){
		return ((Long.hashCode(b0) * 31 + Long.hashCode(b1)) * 31 + Long.hashCode(b2)) * 31 + Long.hashCode(b3);
	}
	private void appendCode(long bit,StringBuilder sb){
		sb.append((b0 & bit) == 0 ? '0' : '1');
		sb.append((b1 & bit) == 0 ? '0' : '1');
		sb.append((b2 & bit) == 0 ? '0' : '1');
		sb.append((b3 & bit) == 0 ? '0' : '1');
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int y = 0;y < 10;y++){
			for(int x = 0;x < 6;x++){
				if(x != 0) sb.append(' ');
				appendCode(row(y) | col(x), sb);
			}
			sb.append(System.lineSeparator());
		}
		sb.append("Hand: ");
		appendCode(HAND, sb);
		return sb.toString();
	}

	public static void main(String[] atgs){
		
		for(byte i = 0;i < 0x10;i++){
			for(int y = 0;y < 10;y++){
				for(int x = 0;x < 6;x++){
					BitboardState state = EMPTY.setBrick(x,y,i);
					for(int ry = 0;ry < 10;ry++){
						for(int rx = 0;rx < 6;rx++){
							byte read = state.getBrick(rx,ry);
							if(read != (x == rx && y == ry ? i : 0)){
								throw new RuntimeException();
							}
						}
					}
				}
			}
		}
		System.out.println("Testing done.");
	}
}
