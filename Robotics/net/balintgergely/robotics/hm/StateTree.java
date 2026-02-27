package net.balintgergely.robotics.hm;

import java.util.function.Consumer;

public class StateTree{
	public static final byte
		ACTION_TYPE_MASK = 0x7,
		ACTION_SWAP      = 0x0,
		ACTION_GRAB_DROP = 0x8;
	private final StateTree parent;
	private final BitboardState state;
	private final int moveScore;
	private final byte moveCode;
	private final boolean mayExpand;
	private StateTree(StateTree parent, BitboardState state, byte moveCode, int moveScore, boolean mayExpand){
		this.parent = parent;
		this.state = state;
		this.moveScore = moveScore;
		this.moveCode = moveCode;
		this.mayExpand = mayExpand;
	}
	public static StateTree start(BitboardState state, int startingScore){
		return new StateTree(null, state, (byte)0, startingScore, true);
	}
	private static byte swapCode(int col){
		return (byte)(ACTION_SWAP | col);
	}
	private static byte grabDropCode(int col){
		return (byte)(ACTION_GRAB_DROP | col);
	}
	private void evalAction(BitboardState ns, byte moveCode, long affectedBricks, Consumer<StateTree> downstream){
		int score = this.moveScore - 1;
		if(affectedBricks == 0){
			downstream.accept(new StateTree(this, ns, moveCode, score, true));
			return;
		}
		long pop = ns.computeNormalPopAt(affectedBricks);
		while(true){
			int localScore = Long.bitCount(pop & BitboardState.BOARD);
			if(4 < localScore){
				score += (localScore - 4) * 10;
			}

			long spop = ns.computeSuperPop();
			if(spop != 0 || (pop & BitboardState.FLAG_TOUCHES_TOP_ROW) != 0){
				ns = ns.pop(spop | pop);
				downstream.accept(new StateTree(this, ns, moveCode, score, false));
				break;
			}

			long unknown = ns.unknown();
			if(pop == 0){
				downstream.accept(new StateTree(this, ns, moveCode, score, unknown == 0));
				break;
			}

			ns = ns.pop(pop);
			if(unknown != 0){
				downstream.accept(new StateTree(this, ns, moveCode, score, false));
			}

			pop = ns.computeAnyPop();
		}
	}
	public byte getMoveCode(){
		return moveCode;
	}
	public BitboardState getState(){
		return state;
	}
	public int getScore(){
		return moveScore;
	}
	public StateTree getParent(){
		return parent;
	}
	public void expand(Consumer<StateTree> downstream){
		if(!mayExpand){
			return;
		}
		for(int i = 0;i < 6;i++){
			long sa = state.swapTargetsInCol(i);
			if((sa << 6) != 0)
				evalAction(state.swap(i), swapCode(i), sa, downstream);
		}
		if(state.handOccupied()){
			for(int i = 0;i < 6;i++){
				long t = state.dropTargetInCol(i);
				if(t != 0)
					evalAction(state.drop(i), grabDropCode(i), t, downstream);
			}
		}else{
			for(int i = 0;i < 6;i++){
				if(state.colOccupied(i))
					evalAction(state.grab(i), grabDropCode(i), 0, downstream);
			}
		}
	}
}
