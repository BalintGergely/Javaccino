package net.balintgergely.puzzles.bp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Program to help solve a certain water pouring puzzle.
 */
@SuppressWarnings("unused")
public class LiquidPuzzle{
	private static final int
		FO = 0, RE = 1, AQ = 2, KI = 3,
		GR = 4, PO = 5, T1 = 6, T2 = 7, RT = 8;
	private static final State CAPACITY = State.of(12,14,6,3,5,9,4,4,6);
	private static final int TOTAL_LIQUID = 46;
	private static final record IntPair(int a,int b){
		public IntPair flip(){ return new IntPair(b, a); }
	}
	private static final class State implements Iterable<Integer>{
		private int[] array;
		private State(int[] values){
			this.array = values;
		}
		public PrimitiveIterator.OfInt iterator(){
			return IntStream.of(array).iterator();
		}
		public static State of(int... values){
			return new State(values.clone());
		}
		public State subtract(State that){
			if(that.array.length != this.array.length){
				throw new IllegalArgumentException();
			}
			return map((i,v) -> v - that.get(i));
		}
		public IntStream stream(){
			return IntStream.of(array);
		}
		public int get(int index){
			return array[index];
		}
		public int sum(){
			int s = 0;
			for(int i : array){
				s += i;
			}
			return s;
		}
		public int sumExcept(int index){
			return sum() - get(index);
		}
		public State append(int value){
			int[] newArray = Arrays.copyOf(array, array.length + 1);
			newArray[array.length] = value;
			return new State(newArray);
		}
		public State map(IntBinaryOperator op){
			int[] newArray = null;
			int index = 0;
			while(index < array.length){
				int newVal = op.applyAsInt(index, array[index]);
				if(newVal != array[index]){
					newArray = new int[array.length];
					System.arraycopy(array,0,newArray,0,index);
					newArray[index] = newVal;
					index++;
					break;
				}
				index++;
			}
			if(newArray == null){
				return this;
			}
			while(index < array.length){
				newArray[index] = op.applyAsInt(index, array[index]);
				index++;
			}
			return new State(newArray);
		}
		public boolean equals(Object obj){ return obj instanceof State that && Arrays.equals(this.array, that.array); }
		public int hashCode(){ return Arrays.hashCode(array); }
		public String toString(){ return Arrays.toString(array); }
	}
	private static final List<String> NAMES = List.of(
		"Fo", "Re", "Aq", "Ki", "Gr", "Po", "Left", "Right", "Reserve"
	);
	private static final Set<IntPair> LEGAL_MOVES;
	private static final Set<IntPair> LEGAL_MOVES_STEAM;
	static{
		HashSet<IntPair> moveSet = new HashSet<>();
		for(IntPair pair : List.of(
			new IntPair(FO, T1), new IntPair(FO, T2), new IntPair(RE, T1), new IntPair(RE, T2),
			new IntPair(AQ, T1), new IntPair(KI, T2), new IntPair(GR, T1), new IntPair(PO, T1)
		)){
			moveSet.add(pair);
			moveSet.add(pair.flip());
		}
		LEGAL_MOVES = Set.copyOf(moveSet);
		LEGAL_MOVES_STEAM = Set.of(new IntPair(RT, T1), new IntPair(T1, RT));
	}

	private record Path(Path prev,State rep,int from,int to,int cost){}

	private static final Comparator<Path> PATH_PRIORITY = Comparator.comparingInt(Path::cost);

	private static Path simpleMove(Path state,IntPair move){
		int fromIndex = move.a();
		int toIndex = move.b();
		int spaceInTarget = CAPACITY.get(toIndex) - state.rep().get(toIndex);
		int liquidInSource = state.rep().get(fromIndex);
		int liquidToMove = Math.min(liquidInSource,spaceInTarget);
		
		State newRep = state.rep().map((i,v) -> {
			if(i == fromIndex){
				v -= liquidToMove;
			}
			if(i == toIndex){
				v += liquidToMove;
			}
			return v;
		});

		return new Path(state,newRep,fromIndex,toIndex,state.cost() + liquidToMove + 2);
	}

	private static Stream<Path> allMoves(Collection<IntPair> moveset,Path s){
		return moveset.stream().map(p -> simpleMove(s, p));
	}

	private static Stream<State> allStates(){
		Stream<State> cumulative = Stream.of(State.of()).parallel();
		for(int i : CAPACITY){
			cumulative = cumulative.flatMap(s -> IntStream.range(0, i + 1).mapToObj(s::append));
		}
		return cumulative.filter(s -> s.sum() == TOTAL_LIQUID);
	}

	private static void printPath(Path state){
		if(state.prev() != null){
			printPath(state.prev());
			System.out.println(NAMES.get(state.from) + " => " + NAMES.get(state.to));
		}
		System.out.println(state.rep());
	}
	private static boolean endConditionTest(State state){
		State s = State.of(12, 6, 6, 3, 5, 0, 4, 4, 6);
		return s.equals(state);
	}
	public static void main(String[] atgs){

		// Parameters:

		State startState = State.of(12, 6, 6, 3, 5, 0, 4, 4, 6);
		boolean steamPowered = false;

		//
		
		Set<IntPair> legalMoves;
		if(steamPowered){
			legalMoves = new HashSet<>();
			legalMoves.addAll(LEGAL_MOVES);
			legalMoves.addAll(LEGAL_MOVES_STEAM);
			legalMoves = Set.copyOf(legalMoves);
		}else{
			legalMoves = LEGAL_MOVES;
		}

		int sum = startState.stream().sum();
		System.out.println("Sum of liquid in containers: " + sum + " (Expected: " + TOTAL_LIQUID + ")");

		if(sum != TOTAL_LIQUID){
			System.err.println("Bad sum error!");
			return;
		}
		// If none of the states meet the condition, we just explore the state-space.
		
		
		State endState = null;

		Path node = new Path(null, startState, 0, 0, 0);

		HashMap<State,Path> stateMap = new HashMap<>();
		PriorityQueue<Path> pathQueue = new PriorityQueue<>(PATH_PRIORITY);

		do{
			if(stateMap.putIfAbsent(node.rep(), node) == null){
				if(endConditionTest(node.rep())){
					endState = node.rep();
					break;
				}
				allMoves(legalMoves,node).forEachOrdered(pathQueue::add);
			}
			node = pathQueue.poll();
		}while(node != null);

		System.out.println("Explored " + stateMap.size() + " states.");
		if(endState == null){
			List<State> states = allStates().filter(s -> !stateMap.containsKey(s)).filter(LiquidPuzzle::endConditionTest).toList();
			if(states.isEmpty()){
				System.out.println("No legal state satisfies the condition.");
			}else{
				System.out.println(states.size() + " states satisfy the requirement. Example:");
				System.out.println(states.get(0));
				System.out.println("Unfortunately none are reachable.");
			}
		}else{
			printPath(stateMap.get(endState));
		}
	}
}
