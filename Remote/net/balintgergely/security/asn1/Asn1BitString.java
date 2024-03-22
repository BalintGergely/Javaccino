package net.balintgergely.security.asn1;

import java.nio.ByteBuffer;

public class Asn1BitString extends Asn1RawItem{
	private static ByteBuffer encodeNow(byte[] data,int padding){
		int contentLength = data.length + 1;
		int lengthOfLength = Asn1Utils.lengthOfLength(contentLength);
		ByteBuffer buf = ByteBuffer.allocate(1 + lengthOfLength + contentLength);
		buf.put((byte)0x03);
		Asn1Utils.putLength(buf, contentLength);
		buf.put((byte)padding);
		buf.put(data);
		byte lastByte = buf.get(buf.limit() - 1);
		buf.put(buf.limit() - 1, (byte)(lastByte & (0xff << padding)));
		buf.flip();
		return buf;
	}
	public Asn1BitString(byte[] k,int padding){
		super(encodeNow(k, padding));
	}
	public Asn1BitString(Asn1RawItem that){
		super(that.ofType(0x03));
	}
	public int bitCount(){
		ByteBuffer content = getContent();
		int padding = content.get();
		return content.remaining() * 8 - padding;
	}
	public String toString(){
		return "BIT STRING " + bitCount() + " " + contentToString();
	}
}
