package net.balintgergely.robotics.hm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.balintgergely.robotics.screen.ScreenFrame;
import static net.balintgergely.robotics.hm.HackMatchGameInterface.*;

public class HackMatcher {
	private static final byte[] BRICK_TYPES = HackMatchState.getBrickTypes();
	private static final byte[] INVERTED_BRICK_TYPES;
	static{
		INVERTED_BRICK_TYPES = new byte[0x10];
		for(int i = 0;i < BRICK_TYPES.length;i++){ INVERTED_BRICK_TYPES[BRICK_TYPES[i]] = (byte)i; }
	}
	private static void drawBrick(Graphics2D g2d,int x,int y,byte brick){
		final int W = VISUAL_BRICK_WIDTH;
		final int H = VISUAL_BRICK_HEIGHT;
		switch(brick){
			case HackMatchState.BRICK_R:
				g2d.setColor(new Color(0xFF0000));
				g2d.fillRect(x, y, W, H);
				break;
			case HackMatchState.BRICK_G:
				g2d.setColor(new Color(0x00FF00));
				g2d.fillRect(x, y, W, H);
				break;
			case HackMatchState.BRICK_B:
				g2d.setColor(new Color(0x0000FF));
				g2d.fillRect(x, y, W, H);
				break;
			case HackMatchState.BRICK_M:
				g2d.setColor(new Color(0xFF00FF));
				g2d.fillRect(x, y, W, H);
				break;
			case HackMatchState.BRICK_Y:
				g2d.setColor(new Color(0xFFFF00));
				g2d.fillRect(x, y, W, H);
				break;
			case HackMatchState.BRICK_SUPER_R:
				g2d.setColor(new Color(0xFF0000));
				g2d.fillOval(x, y, W, H);
				break;
			case HackMatchState.BRICK_SUPER_G:
				g2d.setColor(new Color(0x00FF00));
				g2d.fillOval(x, y, W, H);
				break;
			case HackMatchState.BRICK_SUPER_B:
				g2d.setColor(new Color(0x0000FF));
				g2d.fillOval(x, y, W, H);
				break;
			case HackMatchState.BRICK_SUPER_M:
				g2d.setColor(new Color(0xFF00FF));
				g2d.fillOval(x, y, W, H);
				break;
			case HackMatchState.BRICK_SUPER_Y:
				g2d.setColor(new Color(0xFFFF00));
				g2d.fillOval(x, y, W, H);
				break;
			case HackMatchState.BRICK_GARBAGE:
				g2d.setColor(new Color(0xFFFFFF));
				g2d.fillRect(x, y, W, H);
				break;
			case HackMatchState.BRICK_UNKNOWN:
				g2d.setColor(new Color(0x808080));
				g2d.fillRect(x, y, W, H);
				break;
		}
	}
	private static BufferedImage renderState(BufferedImage image,HackMatchState state){
		final int W = VISUAL_BRICK_WIDTH * 6;
		final int H = VISUAL_BRICK_HEIGHT * 11;
		if(image == null){
			image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
		}
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.BLACK);
		g2d.fillRect(0, 0, W, H);
		if(state == null){
			g2d.dispose();
			return image;
		}
		for(int y = 0;y < 10;y++){
			for(int x = 0;x < 6;x++){
				drawBrick(g2d, x * VISUAL_BRICK_WIDTH, y * VISUAL_BRICK_HEIGHT, state.getBrick(x, y));
			}
		}

		int exaLoc = state.getEXALocation();
		int x = exaLoc < 0 ? VISUAL_BOARD_WIDTH / 2 * 5 : VISUAL_BRICK_WIDTH * exaLoc;

		byte hand = state.getHand();
		if(hand != 0){
			drawBrick(g2d, x, VISUAL_BRICK_HEIGHT * 10, state.getHand());
		}
		else{
			g2d.setColor(new Color(0x808000));
			g2d.drawOval(x, VISUAL_BRICK_HEIGHT * 10, VISUAL_BRICK_WIDTH, VISUAL_BRICK_HEIGHT);
		}

		g2d.dispose();
		return image;
	}
	public static void main(String[] atgs) throws Exception{
		ScreenFrame.init();
		HackMatchGameInterface matcher = new HackMatchGameInterface();
		BufferedImage outImage = renderState(null, null);
		JFrame frame = new JFrame("Output");
		JLabel label = new JLabel(new ImageIcon(outImage));
		label.setText("Hello");
		label.setVerticalTextPosition(SwingConstants.BOTTOM);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		JButton hover = new JButton("Hover me");
		hover.setRolloverEnabled(true);
		JComboBox<String> modeBox = new JComboBox<>(new String[]{"Single player","Versus P1","Versus P2"});
		frame.add(hover, BorderLayout.PAGE_START);
		frame.add(modeBox, BorderLayout.PAGE_END);
		frame.add(label, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		HackMatchState stuck = null;
		while(true){
			matcher.setMode(modeBox.getSelectedIndex());
			HackMatchState state = matcher.readState(stuck).get();
			stuck = null;
			renderState(outImage,state);
			label.repaint();
			if(hover.getModel().isRollover()){

				byte moveCode = StateTree.decideMove(state);
				if(moveCode == StateTree.ACTION_NONE){
					stuck = state;
					continue;
				}

				int x = state.getEXALocation();
				int col = moveCode & StateTree.ACTION_COL_MASK;
				while(col < x){
					matcher.moveLeft();
					x--;
				}
				while(x < col){
					matcher.moveRight();
					x++;
				}

				if((moveCode & StateTree.ACTION_TYPE_MASK) == StateTree.ACTION_SWAP){
					matcher.swap();
				}else{
					matcher.grabOrDrop();
				}
			}
		}
	}
}
