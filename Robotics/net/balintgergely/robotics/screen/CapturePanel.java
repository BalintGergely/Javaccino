package net.balintgergely.robotics.screen;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

public class CapturePanel extends JPanel{
	private int preferredX,preferredY;
	public CapturePanel(){
		super(null,false);
		super.setOpaque(false);
		super.setBackground(ScreenFrame.TRANSPARENT);
		super.setFocusable(false);
	}
	public void setBounds(int x,int y,int width,int height){
		super.setPreferredSize(new Dimension(width, height));
		super.setBounds(x, y, width, height);
	}
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D gr = (Graphics2D)g;
		gr.setColor(getForeground());
		Rectangle rect = new Rectangle();
		for(int i = 0;i < super.getComponentCount();i++){
			Component comp = super.getComponent(i);
			comp.getBounds(rect);
			rect.x -= 1;
			rect.y -= 1;
			rect.width += 1;
			rect.height += 1;
			gr.setColor(comp.getForeground());
			gr.draw(rect);
		}
	}
	public void doLayout(){
		super.doLayout();
		for(int i = 0;i < super.getComponentCount();i++){
			Component c = super.getComponent(i);
			if(c instanceof CapturePanel){
				CapturePanel cp = (CapturePanel)c;
				cp.setLocation(cp.preferredX, cp.preferredY);
				cp.setSize(cp.getPreferredSize());
			}
		}
	}
	public Rectangle getBoundsOnScreen(){
		return new Rectangle(getLocationOnScreen(),getSize());
	}
}
