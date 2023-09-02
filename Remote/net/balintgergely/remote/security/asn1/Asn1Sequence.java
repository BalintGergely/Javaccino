package net.balintgergely.remote.security.asn1;

import java.util.List;

public class Asn1Sequence extends Asn1ObjectSequence{
	public Asn1Sequence(Asn1Item... items){
		super(items);
	}
	public Asn1Sequence(List<Asn1Item> items){
		super(items);
	}
	public Asn1Sequence(Asn1RawItem raw){
		this(raw,false);
	}
	public Asn1Sequence(Asn1RawItem raw,boolean deep){
		super(raw.ofType(0x30),deep);
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		collector.pushLength();
		for(Asn1Item i : items()){
			i.writeTo(collector);
		}
		collector.popLength(0x30);
	}
}
