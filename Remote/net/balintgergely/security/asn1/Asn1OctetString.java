package net.balintgergely.security.asn1;

import java.nio.ByteBuffer;

public class Asn1OctetString extends Asn1RawItem{
	private static ByteBuffer encodeNow(byte[] data){
		int contentLength = data.length;
		int lengthOfLength = Asn1Utils.lengthOfLength(contentLength);
		ByteBuffer buf = ByteBuffer.allocate(1 + lengthOfLength + contentLength);
		buf.put((byte)0x04);
		Asn1Utils.putLength(buf, contentLength);
		buf.put(data);
		buf.flip();
		return buf;
	}
	public Asn1OctetString(Asn1RawItem that) {
		super(that.ofType(0x04));
	}
	public Asn1OctetString(byte[] data){
		super(encodeNow(data));
	}
}
