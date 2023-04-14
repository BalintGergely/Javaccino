package net.balintgergely.robotics.hm;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class HackMatchState {
	public static final byte
		EMPTY = 0,
		GARBAGE = 1,
		TILE_LEAST = 2,
		TILE_R = 2,
		TILE_G = 3,
		TILE_B = 4,
		TILE_M = 5,
		TILE_Y = 6,
		SUPER_LEAST = 7,
		SUPER_R = 7,
		SUPER_G = 8,
		SUPER_B = 9,
		SUPER_M = 10,
		SUPER_Y = 11;
	public static final int
		FLIP_OFF = 1,
		GRAB_OFF = 1 + 6;
	public static final int WIDTH = 6, HEIGHT = 10;
	private final byte[][] board;
	private final byte hand;
	private final HackMatchState parent;
	private final int latestMove;
	private volatile int score = -1;
	public int getHeight(){
		return board.length;
	}
	public byte getAt(int x,int y){
		return board[y][x];
	}
	public HackMatchState getParent(){
		return parent;
	}
	public boolean hasLatestMove(){
		return latestMove != 0;
	}
	public boolean isLatestMoveSwap(){
		return latestMove < GRAB_OFF;
	}
	public int getLatestMoveX(){
		return (latestMove - 1) % 6;
	}
	private HackMatchState(byte[][] board,byte hand,HackMatchState parent,int lastMove){
		this.board = board;
		this.hand = hand;
		this.parent = parent;
		this.latestMove = lastMove;
	}
	public HackMatchState(){
		this(new byte[0][],EMPTY,null,0);
	}
	public boolean isHoldingTile(){
		return hand != EMPTY;
	}
	public HackMatchState addBlockRow(byte[] row){
		byte[][] newBoard = new byte[getHeight() + 1][];
		System.arraycopy(board, 0, newBoard, 1, getHeight());
		newBoard[0] = Arrays.copyOf(row, WIDTH);
		return new HackMatchState(newBoard,hand,null,0);
	}
	public HackMatchState addBlockRowBottom(byte[] row){
		byte[][] newBoard = new byte[getHeight() + 1][];
		System.arraycopy(board, 0, newBoard, 0, getHeight());
		newBoard[getHeight()] = Arrays.copyOf(row, WIDTH);
		return new HackMatchState(newBoard,hand,this,0);
	}
	public int getColumnHeight(int x){
		int y = board.length;
		while(y > 0){
			y--;
			if(board[y][x] != EMPTY){
				return y + 1;
			}
		}
		return 0;
	}
	public HackMatchState flip(int x){
		int h = getColumnHeight(x);
		int y0 = h - 1;
		int y1 = h - 2;
		if(h < 2){
			return this;
		}else{
			byte v0 = board[y0][x];
			byte v1 = board[y1][x];
			if(v0 == v1){
				return this;
			}
			byte[][] newBoard = board.clone();
			newBoard[y0] = newBoard[y0].clone();
			newBoard[y1] = newBoard[y1].clone();
			newBoard[y0][x] = v1;
			newBoard[y1][x] = v0;
			return new HackMatchState(newBoard,hand,this,FLIP_OFF + x);
		}
	}
	public HackMatchState move(int x,int y){
		return grabOrDrop(x).grabOrDrop(y);
	}
	public HackMatchState grabOrDrop(int x){
		int h = getColumnHeight(x);
		int y = hand == EMPTY ? h - 1 : h;
		if(y < 0){
			return this;
		}
		byte[][] newBoard;
		if(y == board.length){
			newBoard = new byte[y + 1][];
			System.arraycopy(board, 0, newBoard, 0, y);
			newBoard[y] = new byte[6];
		}else{
			newBoard = board.clone();
			newBoard[y] = newBoard[y].clone();
		}
		byte newHand = newBoard[y][x];
		newBoard[y][x] = hand;
		return new HackMatchState(newBoard, newHand, this, GRAB_OFF + x);
	}
	public int getScore(){
		int blockScore = score;
		if(blockScore == -1){
			int[] scAssist = new int[6];
			blockScore = 0;
			for(int y = 0;y < getHeight();y++){
				int x = 0;
				int dx = 0;
				int kindScore = 0;
				byte kind = 0;
				boolean scNeighborFlag = false;
				while(dx <= 6){
					if(dx == 6 || board[y][dx] != kind){
						for(int k = x;k < dx;k++){
							scAssist[k] = kindScore;
						}
						x = dx;
						if(kind >= SUPER_LEAST){
							kindScore *= 3;
						}
						if(kind < TILE_LEAST){
							kindScore = 0;
						}
						if(blockScore < kindScore){
							blockScore = kindScore;
						}
						if(dx == 6){
							break;
						}
						kind = board[y][dx];
						kindScore = 0;
						scNeighborFlag = false;
					}
					kindScore++;
					if(y != 0 && kind == board[y-1][dx]){
						if(!scNeighborFlag){
							kindScore += scAssist[dx];
						}
						scNeighborFlag = true;
					}else{
						scNeighborFlag = false;
					}
					dx++;
				}
			}
			score = 1000 + blockScore - getHeight();
		}
		return score;
	}
}
