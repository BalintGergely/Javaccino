package net.balintgergely.security.asn1;

import java.util.List;

public class Asn1Set extends Asn1ObjectSequence{
	public Asn1Set(Asn1Item... items){
		super(items);
	}
	public Asn1Set(List<Asn1Item> items){
		super(items);
	}
	public Asn1Set(Asn1RawItem raw){
		this(raw,false);
	}
	public Asn1Set(Asn1RawItem raw,boolean deep){
		super(raw.ofType(0x31),deep);
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		collector.pushLength();
		for(Asn1Item i : items()){
			i.writeTo(collector);
		}
		collector.popLength(0x31);
	}
}
