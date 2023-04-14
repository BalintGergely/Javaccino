package net.balintgergely.robotics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;

public class ImageUtils {
	public static BufferedImage copy(BufferedImage that,int newType,int dr){
		BufferedImage newImage = new BufferedImage(that.getWidth() / dr, that.getHeight() / dr, newType);
		Graphics2D g = newImage.createGraphics();
		g.drawImage(that, 0, 0, that.getWidth() / dr, that.getHeight() / dr, null);
		g.dispose();
		return newImage;
	}
	public static BufferedImage copy(BufferedImage that){
		return new BufferedImage(that.getColorModel(),that.copyData(null), that.isAlphaPremultiplied(), null);
	}
	public static void drawShape(Shape shape,int color,BufferedImage target){
		Graphics2D gr = target.createGraphics();
		gr.setColor(new Color(color, true));
		gr.draw(shape);
		gr.dispose();
	}
}
