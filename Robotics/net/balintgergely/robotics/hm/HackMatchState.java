package net.balintgergely.robotics.hm;

public final class HackMatchState {

	// Gameplay notes:
	// The game never generates new layers such that a region of 4 is formed.
	// New layers are generated before being seen and may become part of a match.
	// EXA NINA's attack does not affect the brick held by the EXA.
	// Matching SUPERs will leave the brick held by the EXA, as well as other supers of the same type.
	// Garbage remembers it's original color.
	// Reverted garbage can form a match "midair" before falling.

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
		COL_5 = COL_0 >>> 5,
		ROW_0 = 0b111111_000000_000000_000000_000000_000000_000000_000000_000000_000000_0000L,
		ROW_1 = ROW_0 >>> 6,
		ROW_9 = ROW_0 >>> 6 * 9,
		BOARD = 0b111111_111111_111111_111111_111111_111111_111111_111111_111111_111111_0000L,
		HAND  = 0b000000_000000_000000_000000_000000_000000_000000_000000_000000_000000_1000L,
		EXA   = 0b000000_000000_000000_000000_000000_000000_000000_000000_000000_000000_0111L,
		BOARD_HAND = BOARD | HAND;
	
	public static final long
		FLAG_TOUCHES_TOP_ROW = 0b000000_000000_000000_000000_000000_000000_000000_000000_000000_000000_1000L;

	private final long b0,b1,b2,b3;

	private static long col(int col){
		return COL_0 >>> col;
	}
	private static long row(int row){
		return ROW_0 >>> (WIDTH * row);
	}

	private HackMatchState(long b0,long b1,long b2,long b3){
		this.b0 = b0; this.b1 = b1; this.b2 = b2; this.b3 = b3;
	}

	public static final HackMatchState EMPTY = new HackMatchState(0, 0, 0, 0);

	public long occupied(){
		return (b0 | b1 | b2 | b3) & BOARD_HAND;
	}
	public long empty(){
		return ~b0 & ~b1 & ~b2 & ~b3 & BOARD_HAND;
	}
	public long unknown(){
		return b0 & b1 & b2 & b3 & BOARD_HAND;
	}
	public long garbage(){
		return b0 & ~b1 & ~b2 & ~b3 & BOARD_HAND;
	}
	public long known(){
		return occupied() & ~unknown() & BOARD_HAND;
	}
	public long supers(){
		return b0 & (b1 | b2 | b3) & ~(b1 & b2 & b3) & BOARD_HAND;
	}
	public boolean handOccupied(){
		return (occupied() & HAND) != 0;
	}

	public int getEXALocation(){
		return (int)(b0 & EXA) - 1;
	}
	public HackMatchState setEXALocation(int pos){
		return new HackMatchState((b0 & ~EXA) | ((pos + 1) & EXA), b1, b2, b3);
	}

	private HackMatchState set(long bit,byte encoding){
		long rb0 = this.b0 & ~bit | ((encoding >>> 3) & 1) * bit;
		long rb1 = this.b1 & ~bit | ((encoding >>> 2) & 1) * bit;
		long rb2 = this.b2 & ~bit | ((encoding >>> 1) & 1) * bit;
		long rb3 = this.b3 & ~bit | ((encoding >>> 0) & 1) * bit;
		return new HackMatchState(rb0, rb1, rb2, rb3);
	}
	private byte get(long bit){
		return (byte)(
			  ((b0 & bit) == 0 ? 0 : 0b1000)
			| ((b1 & bit) == 0 ? 0 : 0b0100)
			| ((b2 & bit) == 0 ? 0 : 0b0010)
			| ((b3 & bit) == 0 ? 0 : 0b0001));
	}

	public HackMatchState setBrick(int x,int y,byte brick){
		return set(row(y) & col(x), brick);
	}
	public byte getBrick(int x,int y){
		return get(row(y) & col(x));
	}
	public HackMatchState setHand(byte brick){
		return set(HAND, brick);
	}
	public byte getHand(){
		return get(HAND);
	}
	public int freeLevelCount(){
		return (Long.numberOfTrailingZeros((b0 | b1 | b2 | b3) & BOARD) - 4) / 6;
	}

