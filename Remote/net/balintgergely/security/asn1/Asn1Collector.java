package net.balintgergely.security.asn1;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.stream.Stream;

import net.balintgergely.util.ByteBufferInputStream;
import net.balintgergely.util.SequenceInputStream;

/**
 * Helper class for Asn1 serialization.
 */
public class Asn1Collector{
	private ArrayList<ByteBuffer> data = new ArrayList<>();
	/**
	 * Add all data contained in the specified Asn1Collector.
	 */
	public void append(Asn1Collector that){
		data.addAll(that.data);
	}
	/**
	 * Attach length and type information and append the specified payload to this Asn1Collector.
	 * This collector takes ownership of the specified ByteBuffer. Do not modify it afterwards.
	 */
	public void augmentAndAppend(int type,ByteBuffer content){
		int length = content.remaining();
		int lengthOfHeader = Asn1Utils.lengthOfLength(length) + 1;
		if(content.position() < lengthOfHeader){
			if(content.capacity() < length + lengthOfHeader){
				ByteBuffer header = ByteBuffer.allocate(lengthOfHeader);
				header.put((byte)type);
				Asn1Utils.putLength(header, length);
				header.flip();
				data.add(header);
				data.add(content);
				return;
			}
			content.put(lengthOfHeader, content, content.position(), content.remaining());
			content.position(0);
			content.limit(length + lengthOfHeader);
		}else{
			content.position(content.position() - lengthOfHeader);
		}
		content.mark();
		content.put((byte)type);
		Asn1Utils.putLength(content, length);
		content.reset();
		data.add(content);
	}
	/**
	 * Add the specified ByteBuffer raw to the collector.
	 * This collector takes ownership of the ByteBuffer. Do not modify it afterwards.
	 */
	public void append(ByteBuffer content){
		this.data.add(content);
	}
	/**
	 * Insert a length marker into the collector.
	 */
	public void pushLength(){
		this.data.add(null);
	}
	/**
	 * Remove the last length marker (added by pushLength) and replace it with the specified type marker
	 * and the encoded amount of bytes added to this collector since the length marker was inserted.
	 */
	public void popLength(int type){
		int index = data.size();
		int totalLength = 0;
		while(true){
			index--;
			ByteBuffer c = data.get(index);
			if(c != null){
				totalLength += c.remaining();
			}else{
				break;
			}
		}
		int lengthOfLength = Asn1Utils.lengthOfLength(totalLength);
		ByteBuffer headerContainer = ByteBuffer.allocate(lengthOfLength + 1);
		headerContainer.put((byte)type);
		Asn1Utils.putLength(headerContainer, totalLength);
		data.set(index, headerContainer.flip());
	}
	/**
	 * Returns a stream of ByteBuffers containing all bytes added to this Asn1Collector.
	 */
	public Stream<ByteBuffer> stream(){
		return data.stream().map(ByteBuffer::asReadOnlyBuffer);
	}
	public InputStream toInputStream(){
		return new SequenceInputStream(stream().map(ByteBufferInputStream::new).iterator());
	}
	/**
	 * Converts this collector to a ByteBuffer.
	 */
	public ByteBuffer toByteBuffer(){
		int len = 0;
		for(ByteBuffer b : data){
			len += b.remaining();
		}
		ByteBuffer fin = ByteBuffer.allocate(len);
		for(ByteBuffer b : data){
			fin.put(b.asReadOnlyBuffer());
		}
		return fin.flip();
	}
	/**
	 * Print information about this collector to the standard output.
	 */
	public void printData(){
		System.out.println("=====");
		for(ByteBuffer buf : data){
			ByteBuffer k = buf.asReadOnlyBuffer();
			while(k.hasRemaining()){
				int v = k.get() & 0xff;
				System.out.print(" ");
				System.out.print(Integer.toHexString(v >> 4));
				System.out.print(Integer.toHexString(v & 0xf));
			}
			System.out.println();
		}
		System.out.println("=====");
	}
}
