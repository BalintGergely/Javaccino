package net.balintgergely.robotics.hm;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import net.balintgergely.robotics.Convolution;
import net.balintgergely.robotics.ImageUtils;
import net.balintgergely.robotics.Convolution.MatchResult;
import net.balintgergely.robotics.screen.CapturePanel;
import net.balintgergely.robotics.screen.ScreenFrame;

/**
 * Class responsible for interfacing with Hack & Match.
 * Both reading the game state and performing moves.
 */
public class HackMatchGameInterface{
	public static final int
		VISUAL_BRICK_WIDTH = 24,
		VISUAL_BRICK_HEIGHT = 24,
		VISUAL_BOARD_WIDTH = 24*6 + 1,//145
		VISUAL_BOARD_HEIGHT = VISUAL_BRICK_HEIGHT * 11 - 6,
		BOARD_PANEL_Y_OFFSET = 46,
		BOARD_PANEL_LEFT_OFFSET = 8,
		BOARD_PANEL_CENTER_OFFSET = 109,
		BOARD_PANEL_RIGHT_OFFSET = 211,
		EXA_LINE_Y = 238,
		EXA_BRICK_Y = 239,
		MODE_SINGLEPLAYER = 0,
		MODE_MULTIPLAYER_P1 = 1,
		MODE_MULTIPLAYER_P2 = 2,
		MODE_MULTIPLAYER_P2_NO_NUMPAD = 3;
	private static final BufferedImage ROW_PATTERN;
	static{
		try{
			ROW_PATTERN = ImageIO.read(HackMatchGameInterface.class.getResourceAsStream("RowPattern.png"));
		}catch(IOException e){
			throw new ExceptionInInitializerError(e);
		}
	}
	private Robot robot;
	private int scale = 1;
	private ScreenFrame screenFrame;
	private CapturePanel boardPanel;
	private ArrayBlockingQueue<Runnable> workerQueue;
	private Thread worker;
	private int mode = -1;
	public HackMatchGameInterface() throws AWTException{
		robot = new Robot();
		robot.setAutoDelay(0);
		screenFrame = new ScreenFrame(363, 313);
		screenFrame.setVisible(true);
		screenFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		screenFrame.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_SPACE){
					autoAlign();
				}
			}
		});
		boardPanel = new CapturePanel();
		boardPanel.setForeground(Color.BLUE);
		screenFrame.getClient().add(boardPanel);

		workerQueue = new ArrayBlockingQueue<>(50);
		worker = new Thread(this::workerRun, "HM Game interface robot worker");
		worker.start();
	}
	public void setMode(int mode){
		if(this.mode == mode){
			return;
		}
		int left = 0;
		switch(mode){
			case MODE_SINGLEPLAYER:
				left = BOARD_PANEL_CENTER_OFFSET;
				break;
			case MODE_MULTIPLAYER_P1:
				left = BOARD_PANEL_LEFT_OFFSET;
				break;
			case MODE_MULTIPLAYER_P2:
				left = BOARD_PANEL_RIGHT_OFFSET;
			case MODE_MULTIPLAYER_P2_NO_NUMPAD:
				left = BOARD_PANEL_RIGHT_OFFSET;
				break;
			default:
				throw new IllegalArgumentException("Bad mode!");
		}
		this.mode = mode;
		screenFrame.repaint();
		boardPanel.setBounds(left, BOARD_PANEL_Y_OFFSET, VISUAL_BOARD_WIDTH, VISUAL_BOARD_HEIGHT + 4);
	}
	private void workerRun(){
		try{
			while(true){
				workerQueue.take().run();
			}
		}catch(InterruptedException ex){
			Thread.currentThread().interrupt();
		}
	}
	private void sleep(long millis){
		try{
			Thread.sleep(millis);
		}catch(InterruptedException e){
			throw new RuntimeException(e);
		}
	}
	private int leftKey(){
		switch(mode){
			case MODE_SINGLEPLAYER:
			case MODE_MULTIPLAYER_P1:
				return KeyEvent.VK_A;
			case MODE_MULTIPLAYER_P2:
			case MODE_MULTIPLAYER_P2_NO_NUMPAD:
				return KeyEvent.VK_LEFT;
			default:
				return 0;
		}
	}
	private int rightKey(){
		switch(mode){
			case MODE_SINGLEPLAYER:
			case MODE_MULTIPLAYER_P1:
				return KeyEvent.VK_D;
			case MODE_MULTIPLAYER_P2:
			case MODE_MULTIPLAYER_P2_NO_NUMPAD:
				return KeyEvent.VK_RIGHT;
			default:
				return 0;
		}
	}
	private int downKey(){
		switch(mode){
			case MODE_SINGLEPLAYER:
			case MODE_MULTIPLAYER_P1:
				return KeyEvent.VK_S;
			case MODE_MULTIPLAYER_P2:
			case MODE_MULTIPLAYER_P2_NO_NUMPAD:
				return KeyEvent.VK_DOWN;
			default:
				return 0;
		}
	}
	private int grabKey(){
		switch(mode){
			case MODE_SINGLEPLAYER:
			case MODE_MULTIPLAYER_P1:
				return KeyEvent.VK_J;
			case MODE_MULTIPLAYER_P2:
				return KeyEvent.VK_NUMPAD0;
			case MODE_MULTIPLAYER_P2_NO_NUMPAD:
				return KeyEvent.VK_8;
			default:
				return 0;
		}
	}
	private int swapKey(){
		switch(mode){
			case MODE_SINGLEPLAYER:
			case MODE_MULTIPLAYER_P1:
				return KeyEvent.VK_K;
			case MODE_MULTIPLAYER_P2:
				return KeyEvent.VK_DELETE;
			case MODE_MULTIPLAYER_P2_NO_NUMPAD:
				return KeyEvent.VK_9;
			default:
				return 0;
		}
	}
	public void moveLeft(){
		workerQueue.add(() -> {
			robot.keyPress(leftKey());
			sleep(60);
			robot.keyRelease(leftKey());
			sleep(60);
		});
	}
	public void moveRight(){
		workerQueue.add(() -> {
			robot.keyPress(rightKey());
			sleep(60);
			robot.keyRelease(rightKey());
			sleep(60);
		});
	}
	public void grabOrDrop(){
		workerQueue.add(() -> {
			robot.keyPress(grabKey());
			sleep(60);
			robot.keyRelease(grabKey());
			sleep(60);
		});
	}
	public void swap(){
		workerQueue.add(() -> {
			robot.keyPress(swapKey());
			sleep(60);
			robot.keyRelease(swapKey());
			sleep(60);
		});
	}
	public void close(){
		worker.interrupt();
		workerQueue.clear();
		screenFrame.setVisible(false);
	}
	private void autoAlign(){
		BufferedImage sc = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
		int[] rowCounter = new int[sc.getHeight()];
		int[] colCounter = new int[sc.getWidth()];
		for(int y = 0;y < sc.getHeight();y++){
			for(int x = 0;x < sc.getWidth();x++){
				if(sc.getRGB(x, y) == 0xFF525959){
					rowCounter[y]++;
					colCounter[x]++;
				}
			}
		}
		Point p = screenFrame.getClientLocation();
		for(int i = 0;i < rowCounter.length;i++){
			if(300 < rowCounter[i]){
				p.y = i - 1;
				break;
			}
		}
		for(int i = 0;i < colCounter.length;i++){
			if(300 < colCounter[i]){
				p.x = i - 1;
				break;
			}
		}
		screenFrame.setClientLocation(p);
	}
	private BufferedImage makeScreenshot(){
		BufferedImage sc = robot.createScreenCapture(boardPanel.getBoundsOnScreen());
		return ImageUtils.copy(sc, BufferedImage.TYPE_INT_ARGB, scale);
	}
	private static byte readBrick(BufferedImage board,int x,int y){
		if(y < 19){
			switch(board.getRGB(x + 11, y + 19)){
				case 0xFF4A0013:
				case 0xFF2C0A14: return HackMatchState.BRICK_R;
				case 0xFF13004A:
				case 0xFF100A30: return HackMatchState.BRICK_B;
				case 0xFF0A4733:
				case 0xFF0C2D24: return HackMatchState.BRICK_G;
				case 0xFF4A003B:
				case 0xFF2C0A28: return HackMatchState.BRICK_M;
				case 0xFF604429:
				case 0xFF372C1F: return HackMatchState.BRICK_Y;
				case 0xFF921111:
				case 0xFF501213: return HackMatchState.BRICK_SUPER_R;
				case 0xFF03252A:
				case 0xFF081C20: return HackMatchState.BRICK_SUPER_G;
				case 0xFF080430:
				case 0xFF0B0C23: return HackMatchState.BRICK_SUPER_B;
				case 0xFF38002F:
				case 0xFF230A22: return HackMatchState.BRICK_SUPER_M;
				case 0xFF1B1907:
				case 0xFF14160E: return HackMatchState.BRICK_SUPER_Y;
				case 0xFF222428:
				case 0xFF181C1F: return HackMatchState.BRICK_GARBAGE;
				default: return (byte)0;
			}
		}else{
			switch(board.getRGB(x + 11, y)){
				case 0xFFFF7B8E: return HackMatchState.BRICK_R;
				case 0xFFCB5BFF: return HackMatchState.BRICK_B;
				case 0xFF95D5DC: return HackMatchState.BRICK_G;
				case 0xFFFFA9CC: return HackMatchState.BRICK_M;
				case 0xFFFFB893: return HackMatchState.BRICK_Y;
				case 0xFF721111: return HackMatchState.BRICK_SUPER_R;
				case 0xFF06765F: return HackMatchState.BRICK_SUPER_G;
				case 0xFF080430: return HackMatchState.BRICK_SUPER_B;
				case 0xFF38002F: return HackMatchState.BRICK_SUPER_M;
				case 0xFF1B1907: return HackMatchState.BRICK_SUPER_Y;
				case 0xFFD6F7FF: return HackMatchState.BRICK_GARBAGE;
				default: return (byte)0;
			}
		}
	}
	private static byte readEXAPos(BufferedImage board){
		for(byte x = 0;x < 6;x++){
			if(board.getRGB(x * VISUAL_BRICK_WIDTH + 10, EXA_LINE_Y) == 0xFFA8A1A3){
				return x;
			}
		}
		return -1;
	}
	private static HackMatchState readState(BufferedImage board){
		MatchResult alignment = Convolution.bestMatch(board, ROW_PATTERN);
		
		int baseY = alignment.y() % VISUAL_BRICK_HEIGHT + 1;
		if(5 < baseY){
			baseY -= VISUAL_BRICK_HEIGHT;
		}

		board.setRGB(0, baseY + VISUAL_BRICK_HEIGHT, 0x00FFFF);

		HackMatchState state = HackMatchState.EMPTY;
		
		for(int y = 0;y < 10;y++){
			for(int x = 0;x < 6;x++){
				state = state.setBrick(x, y, readBrick(board, x * VISUAL_BRICK_WIDTH + 1, baseY + y * VISUAL_BRICK_HEIGHT));
			}
		}

		int x = readEXAPos(board);
		if(0 <= x){
			state = state.setHand(readBrick(board, x * VISUAL_BRICK_WIDTH + 1, EXA_BRICK_Y));
			state = state.setEXALocation(x);
		}

		return state;
	}
	private void doReadState(HackMatchState p, CompletableFuture<HackMatchState> target){
		boolean isScrolling = false;
		try{
			while(!target.isDone()){
				BufferedImage bi = this.makeScreenshot();
				boolean hasMovement = false;
				for(int y = 0;y < bi.getHeight();y++){
					for(int x = 0;x < bi.getWidth();x++){
						if(bi.getRGB(x, y) == 0xFFFFFFFF){
							hasMovement = true;
						}
					}
				}
				HackMatchState state = null;
				if(!hasMovement){
					state = readState(bi);
					long oc = state.occupied() & HackMatchState.BOARD;
					if(((oc << 6) & ~oc) != 0){
						hasMovement = true; // We are detecting blocks that are "midair"
					}
				}
				boolean shouldBeScrolling = !hasMovement && state.equals(p);
				if(shouldBeScrolling){
					if(!isScrolling){
						robot.keyPress(downKey());
						isScrolling = true;
					}
				}else{
					if(isScrolling){
						robot.keyRelease(downKey());
						isScrolling = false;
					}
				}
				if(hasMovement || shouldBeScrolling){
					sleep(100);
				}else{
					target.complete(state);
				}
			}
		}finally{
			if(isScrolling){
				robot.keyRelease(downKey());
			}
		}
	}
	/**
	 * Reads the state and returns a CompletableFuture that will be completed
	 * with the new state. It is possible for the future to never be completed
	 * if an error occurs or the interface is closed.
	 * 
	 * @param seenState
	 * If this state is observed, the interface performs a scroll until a different
	 * state is observed.
	 */
	public CompletableFuture<HackMatchState> readState(HackMatchState seenState){
		CompletableFuture<HackMatchState> future = new CompletableFuture<>();
		workerQueue.add(() -> doReadState(seenState, future));
		return future;
	}
}