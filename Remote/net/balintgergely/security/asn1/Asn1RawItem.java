package net.balintgergely.security.asn1;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Base64.Encoder;

/**
 * An Asn1Item stored as a sequence of bytes.
 */
public class Asn1RawItem implements Asn1Item, Cloneable{
	private static ByteBuffer readFrom(ByteBuffer input){
		int start = input.position();
		input.get();
		long len = Asn1Utils.getLength(input);
		if(len < 0 || len > input.remaining()){
			return null;
		}
		ByteBuffer data = input.slice(start, (int)(input.position() + len - start));
		data.position(input.position() - start);
		input.position((int)(input.position() + len));
		return data;
	}
	private ByteBuffer data;
	private Asn1RawItem(){}
	/**
	 * Creats and Asn1RawItem from the specified item.
	 * If the specified item is already an Asn1RawItem, this is a shallow copy.
	 * Otherwise it is encoded into a sequence of bytes.
	 */
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
	/**
	 * Read one Asn1Item from the specified ByteBuffer.
	 * The resulting Asn1RawItem contains a view to the specified buffer.
	 * The buffer is advanced to the next item.
	 */
	public Asn1RawItem(ByteBuffer input){
		this.data = Objects.requireNonNull(readFrom(input));
	}
	/**
	 * @return The type of this Asn1RawItem.
	 */
	public byte getType(){
		return data.get(0);
	}
	/**
	 * Returns the raw content.
	 * This is modifiable if the Asn1RawItem was not created from a read only view to a ByteBuffer.
	 */
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
	/**
	 * @return An Asn1RawItem backed by the same data, but the buffer is made read-only.
	 */
	public Asn1RawItem toReadOnly(){
		Asn1RawItem r = new Asn1RawItem();
		r.data = data.asReadOnlyBuffer();
		return r;
	}
	public <E extends Asn1Item> E as(Class<E> type){
		if(type.isInstance(this)){
			return type.cast(this);
		}else if(type == Asn1ContextSequence.class){
			return type.cast(new Asn1ContextSequence(this));
		}else{
			Asn1Item r = resolve();
			if(r == this){
				throw new IllegalStateException("This Asn1RawItem can not be interpreted as "+type);
			}
			return r.as(type);
		}
	}
	/**
	 * Checks that this Asn1RawItem has the specified type.
	 * @return This Asn1RawItem
	 * @throws IllegalStateException on a type mismatch
	 */
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
	private Asn1Item tryResolveAsContextSequence(boolean deep){
		ByteBuffer input = getContent();
		if(!input.hasRemaining()){
			return this;
		}
		do{
			if(readFrom(input) == null){
				return this;
			}
		}while(input.hasRemaining());
		return new Asn1ContextSequence(this,deep);
	}
	protected String contentToString(){
		Asn1Item item = tryResolveAsContextSequence(false);
		if(item != this){
			return ((Asn1ContextSequence)item).contentToString();
		}else{
			Encoder encoder = Base64.getEncoder();
			ByteBuffer result = encoder.encode(getContent());
			return StandardCharsets.UTF_8.decode(result).toString();
		}
	}
	public String toString(){
		Asn1Item resolved = resolve();
		if(resolved == this){
			return "RAW ITEM " + Integer.toHexString(Byte.toUnsignedInt(getType())) + " " + contentToString();
		}else{
			return resolved.toString();
		}
	}
}
