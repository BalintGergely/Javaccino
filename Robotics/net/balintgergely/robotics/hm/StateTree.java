package net.balintgergely.robotics.hm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Consumer;

public class StateTree{
	public static final byte
		ACTION_TYPE_MASK = 0x70,
		ACTION_COL_MASK  = 0x07,
		ACTION_NONE      = 0x00,
		ACTION_MOVE      = 0x10,
		ACTION_SWAP      = 0x20,
		ACTION_GRAB_DROP = 0x30;
	private final StateTree parent;
	private final HackMatchState state;
	private final int moveScore;
	private final byte moveCode;
	private final byte popCount;
	private final byte moveCount;
	private final boolean mayExpand;
	private StateTree(StateTree parent, HackMatchState state, byte moveCode, byte popCount, int moveScore, byte moveCount, boolean mayExpand){
		this.parent = parent;
		this.state = state;
		this.moveScore = moveScore;
		this.moveCode = moveCode;
		this.moveCount = moveCount;
		this.popCount = popCount;
		this.mayExpand = mayExpand;
	}
	public static StateTree start(HackMatchState state, int startingScore){
		return new StateTree(null, state, (byte)0, (byte)0, startingScore, (byte)0, true);
	}
	private static byte swapCode(int col){
		return (byte)(ACTION_SWAP | col);
	}
	private static byte grabDropCode(int col){
		return (byte)(ACTION_GRAB_DROP | col);
	}
	private StateTree evalState(HackMatchState ns, byte moveCode, byte popCount, int moveScore, byte moveCount, boolean mayExpand){
		/* 
		 * This is no longer scored in the last state due to what I term the "procrastination effect".
		 * 
		 * The program would make a plan to reach a state with no overflow.
		 * The move that resolves overflow is the last move.
		 * The program makes the first move of the plan and makes a new plan.
		 * The move that resolves overflow is the last move.
		 * ...and so on, handling overflow gets pushed back one move each time.
		 * 
		 * Death by procrastination.
		 */
		int pBad = Long.bitCount(state.occupied() & (HackMatchState.ROW_8 | HackMatchState.ROW_9));
		int nBad = Long.bitCount(ns.occupied() & (HackMatchState.ROW_8 | HackMatchState.ROW_9));
		int diff = pBad - nBad;
		if(diff < 0){
			moveScore += diff * 1000000;
		}else{
			moveScore += diff * (1000000 - moveCount * 10000);
		}
		return new StateTree(this, ns, moveCode, popCount, moveScore, moveCount, mayExpand);
	}
	private StateTree evalAction(
		HackMatchState ns,
		byte moveCode,
		long affectedBricks,
		int penalty,
		final boolean isGrab){
	
		int score = this.moveScore - penalty;
		byte nmc = (byte)(this.moveCount + 1);
		if(isGrab){
			return evalState(ns, moveCode, (byte)0, score, nmc, true);
		}
		long pop = ns.computeNormalPopAt(affectedBricks);
		byte popCount = 0;
		boolean firstScore = true;
		while(true){

			int localScore = Long.bitCount(pop & HackMatchState.BOARD);
			score += localScore * 100;
			if(localScore != 0 && firstScore){
				score -= 300; // We deduce 300 per initial match to prefer combos.
				firstScore = false;
			}

			long spop = ns.computeSuperPop();
			if(spop != 0 || (pop & HackMatchState.FLAG_TOUCHES_TOP_ROW) != 0){
				if(spop != 0){
					score -= 200;
				}

				if(((spop | pop) & HackMatchState.BOARD) != 0)
					ns = ns.pop((spop | pop) & HackMatchState.BOARD);
				
				popCount++;

				score += Long.bitCount(ns.unknown()) * 50;
				return evalState(ns, moveCode, popCount, score, nmc, false);
			}

			long unknown = ns.unknown();
			if(pop == 0){
				score += Long.bitCount(unknown) * 50;
				return evalState(ns, moveCode, popCount, score, nmc, unknown == 0);
			}

			ns = ns.pop(pop);
			popCount++;
			if(unknown != 0){
				score += Long.bitCount(ns.unknown()) * 50;
				return evalState(ns, moveCode, popCount, score, nmc, false);
			}

			pop = ns.computeAnyPop();
		}
	}
	public byte getMoveCode(){
		return moveCode;
	}
	public byte getPopCount(){
		return popCount;
	}
	public int getMoveCount(){
		return moveCount;
	}
	public HackMatchState getState(){
		return state;
	}
	public int getScore(){
		return moveScore;
	}
	public StateTree getParent(){
		return parent;
	}
	public HackMatchState stateDirectlyAfter(byte action){
		if((action & ACTION_TYPE_MASK) == ACTION_SWAP){
			return state.swap(action & ACTION_COL_MASK);
		}else if(state.handOccupied()){
			return state.drop(action & ACTION_COL_MASK);
		}else{
			return state.grab(action & ACTION_COL_MASK);
		}
	}
	public void expand(Consumer<StateTree> downstream){
		if(!mayExpand){
			return;
		}
		int exa = state.getEXALocation();
		for(int i = 0;i < 6;i++){
			if(state.maySwap(i)){
				long t = state.swapTargetsInCol(i);
				downstream.accept(
					evalAction(state.swap(i), swapCode(i), t, Math.abs(exa - i) + 1, false));
			}
		}
		if(state.handOccupied()){
			for(int i = 0;i < 6;i++){
				long t = state.dropTargetInCol(i);
				if(t != 0)
					downstream.accept(
						evalAction(state.drop(i), grabDropCode(i), t, Math.abs(exa - i) + 1, false));
			}
		}else{
			for(int i = 0;i < 6;i++){
				long t = state.grabTargetInCol(i);
				if(t != 0)
					downstream.accept(
						evalAction(state.grab(i), grabDropCode(i), t, Math.abs(exa - i) + 1, true));
			}
		}
	}
	public static byte decideMove(HackMatchState startState){
		StateTree start = start(startState, 0);
		int garbageCount = Long.bitCount(startState.garbage());
		int brickCount = Long.bitCount(startState.occupied());
		int freeLevels = startState.freeLevelCount();
		final int moveCap = 12 + garbageCount;
		final boolean avoidMatching = brickCount < 6 * 5 && 1 < freeLevels;
		PriorityQueue<StateTree> priorityQueue = new PriorityQueue<>(1000,Comparator.comparingInt(StateTree::getScore).reversed());
		HashMap<HackMatchState,StateTree> bestMap = new HashMap<>(10010);
		bestMap.put(start.getState(), start);
		priorityQueue.add(start);
		StateTree bestState = start;
		while(true){
			StateTree x = priorityQueue.poll();
			if(x == null){
				break;
			}
			if(bestState.getScore() < x.getScore()){
				bestState = x;
			}
			x.expand(y -> {
				if(bestMap.putIfAbsent(y.getState(), y) == null){
					if(y.getMoveCount() < moveCap && (bestMap.size() < 10000 || y.getMoveCount() < 5)){
						priorityQueue.add(y);
					}
				}
			});
		}

		if(bestState == start){
			// Fall back to improving the adjacency score.
			// Note that penalizing late-resolving overflow risks
			// is handled by us picking the best path for each state.
			bestState = null;
			int bestScore = Integer.MIN_VALUE;
			for(Map.Entry<HackMatchState,StateTree> entry : bestMap.entrySet()){
				HackMatchState state = entry.getKey();
				int entryScore = state.rateBrickAdjacency();
				entryScore += Long.bitCount(state.empty() | state.unknown()) * 5;
				switch(entry.getKey().freeLevelCount()){
					case 0: entryScore -= 200; break;
					case 1: entryScore -= 100; break;
				}
				if(bestScore < entryScore){
					bestScore = entryScore;
					bestState = entry.getValue();
				}
			}
			if(bestState == start){
				return ACTION_NONE;
			}
		}
		while(bestState.getParent() != start){
			bestState = bestState.getParent();
		}
		if(avoidMatching && bestState.getPopCount() != 0){
			return (byte)((bestState.getMoveCode() & ~ACTION_TYPE_MASK) | ACTION_MOVE);
		}
		return bestState.getMoveCode();
	}
	public static String formatAction(byte move){
		if(move == ACTION_NONE){
			return "N";
		}
		if((move & ACTION_TYPE_MASK) == ACTION_SWAP){
			return "X" + (move & ACTION_COL_MASK);
		}else{
			return "G" + (move & ACTION_COL_MASK);
		}
	}
	public String toString(){
		if(parent == null){
			return "";
		}
		return parent.toString() + " " + formatAction(moveCode);
	}
	public static void main(String[] atgs){
		HackMatchState state =
			HackMatchState.EMPTY
				.setBrick(0, 0, HackMatchState.BRICK_R)
				.setBrick(1, 0, HackMatchState.BRICK_R)
				.setBrick(2, 0, HackMatchState.BRICK_R)
				.setBrick(3, 0, HackMatchState.BRICK_G)
				.setBrick(3, 1, HackMatchState.BRICK_R);
		
		byte x = decideMove(state);

		System.out.println("Decision: " + formatAction(x));
	}
}
