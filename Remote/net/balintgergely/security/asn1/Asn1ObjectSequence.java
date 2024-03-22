package net.balintgergely.security.asn1;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An abstract Asn1Item containing multiple Asn1Items.
 */
public abstract class Asn1ObjectSequence implements Asn1Item, Cloneable{
	private List<Asn1Item> items;
	public List<Asn1Item> items() { return items; }
	public Asn1ObjectSequence(Asn1Item... items){
		this.items = Arrays.asList(items);
	}
	public Asn1ObjectSequence(List<Asn1Item> items){
		this.items = items;
	}
	public Asn1ObjectSequence(Asn1RawItem raw){
		this(raw,false);
	}
	public Asn1ObjectSequence(Asn1RawItem raw,boolean deep){
		items = new ArrayList<>();
		ByteBuffer c = raw.getContent();
		while(c.hasRemaining()){
			Asn1RawItem r = new Asn1RawItem(c);
			if(deep){
				items.add(r.resolve(true));
			}else{
				items.add(r);
			}
		}
	}
	@Override
	public Asn1ObjectSequence duplicate(){
		try{
			Asn1ObjectSequence seq = (Asn1ObjectSequence)super.clone();
			seq.items = new ArrayList<>(items.size());
			for(Asn1Item item : items){
				seq.items.add(item.duplicate());
			}
			return seq;
		}catch(CloneNotSupportedException c){
			throw new RuntimeException(c);
		}
	}
	public Asn1ObjectSequence ofLength(int length){
		if(items.size() != length){
			throw new IllegalStateException();
		}
		return this;
	}
}
