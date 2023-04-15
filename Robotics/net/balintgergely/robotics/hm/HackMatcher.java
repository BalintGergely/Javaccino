package net.balintgergely.robotics.hm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.balintgergely.robotics.Convolution;
import net.balintgergely.robotics.ImageUtils;
import net.balintgergely.robotics.Convolution.MatchResult;
import net.balintgergely.robotics.screen.CapturePanel;
import net.balintgergely.robotics.screen.ScreenFrame;

public class HackMatcher{
	private static final int VISUAL_BRICK_WIDTH = 24;
	private static final int VISUAL_BRICK_HEIGHT = 24;
	private static final int VISUAL_BOARD_WIDTH = 24*6 + 1;//145;
	private static final int VISUAL_BOARD_HEIGHT = 238;
	private BufferedImage rowPattern;
	private BufferedImage exaPattern;
	private BufferedImage[] blockPatternArray;
	private BufferedImage[] brickPatternArray;
	private Robot robot;
	private int scale = 1;
	private int delay = 60;
	private ScreenFrame screenFrame;
	private CapturePanel boardPanel;
	private CapturePanel exaPanel;
	private BufferedImage readPattern(String file) throws Exception{
		BufferedImage pattern = ImageIO.read(new File("Robotics\\assets\\"+file+".png"));
		return pattern;
	}
	private HackMatcher() throws Exception{
		rowPattern = readPattern("HackMatchRowPattern");
		exaPattern = readPattern("EXA");
		robot = new Robot();
		robot.setAutoDelay(0);
		screenFrame = new ScreenFrame(360, 270);
		screenFrame.setVisible(true);
		screenFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		exaPanel = new CapturePanel();
		exaPanel.setForeground(Color.YELLOW);
		boardPanel = new CapturePanel();
		boardPanel.setForeground(Color.BLUE);
		screenFrame.getClient().add(boardPanel);
		screenFrame.getClient().add(exaPanel);
		boardPanel.setBounds(107, 0, VISUAL_BOARD_WIDTH, VISUAL_BOARD_HEIGHT + 4);
		exaPanel.setBounds(107, VISUAL_BOARD_HEIGHT + 7, VISUAL_BOARD_WIDTH, 9);
		blockPatternArray = new BufferedImage[]{
			readPattern("Chip"),
			readPattern("Garbage"),
			readPattern("Red"),
			readPattern("Green"),
			readPattern("Blue"),
			readPattern("Magenta"),
			readPattern("Yellow"),
			readPattern("RedS"),
			readPattern("GreenS"),
			readPattern("BlueS"),
			readPattern("MagentaS"),
			readPattern("YellowS"),
		};
		brickPatternArray = Arrays.copyOf(blockPatternArray,7);
		brickPatternArray[0] = null;
	}
	private void sleep(long millis){
		try{
			Thread.sleep(millis);
		}catch(InterruptedException e){
			throw new RuntimeException(e);
		}
	}
	private boolean isScrolling = false;
	private void beginScroll(){
		if(!isScrolling){
			robot.keyPress(KeyEvent.VK_S);
			isScrolling = true;
		}
	}
	private void endScroll(){
		if(isScrolling){
			robot.keyRelease(KeyEvent.VK_S);
			isScrolling = false;
		}
	}
	private void moveLeft(){
		endScroll();
		robot.keyPress(KeyEvent.VK_A);
		sleep(delay / 2);
		robot.keyRelease(KeyEvent.VK_A);
		sleep(delay / 2);
	}
	private void moveRight(){
		endScroll();
		robot.keyPress(KeyEvent.VK_D);
		sleep(delay / 2);
		robot.keyRelease(KeyEvent.VK_D);
		sleep(delay / 2);
	}
	private void grabOrDrop(){
		endScroll();
		robot.keyPress(KeyEvent.VK_J);
		sleep(delay / 2);
		robot.keyRelease(KeyEvent.VK_J);
		sleep(delay / 2);
	}
	private void swap(){
		endScroll();
		robot.keyPress(KeyEvent.VK_K);
		sleep(delay / 2);
		robot.keyRelease(KeyEvent.VK_K);
		sleep(delay / 2);
	}
	private BufferedImage makeScreenshot(){
		BufferedImage sc = robot.createScreenCapture(boardPanel.getBoundsOnScreen());
		return ImageUtils.copy(sc, BufferedImage.TYPE_INT_ARGB, scale);
	}
	private HackMatchState readBoardState(BufferedImage board) throws Exception{
		MatchResult alignment = Convolution.bestMatch(board, rowPattern);
		
		int y = alignment.y() % VISUAL_BRICK_HEIGHT + 1;

		HackMatchState state = new HackMatchState();
		byte[] row = new byte[6];

		while(y + VISUAL_BRICK_HEIGHT < board.getHeight()){
			boolean rowHadElements = false;
			for(int x = 0;x < 6;x++){
				if(state.getHeight() != 0 && state.getAt(x, state.getHeight() - 1) == HackMatchState.EMPTY){
					continue;
				}
				int rx = 1 + x * VISUAL_BRICK_WIDTH;
				BufferedImage brickImage = board.getSubimage(rx, y, VISUAL_BRICK_WIDTH, VISUAL_BRICK_HEIGHT);
				MatchResult r = Convolution.bestMatchAtOrigin(brickImage, blockPatternArray);
				boolean valid = false;
				if(r.pid() == 0 && r.score() > 0.9){
					r = Convolution.bestMatchAtOrigin(brickImage, brickPatternArray);
					valid = true;
				}else{
					valid = r.score() > 0.9;
				}
				
				if(valid){
					row[x] = (byte)(r.pid());
					rowHadElements = true;
				}else{
					row[x] = 0;
				}
			}
			if(!rowHadElements){
				break;
			}
			state = state.addBlockRowBottom(row);
			y += VISUAL_BRICK_HEIGHT;
		}

		return state;
	}
	private BufferedImage renderState(BufferedImage image,HackMatchState state) throws Exception{
		if(image == null){
			image = new BufferedImage(VISUAL_BOARD_WIDTH, VISUAL_BOARD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		}
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, VISUAL_BOARD_WIDTH, VISUAL_BOARD_HEIGHT);
		if(state == null){
			g2d.dispose();
			return image;
		}
		for(int y = 0;y < state.getHeight();y++){
			for(int x = 0;x < 6;x++){
				g2d.drawImage(blockPatternArray[state.getAt(x, y)], x * VISUAL_BRICK_WIDTH, y * VISUAL_BRICK_HEIGHT, null);
			}
		}
		g2d.dispose();
		return image;
	}
	private void makeAllMoves(HackMatchState state,Consumer<HackMatchState> target){
		for(int i = 0;i < 6;i++){
			HackMatchState move = state.flip(i);
			if(move != state){
				target.accept(move);
			}
		}
		for(int i = 0;i < 6;i++){
			for(int v = 0;v < 6;v++){
				HackMatchState s = state.move(i, v);
				if(s != state){
					target.accept(s);
				}
			}
		}
	}
	private void makeAllDrops(HackMatchState state,Consumer<HackMatchState> target){
		if(state.isHoldingTile()){
			for(int i = 0;i < 6;i++){
				HackMatchState move = state.grabOrDrop(i);
				if(move != state){
					target.accept(move);
				}
			}
		}else{
			target.accept(state);
		}
	}
	private Stream<HackMatchState> movesAtMost(HackMatchState start,int limit){
		Stream<HackMatchState> s = Stream.of(start).unordered().parallel();
		for(int i = 0;i < limit;i++){
			s = s.mapMulti(this::makeAllMoves);
		}
		s = s.mapMulti(this::makeAllDrops);
		return s;
	}
	private HackMatchState findGoodMove(HackMatchState start, int limit){
		return movesAtMost(start, limit).filter(k -> k.getScore() >= 4).findAny().orElse(null);
	}
	private HackMatchState findBestMove(HackMatchState start, int limit){
		return movesAtMost(start, limit).max(Comparator.comparing(k -> k.getScore())).orElse(null);
	}
	private HackMatchState findMoveAlg(Queue<HackMatchState> queue,HackMatchState start) throws InterruptedException{
		if(start.getHeight() <= 1){
			return start;
		}
		HashSet<HackMatchState> lim = new HashSet<>();
		queue.clear();
		queue.add(start);
		lim.add(start);
		int omgScore = start.getScore() + 10000;
		HackMatchState best = start;
		boolean trip = false;
		long fallTime = System.nanoTime() + 2000000000l;
		while(!queue.isEmpty()){
			HackMatchState s = queue.poll();
			if(!s.isHoldingTile()){
				if(start.getHeight() >= 7 && s.getHeight() < start.getHeight()){
					return s;
				}
				if(s.getScore() > best.getScore()){
					if(s.getScore() >= omgScore){
						System.out.println("Oh!");
						trip = true;
					}
					best = s;
				}
			}
			if(!trip){
				if(s.getScore() < best.getScore() - 2000000){
					continue;
				}
				//if(s.getParent() != null && s.getParent().getScore() < s.getScore() + 2000000){
				//	continue;
				//}
				makeAllMoves(s, v -> {
					if(lim.add(v)){
						queue.add(v);
					}
				});
			}
			if(System.nanoTime() > fallTime){
				trip = true;
			}
		}
		return best;
	}
	private int savedLocation = -1;
	private int calibrate(){
		if(savedLocation == -1){
			for(int k = 0;k < 5;k++){
				moveLeft();
			}
			savedLocation = 0;
		}
		return savedLocation;
	}
	private void recalibrate(){
		BufferedImage wrow = robot.createScreenCapture(exaPanel.getBoundsOnScreen());
		MatchResult res = Convolution.bestMatch(wrow, exaPattern);
		if(res.score() < 0.9){
			grabOrDrop();
			savedLocation = -1;
		}else{
			savedLocation = res.x() / VISUAL_BRICK_WIDTH;
		}
	}
	private int executeMove(HackMatchState start,HackMatchState end){
		if(end == start){
			return calibrate();
		}else{
			int x = executeMove(start, end.getParent());
			if(end.hasLatestMove()){
				int nx = end.getLatestMoveX();
				while(x < nx){
					moveRight();
					x++;
				}
				while(x > nx){
					moveLeft();;
					x--;
				}
				if(end.isLatestMoveSwap()){
					swap();
				}else{
					grabOrDrop();
				}
			}
			savedLocation = x;
			return x;
		}
	}
	public static void main(String[] atgs) throws Exception{
		ScreenFrame.init();
		HackMatcher matcher = new HackMatcher();
		BufferedImage outImage = matcher.renderState(null, null);
		JFrame frame = new JFrame("Output");
		JLabel label = new JLabel(new ImageIcon(outImage));
		label.setText("Hello");
		label.setVerticalTextPosition(SwingConstants.BOTTOM);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		JButton hover = new JButton("Hover me");
		frame.add(hover, BorderLayout.PAGE_START);
		frame.add(label);
		frame.pack();
		frame.setVisible(true);
		int writeMax = 10;
		int write = 0;
		Queue<HackMatchState> buffer = new PriorityQueue<>(Comparator.comparingInt(HackMatchState::getScore).reversed());
		HackMatchState stuck = null;
		while(true){
			System.out.println("Scanning...");
			BufferedImage board = matcher.makeScreenshot();
			if(write < writeMax){
				ImageIO.write(board, "png", new File("Temp/Output/"+write+".png"));
				write++;
			}
			HackMatchState state = matcher.readBoardState(board);
			matcher.renderState(outImage,state);
			label.repaint();
			label.setText(state.getScore()+"  "+state.getHeight());
			if(hover.getModel().isRollover()){
				HackMatchState newState;
				if(state.equals(stuck)){
					newState = state;
				}else{
					matcher.endScroll();
					System.out.println("Searching...");
					newState = matcher.findMoveAlg(buffer,state);
				}
				if(newState == state){
					System.out.println("Scrolling...");
					matcher.beginScroll();
					stuck = state;
				}else{
					stuck = null;
					System.out.println("Executing...");
					matcher.executeMove(state, newState);
					matcher.savedLocation = -1;
					System.out.println("Recalibrating...");
					Thread.sleep(300);
					matcher.recalibrate();
					Thread.sleep(300);
				}
			}else{
				matcher.endScroll();
				matcher.savedLocation = -1;
				stuck = null;
			}
		}
	}
}