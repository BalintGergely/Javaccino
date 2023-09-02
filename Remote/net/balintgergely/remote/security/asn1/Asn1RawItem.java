package net.balintgergely.remote.security.asn1;

import java.nio.ByteBuffer;

public class Asn1RawItem implements Asn1Item, Cloneable{
	private static ByteBuffer readFrom(ByteBuffer input){
		int start = input.position();
		input.get();
		long len = Asn1Utils.getLength(input);
		if(len < 0 || len > input.remaining()){
			throw new RuntimeException("Length too big!");
		}
		ByteBuffer data = input.slice(start, (int)(input.position() + len - start));
		data.position(input.position() - start);
		input.position((int)(input.position() + len));
		return data;
	}
	private ByteBuffer data;
	public Asn1RawItem(Asn1Item that){
		if(that instanceof Asn1RawItem rawItem){
			this.data = rawItem.data;
		}else{
			Asn1Collector collector = new Asn1Collector();
			that.writeTo(collector);
			ByteBuffer bb = collector.toByteBuffer();
			this.data = readFrom(bb);
			if(bb.hasRemaining()){
				throw new RuntimeException("Bad tag!");
			}
		}
	}
	public Asn1RawItem(ByteBuffer input){
		this.data = readFrom(input);
	}
	public byte getType(){
		return data.get(0);
	}
	public ByteBuffer getContent(){
		return data.slice();
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		collector.append(data.slice(0, data.capacity()));
	}
	@Override
	public Asn1RawItem duplicate(){
		try{
			Asn1RawItem ri = (Asn1RawItem)super.clone();
			ri.data = ByteBuffer.allocate(data.capacity());
			ri.data.put(0, data, 0, data.capacity());
			ri.data.position(data.position());
			return ri;
		}catch(CloneNotSupportedException c){
			throw new RuntimeException(c);
		}
	}
	public <E extends Asn1Item> E as(Class<E> type){
		if(type.isInstance(this)){
			return type.cast(this);
		}else if(type == Asn1ContextSequence.class){
			return type.cast(new Asn1ContextSequence(this));
		}else{
			return type.cast(resolve());
		}
	}
	public Asn1RawItem ofType(int type){
		if(getType() != type){
			throw new IllegalStateException();
		}
		return this;
	}
	public Asn1Item resolve(){
		return resolve(false);
	}
	public Asn1Item resolve(boolean deep){
		if(getClass() != Asn1RawItem.class){
			return this;
		}
		return switch (getType()) {
			case 0x02 -> new Asn1Integer(this);
			case 0x03 -> new Asn1BitString(this);
			case 0x04 -> new Asn1OctetString(this);
			case 0x05 -> Asn1Null.INSTANCE;
			case 0x06 -> new Asn1ObjectIdentifier(this);
			case 0x18 -> new Asn1GeneralizedTime(this);
			case 0x30 -> new Asn1Sequence(this,deep);
			case 0x31 -> new Asn1Set(this,deep);
			case 0x13, 0x16, 0x1B, 0x14, 0x1E, 0x0C, 0x1C -> new Asn1String(this);
			default -> this;
		};
	}
	public String toString(){
		return "Asn1RawItem of type " + getType();
	}
}
