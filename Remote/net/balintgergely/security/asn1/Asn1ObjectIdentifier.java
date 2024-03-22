package net.balintgergely.security.asn1;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

/**
 * Represents an object identifier.
 */
public class Asn1ObjectIdentifier implements Asn1Item{
	public static final Pattern ID_PATTERN = Pattern.compile("[0-6]\\.[123]?[0-9](?:\\.(?:[1-9][0-9]{0,17}|[0-9]))*");
	private String id;
	public Asn1ObjectIdentifier(String id){
		if(!ID_PATTERN.matcher(id).matches()){
			throw new IllegalArgumentException();
		}
		this.id = id;
	}
	public Asn1ObjectIdentifier(Asn1AlgorithmId id){
		this.id = id.getObjectId();
	}
	public Asn1ObjectIdentifier(Asn1RawItem item){
		item.ofType(0x06);
		ByteBuffer content = item.getContent();
		StringBuilder builder = new StringBuilder(content.remaining() * 2);
		int first = content.get() & 0xff;
		builder.append(first / 40);
		builder.append('.');
		builder.append(first % 40);
		while(content.hasRemaining()){
			long data = 0;
			while(true){
				int b = content.get() & 0xff;
				data = (data << 7) | (b & 0x7F);
				if(b < 0x80){
					break;
				}
			}
			builder.append('.');
			builder.append(data);
		}
		this.id = builder.toString();
	}
	public void getContent(ByteBuffer data){
		String[] d = id.split("\\.");
		int a = Integer.parseInt(d[0]);
		int b = Integer.parseInt(d[1]);
		data.put((byte)(a * 40 + b));
		for(int i = 2;i < d.length;i++){
			long c = Long.parseLong(d[i]);
			int len = 1;
			while((c >> (len * 7)) != 0){
				len++;
			}
			while(len != 0){
				len--;
				data.put((byte)(((c >> (len * 7)) & 0x7F) | (len == 0 ? 0 : 0x80)));
			}
		}
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		ByteBuffer d = ByteBuffer.allocate(id.length());
		d.position(2);
		getContent(d);
		d.flip();
		d.position(2);
		collector.augmentAndAppend(0x06, d);
	}
	@Override
	public <E extends Asn1Item> E as(Class<E> type){
		if(type == Asn1AlgorithmId.class){
			return type.cast(Asn1AlgorithmId.strictLookup(this.id));
		}else{
			return Asn1Item.super.as(type);
		}
	}
	@Override
	public String toString(){
		Asn1AlgorithmId algid = Asn1AlgorithmId.lookup(this.id);
		if(algid != null){
			return algid.toString();
		}else{
			return "OBJECT IDENTIFIER " + id;
		}
	}
}
