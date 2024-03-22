package net.balintgergely.security.asn1;

import java.math.BigInteger;
import java.nio.ByteBuffer;

public class Asn1Integer implements Asn1Item{
	public final BigInteger value;
	public Asn1Integer(long value){
		this.value = BigInteger.valueOf(value);
	}
	public Asn1Integer(BigInteger value){
		this.value = value;
	}
	public Asn1Integer(Asn1RawItem item){
		item.ofType(0x02);
		ByteBuffer b = item.getContent();
		if(b.hasArray()){
			value = new BigInteger(b.array(), b.arrayOffset()+b.position(), b.remaining());
		}else{
			byte[] d = new byte[b.remaining()];
			b.get(d);
			value = new BigInteger(d);
		}
	}
	public BigInteger getIntValue(){
		return value;
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		collector.augmentAndAppend(0x02,ByteBuffer.wrap(value.toByteArray()));
	}
}
