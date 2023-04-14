package net.balintgergely.robotics;

import java.awt.*;
import java.awt.image.*;
import java.util.Comparator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.balintgergely.robotics.Colors.*;

public class Convolution {
	public static record MatchResult(int x,int y,double score,int pid) implements Comparable<MatchResult>{
		@Override
		public int compareTo(MatchResult o) {
			return Double.compare(score, o.score);
		}
	}
	public static double match(BufferedImage target,BufferedImage pattern){
		if(pattern == null){
			return 0;
		}
		int w = pattern.getWidth(),h = pattern.getHeight();
		double totalScore = 0;
		double totalAlpha = 0;
		for(int y = 0;y < h;y++){
			for(int x = 0;x < w;x++){
				int trgb = target.getRGB(x, y);
				int prgb = pattern.getRGB(x, y);

				int tr = getRed(trgb);
				int tg = getGreen(trgb);
				int tb = getBlue(trgb);

				int pr = getRed(prgb);
				int pg = getGreen(prgb);
				int pb = getBlue(prgb);
				int pa = getAlpha(prgb);

				int diff = abs(tr - pr) + abs(tg - pg) + abs(tb - pb);

				double matchRatio = ((double)(0x300 - diff)) / 0x300;

				totalScore += matchRatio * pa;

				totalAlpha += pa;
			}
		}
		return totalScore / totalAlpha;
	}
	public static Stream<MatchResult> allMatchesAtOrigin(BufferedImage target,BufferedImage[] patterns){
		return IntStream.range(0, patterns.length)
			.mapToObj(i -> new MatchResult(0, 0, match(target, patterns[i]), i));
	}
	public static MatchResult bestMatchAtOrigin(BufferedImage target,BufferedImage[] patterns){
		return allMatchesAtOrigin(target, patterns).unordered().parallel().max(Comparator.naturalOrder()).get();
	}
	public static Stream<MatchResult> allMatches(BufferedImage target,BufferedImage pattern){
		int w = pattern.getWidth();
		int h = pattern.getHeight();
		int wDiff = target.getWidth() - w + 1;
		int hDiff = target.getHeight() - h + 1;
		return pointsInRectangle(0, 0, wDiff, hDiff)
			.map(p -> new MatchResult(p.x, p.y,
				match(target.getSubimage(p.x, p.y, w, h),pattern),0));
	}
	public static MatchResult bestMatch(BufferedImage target,BufferedImage pattern){
		return allMatches(target, pattern).unordered().parallel().max(Comparator.naturalOrder()).orElse(null);
	}
	public static Stream<Point> pointsInRectangle(int x,int y,int w,int h){
		return IntStream.range(0, w * h).mapToObj(i -> new Point(x + i%w,y + i/w));
	}
}
