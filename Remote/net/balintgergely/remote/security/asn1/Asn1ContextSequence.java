package net.balintgergely.remote.security.asn1;

import java.util.List;

public class Asn1ContextSequence extends Asn1ObjectSequence{
	public static Asn1ContextSequence wrap(int type,Asn1Item... items){
		return new Asn1ContextSequence(type,items);
	}
	private byte type;
	public Asn1ContextSequence(int type,Asn1Item... items){
		super(items);
		this.type = (byte)type;
	}
	public Asn1ContextSequence(int type,List<Asn1Item> items){
		super(items);
		this.type = (byte)type;
	}
	public Asn1ContextSequence(Asn1RawItem raw){
		this(raw,false);
	}
	public Asn1ContextSequence(Asn1RawItem raw,boolean deep){
		super(raw,deep);
		this.type = raw.getType();
	}
	public byte getType(){
		return type;
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		collector.pushLength();
		for(Asn1Item i : items()){
			i.writeTo(collector);
		}
		collector.popLength(type);
	}
}
