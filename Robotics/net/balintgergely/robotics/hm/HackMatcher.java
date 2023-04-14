package net.balintgergely.robotics.hm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
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
	private BufferedImage[] blockPatternArray;
	private Robot robot;
	private int scale = 1;
	private int delay = 100;
	private ScreenFrame screenFrame;
	private CapturePanel boardPanel;
	private BufferedImage readPattern(String file) throws Exception{
		BufferedImage pattern = ImageIO.read(new File("Robotics\\assets\\"+file+".png"));
		return pattern;
	}
	private HackMatcher() throws Exception{
		rowPattern = readPattern("HackMatchRowPattern");
		robot = new Robot();
		robot.setAutoDelay(0);
		screenFrame = new ScreenFrame(360, 270);
		screenFrame.setVisible(true);
		screenFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		boardPanel = new CapturePanel();
		boardPanel.setForeground(Color.BLUE);
		screenFrame.getClient().add(boardPanel);
		boardPanel.setBounds(107, 6, VISUAL_BOARD_WIDTH, VISUAL_BOARD_HEIGHT);
		blockPatternArray = new BufferedImage[]{
			null,
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
	}
	private void sleep(long millis){
		try{
			Thread.sleep(millis);
		}catch(InterruptedException e){
			throw new RuntimeException(e);
		}
	}
	private void moveLeft(){
		robot.keyPress(KeyEvent.VK_A);
		sleep(delay / 2);
		robot.keyRelease(KeyEvent.VK_A);
		sleep(delay / 2);
	}
	private void moveRight(){
		robot.keyPress(KeyEvent.VK_D);
		sleep(delay / 2);
		robot.keyRelease(KeyEvent.VK_D);
		sleep(delay / 2);
	}
	private void grabOrDrop(){
		robot.keyPress(KeyEvent.VK_J);
		sleep(delay / 2);
		robot.keyRelease(KeyEvent.VK_J);
		sleep(delay / 2);
	}
	private void swap(){
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
			int rx = 1;
			boolean rowHadElements = false;
			for(int x = 0;x < 6;x++){
				if(state.getHeight() != 0 && state.getAt(x, state.getHeight() - 1) == HackMatchState.EMPTY){
					continue;
				}
				BufferedImage brickImage = board.getSubimage(rx, y, VISUAL_BRICK_WIDTH, VISUAL_BRICK_HEIGHT);
				MatchResult r = Convolution.bestMatchAtOrigin(brickImage, blockPatternArray);
				if(r.score() >= 0.9){
					row[x] = (byte)(r.pid());
					rowHadElements = true;
				}else{
					row[x] = 0;
				}
				rx += VISUAL_BRICK_WIDTH;
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
			HackMatchState s = state.grabOrDrop(i);
			if(s != state){
				target.accept(s);
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
		if(start.getHeight() <= 2){
			return start;
		}
		HashSet<HackMatchState> lim = new HashSet<>();
		queue.clear();
		queue.add(start);
		lim.add(start);
		int omgScore = start.getScore() + 10000;
		HackMatchState best = start;
		boolean trip = false;
		while(!queue.isEmpty()){
			HackMatchState s = queue.poll();
			if(s.getHeight() > 10){
				continue;
			}
			if(!s.isHoldingTile() && s.getScore() > best.getScore()){
				if(s.getScore() >= omgScore){
					System.out.println("Oh!");
					trip = true;
				}
				best = s;
			}
			if(queue.size() >= 500000){
				System.out.println("Tripped!");
				trip = true;
			}
			if(!trip){
				if(s.getParent() != null && s.getParent().getScore() + 200 < s.getScore()){
					continue;
				}
				makeAllMoves(s, v -> {
					if(lim.add(v)){
						queue.add(v);
					}
				});
			}
		}
		return best;
	}
	private int savedLocation = -1;
	private int executeMove(HackMatchState start,HackMatchState end){
		if(end == start){
			if(savedLocation == -1){
				for(int k = 0;k < 5;k++){
					moveLeft();
				}
				savedLocation = 0;
			}
			return savedLocation;
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
				System.out.println("Searching...");
				HackMatchState newState = matcher.findMoveAlg(buffer,state);
				System.out.println("Executing...");
				if(newState != null){
					matcher.executeMove(state, newState);
					System.out.println("Waiting...");
					Thread.sleep(1000);
				}
			}else{
				matcher.savedLocation = -1;
			}
		}
	}
}