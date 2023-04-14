package net.balintgergely.robotics;

public class Colors {
	public static int getAlpha(int rgb){
		return (rgb >> 24) & 0xff;
	}
	public static int getRed(int rgb){
		return (rgb >> 16) & 0xff;
	}
	public static int getGreen(int rgb){
		return (rgb >> 8) & 0xff;
	}
	public static int getBlue(int rgb){
		return rgb & 0xff;
	}
}
