package net.balintgergely.remote.security.asn1;

import java.nio.ByteBuffer;

/**
 * Singleton null item.
 */
public class Asn1Null implements Asn1Item{
	private static final byte[] DATA = new byte[]{ 0x05, 0x00 };
	public static Asn1Null INSTANCE = new Asn1Null();
	private Asn1Null(){}
	@Override
	public void writeTo(Asn1Collector collector) {
		collector.append(ByteBuffer.wrap(DATA).asReadOnlyBuffer());
	}
}