	public long swapTargetsInCol(int col){
		long x = Long.lowestOneBit(occupied() & col(col));
		return x << 6 | x;
	}
	private static long computeSwap(long m,long bit0,long bit1){
		boolean d = ((m & bit0) != 0) != ((m & bit1) != 0);
		return d ? (m ^ (bit0 | bit1)) : m;
	}
	public HackMatchState swap(int col){
		long sw1 = Long.lowestOneBit(occupied() & col(col));
		long sw0 = sw1 << 6;
		long rb0 = computeSwap(b0, sw0, sw1);
		long rb1 = computeSwap(b1, sw0, sw1);
		long rb2 = computeSwap(b2, sw0, sw1);
		long rb3 = computeSwap(b3, sw0, sw1);
		return new HackMatchState(rb0,rb1,rb2,rb3);
	}
	public boolean maySwap(int col){
		long sw0 = Long.lowestOneBit(occupied() & col(col)) << 6;
		
		return
			   (((b0 << 6) ^ b0) & sw0) != 0
			|| (((b1 << 6) ^ b1) & sw0) != 0
			|| (((b2 << 6) ^ b2) & sw0) != 0
			|| (((b3 << 6) ^ b3) & sw0) != 0;
	}
	private static long computeMove(long m,long bit0,long bit1){
		return (m & ~bit0) | ((m & bit0) == 0 ? 0 : bit1);
	}
	public long grabTargetInCol(int col){
		return Long.lowestOneBit(occupied() & col(col));
	}
	public HackMatchState grab(int col){
		long gb = Long.lowestOneBit(occupied() & col(col));
		long rb0 = computeMove(b0,gb,HAND);
		long rb1 = computeMove(b1,gb,HAND);
		long rb2 = computeMove(b2,gb,HAND);
		long rb3 = computeMove(b3,gb,HAND);
		return new HackMatchState(rb0, rb1, rb2, rb3);
	}
	public boolean colOccupied(int col){
		return (occupied() & col(col)) != 0;
	}
	public long dropTargetInCol(int col){
		return Long.highestOneBit(~occupied() & col(col));
	}
	public HackMatchState drop(int col){
		long tb = Long.highestOneBit(~occupied() & col(col));
		long rb0 = computeMove(b0,HAND,tb);
		long rb1 = computeMove(b1,HAND,tb);
		long rb2 = computeMove(b2,HAND,tb);
		long rb3 = computeMove(b3,HAND,tb);
		return new HackMatchState(rb0, rb1, rb2, rb3);
	}
	private static long detectMatch2(long board){
		long hmatches = ((board & ~COL_0 & BOARD) << 1) & board;
		long vmatches = ((board & ~ROW_0 & BOARD) << 6) & board;
		return hmatches | hmatches >>> 1 | vmatches | vmatches >>> 6;
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
			  processSuperPop(b0, ~b1 & ~b2 &  b3 & BOARD)
			| processSuperPop(b0, ~b1 &  b2 & ~b3 & BOARD)
			| processSuperPop(b0, ~b1 &  b2 &  b3 & BOARD)
			| processSuperPop(b0,  b1 & ~b2 & ~b3 & BOARD)
			| processSuperPop(b0,  b1 & ~b2 &  b3 & BOARD);
	}
	private static long flood(long x){
		return
			  (x & (~COL_0 & BOARD)) <<  1
			| (x & (~COL_5 & BOARD)) >>> 1
			| (x & (~ROW_0 & BOARD)) <<  6
			| (x & (~ROW_9 & BOARD)) >>> 6;
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
			  processSingleMatchAt(~b0 & ~b1 & ~b2 &  b3 & BOARD, mod)
			| processSingleMatchAt(~b0 & ~b1 &  b2 & ~b3 & BOARD, mod)
			| processSingleMatchAt(~b0 & ~b1 &  b2 &  b3 & BOARD, mod)
			| processSingleMatchAt(~b0 &  b1 & ~b2 & ~b3 & BOARD, mod)
			| processSingleMatchAt(~b0 &  b1 & ~b2 &  b3 & BOARD, mod);
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
			  processAnyMatch(~b0 & ~b1 & ~b2 &  b3 & BOARD)
			| processAnyMatch(~b0 & ~b1 &  b2 & ~b3 & BOARD)
			| processAnyMatch(~b0 & ~b1 &  b2 &  b3 & BOARD)
			| processAnyMatch(~b0 &  b1 & ~b2 & ~b3 & BOARD)
			| processAnyMatch(~b0 &  b1 & ~b2 &  b3 & BOARD);
	}
	private static long area(long board,long flow){
		while(true){
			long newFlow = flow | board & flood(flow);
			if(newFlow == flow){
				break;
			}
			flow = newFlow;
		}
		return flow;
	}
	private static int ratePositionalScore(long board,long mod){
		if((board & mod) == 0) return 0;
		mod = flood(mod) & board;
		board &= ~mod;

		int prevAreaScores = 0;
		int newAreaSize = 1;

		while(mod != 0){
			long flow = Long.lowestOneBit(mod);
			long a = area(board & ~mod, flow);
			mod &= ~a;
			int bc = Long.bitCount(a);
			newAreaSize += bc;
			prevAreaScores += bc * bc;
		}

		return newAreaSize * newAreaSize - prevAreaScores;
	}
	public int rateBrickPosition(long t){
		return
			  ratePositionalScore(~b0 & ~b1 & ~b2 &  b3 & BOARD, t)
			+ ratePositionalScore(~b0 & ~b1 &  b2 & ~b3 & BOARD, t)
			+ ratePositionalScore(~b0 & ~b1 &  b2 &  b3 & BOARD, t)
			+ ratePositionalScore(~b0 &  b1 & ~b2 & ~b3 & BOARD, t)
			+ ratePositionalScore(~b0 &  b1 & ~b2 &  b3 & BOARD, t);
	}
	private static final long COL_SHIFT = 0b000001_000001_000001_000001_000001_000001_000001_000001_000001_000001L;
	/**
	 * Pop all blocks specified by the pattern.
	 * All garbage in the vicinity will be transformed into unknown.
	 */
	public HackMatchState pop(long pop){
		
		long ga = b0 & ~b1 & ~b2 & ~b3;
		long unknown = ga & flood(pop);

		long b0 = this.b0 | unknown;
		long b1 = this.b1 | unknown;
		long b2 = this.b2 | unknown;
		long b3 = this.b3 | unknown;
			
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
			write = (write & ~readCols) | ((write & readCols) >>> 6);
		}

