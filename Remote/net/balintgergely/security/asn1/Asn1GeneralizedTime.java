package net.balintgergely.security.asn1;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Asn1GeneralizedTime implements Asn1Item{
	private static final String OFFSET_FORMAT = "%02d%02d";
	private static final String DATE_FORMAT = 
			 "%04d" +"%02d" +"%02d" +"%02d"  +  "%02d"  +  "%02d"    +   ".%01d";
	private static final Pattern DATE_PATTERN = Pattern.compile("" +
			"(\\d{4})(\\d{2})(\\d{2})(\\d{2})(?:(\\d{2})(?:(\\d{2})(?:\\.(\\d{1,3}))?)?)?(Z|[+-]\\d{4})?");
	private Temporal date;
	public Asn1GeneralizedTime(Temporal d){
		if(d instanceof OffsetDateTime || d instanceof LocalDateTime){
			this.date = d;
		}else{
			throw new IllegalArgumentException();
		}
	}
	private int x(Matcher m,int g){
		String v = m.group(g);
		if(v == null){
			return 0;
		}else{
			return Integer.parseInt(v);
		}
	}
	public Asn1GeneralizedTime(Asn1RawItem i){
		i.ofType(0x18);
		ByteBuffer content = i.getContent();
		byte[] bytes = new byte[content.remaining()];
		content.get(bytes);
		Matcher r = DATE_PATTERN.matcher(new String(bytes, StandardCharsets.UTF_8));
		r.matches();
		int y = x(r,1);
		int m = x(r,2);
		int d = x(r,3);
		int ho = x(r,4);
		int mt = x(r,5);
		int sc = x(r,6);
		int ns = x(r,7);
		String zone = r.group(8);
		if(zone == null){
			date = LocalDateTime.of(y,m,d,ho,mt,sc,ns);
		}else{
			date = OffsetDateTime.of(y,m,d,ho,mt,sc,ns,ZoneOffset.of(zone));
		}
	}
	public String getDateString(){
		int y = date.get(ChronoField.YEAR);
		int m = date.get(ChronoField.MONTH_OF_YEAR);
		int d = date.get(ChronoField.DAY_OF_MONTH);
		int ho = date.get(ChronoField.HOUR_OF_DAY);
		int mt = date.get(ChronoField.MINUTE_OF_HOUR);
		int sc = date.get(ChronoField.SECOND_OF_MINUTE);
		int ns = date.get(ChronoField.MILLI_OF_SECOND);
		String s = String.format(DATE_FORMAT, y, m, d, ho, mt, sc, ns);
		offset :if(date instanceof OffsetDateTime o){
			ZoneOffset zone = o.getOffset();
			int secs = zone.getTotalSeconds();
			if(secs > 0){
				s += "+";
			}
			if(secs == 0){
				s += "Z";
				break offset;
			}
			if(secs < 0){
				secs = -secs;
				s += "-";
			}
			if(secs % 60 != 0){
				throw new IllegalArgumentException("Offset must be full minutes!");
			}
			int offsetMins = (secs / 60) % 60;
			int offsetHours = (secs / (60 * 60));
			s += String.format(OFFSET_FORMAT, offsetHours, offsetMins);
		}
		return s;
	}
	@Override
	public String toString(){
		return "GENERALIZED TIME " + getDateString();
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		collector.augmentAndAppend(0x18, ByteBuffer.wrap(getDateString().getBytes(StandardCharsets.UTF_8)));
	}
}
