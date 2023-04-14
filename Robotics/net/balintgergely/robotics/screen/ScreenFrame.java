package net.balintgergely.robotics.screen;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class ScreenFrame extends JFrame{
	public static void init(){
		System.setProperty("sun.java2d.uiScale", "1");
	}
	public static Color TRANSPARENT = new Color(0,true);
	public static void main(String[] atgs){
		init();
		ScreenFrame frame = new ScreenFrame(500,600);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		CapturePanel cp = new CapturePanel();
		cp.setForeground(Color.RED);
		frame.getClient().add(cp);
		cp.setBounds(20, 20, 50, 50);
	}
	private JPanel header;
	private JPanel content;
	private CapturePanel client;
	private final MotionHelper motionHelper = new MotionHelper();
	public ScreenFrame(int width,int height){
		super.setUndecorated(true);
		super.setAlwaysOnTop(true);
		super.setBackground(new Color(0,true));
		{
			header = new JPanel(new FlowLayout(), false);
			header.setBackground(Color.WHITE);
			header.setPreferredSize(new Dimension(0, 32));
			header.setFocusable(false);
			super.add(header, BorderLayout.PAGE_START);
		}
		{
			content = new JPanel(new BorderLayout(), false);
			content.setOpaque(false);
			content.setBackground(TRANSPARENT);
			content.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1, false));
			content.setFocusable(false);
			super.add(content, BorderLayout.CENTER);
		}
		{
			client = new CapturePanel();
			client.setPreferredSize(new Dimension(width, height));
			content.add(client);
		}
		super.addMouseListener(motionHelper);
		super.addMouseMotionListener(motionHelper);
		super.addKeyListener(motionHelper);
		super.pack();
		super.setLocationRelativeTo(null);
	}
	public CapturePanel getClient(){
		return client;
	}
	private class MotionHelper extends MouseAdapter implements KeyListener{
		private Point mouseDragOffset;
		private Point getPointRelativeToFrame(MouseEvent e){
			Point p = e.getPoint();
			Component comp = e.getComponent();
			while(comp != ScreenFrame.this){
				p.translate(comp.getX(), comp.getY());
				comp = comp.getParent();
			}
			return p;
		}
		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if(mouseDragOffset != null){
				Point p = e.getLocationOnScreen();
				ScreenFrame.this.setLocation(p.x - mouseDragOffset.x, p.y - mouseDragOffset.y);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			super.mouseEntered(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			super.mouseExited(e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			// TODO Auto-generated method stub
			super.mouseMoved(e);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			mouseDragOffset = getPointRelativeToFrame(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			mouseDragOffset = null;
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			// TODO Auto-generated method stub
			super.mouseWheelMoved(e);
		}

		@Override
		public void keyPressed(KeyEvent e) {
			switch(e.getKeyCode()){
				case KeyEvent.VK_LEFT:
					setLocation(getX() - 1,getY());
					break;
				case KeyEvent.VK_RIGHT:
					setLocation(getX() + 1,getY());
					break;
				case KeyEvent.VK_UP:
					setLocation(getX(), getY() - 1);
					break;
				case KeyEvent.VK_DOWN:
					setLocation(getX(), getY() + 1);
					break;
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