		return new HackMatchState(rb0, rb1, rb2, rb3);
	}
	public int rateState(){
		int specialCount = Long.bitCount(supers() & BOARD_HAND);
		int freeLevelCount = (Long.numberOfTrailingZeros(occupied() & BOARD) - 4) / 6;
		int emptyOrUnknownCount = Long.bitCount((unknown() | empty()) & BOARD_HAND);

		int freeLevelScore;
		switch(freeLevelCount){
			case 0: freeLevelScore = 0;
			case 1: freeLevelScore = 40000;
			case 2: freeLevelScore = 60000;
			case 3: freeLevelScore = 70000;
			default: freeLevelScore = 80000;
		}

		int emptyHandBonus = handOccupied() ? 0 : 1000000;

		return emptyOrUnknownCount * 50 + freeLevelScore + specialCount * 400 + emptyHandBonus;
	}
	@Override
	public boolean equals(Object obj){
		return obj instanceof HackMatchState that && this.b0 == that.b0 && this.b1 == that.b1 && this.b2 == that.b2 && this.b3 == that.b3;
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
				appendCode(row(y) & col(x), sb);
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
					HackMatchState state = EMPTY.setBrick(x,y,i);
					for(int ry = 0;ry < 10;ry++){
						for(int rx = 0;rx < 6;rx++){
							byte read = state.getBrick(rx,ry);
							assert read == (x == rx && y == ry ? i : 0);
						}
					}
				}
			}
		}
		HackMatchState state = HackMatchState.EMPTY;
		for(int y = 0;y < 10;y++){
			for(int x = 0;x < 6;x++){
				state = state.setBrick(x, y, BRICK_B);
			}
		}
		for(int y = 1;y < 10;y++){
			for(int x = 0;x < 6;x++){
				HackMatchState local = state;
				int z = 10;
				while(y + 1 < z){
					z--;
					local = local.setBrick(x, z, BRICK_EMPTY);
				}
				local = local.setBrick(x, y, BRICK_G);
				local = local.setBrick(x, y-1, BRICK_R);
				local = local.swap(x);
				for(int rx = 0;rx < 6;rx++){
					for(int ry = 0;ry < 10;ry++){
						if(rx != x || ry < y -1){
							assert local.getBrick(rx, ry) == BRICK_B;
						}else if(ry >  y){
							assert local.getBrick(rx, ry) == BRICK_EMPTY;
						}else if(ry == y){
							assert local.getBrick(rx, ry) == BRICK_R;
						}else if(ry == y - 1){
							assert local.getBrick(rx, ry) == BRICK_G;
						}
					}
				}
			}
		}

		state = new HackMatchState(0x0L, 0x4401810000000001L, 0x830420000000000L, 0x1550010000000001L);

		System.out.println(state);
		StateTree st0 = StateTree.start(state, 0xfffffffe);
		HackMatchState sx = st0.stateDirectlyAfter((byte)0x4);
		System.out.println(sx);
//		StateTree st1 = Stream.of(st0).mapMulti(StateTree::expand).filter(s -> s.getMoveCode() == 0).findAny().get();
//		BitboardState sx = st0.stateDirectlyAfter((byte)0);
		for(int i = 0;i < 1;i++){
			sx = sx.pop(sx.computeAnyPop() | sx.computeSuperPop());
		}
		System.out.println(sx);
	}
}
