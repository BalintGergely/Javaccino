package net.balintgergely.security.asn1;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * An Asn1Item being stored as a Java String.
 * Immutable.
 */
public class Asn1String implements Asn1Item{
	private byte type;
	public final String value;
	public Asn1String(String val){
		this.type = 0x0C;
		this.value = val;
	}
	public Asn1String(Asn1RawItem item){
		ByteBuffer b = item.getContent();
		this.type = item.getType();
		this.value = getCharset().decode(b).toString();
	}
	public Charset getCharset(){
		// What idiot thought we need 7 different encodings?!
		return switch (type) {
			case 0x13, 0x16, 0x1B -> StandardCharsets.US_ASCII;
			case 0x14 -> StandardCharsets.ISO_8859_1;
			case 0x1E -> StandardCharsets.UTF_16BE;
			case 0x0C -> StandardCharsets.UTF_8;
			case 0x1C -> Charset.forName("UTF_32BE");
			default -> null;
        };
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		collector.augmentAndAppend(type, getCharset().encode(value));
	}
	@Override
	public String toString(){
		return "STRING " + getCharset().toString() + " \"" + value + "\"";
	}
}
