package net.balintgergely.remote.security.asn1;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import net.balintgergely.util.ByteBufferInputStream;

public class Asn1Collector{
	private ArrayList<ByteBuffer> data = new ArrayList<>();
	public void append(Asn1Collector that){
		data.addAll(that.data);
	}
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
	public void append(ByteBuffer content){
		this.data.add(content);
	}
	public void pushLength(){
		this.data.add(null);
	}
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
	public Stream<ByteBuffer> stream(){
		return data.stream().map(ByteBuffer::duplicate);
	}
	public InputStream toInputStream(){
		return new SequenceInputStream(Collections.enumeration(
			new AbstractCollection<ByteBufferInputStream>() {
				@Override
				public Iterator<ByteBufferInputStream> iterator() {
					return data.stream().map(ByteBuffer::duplicate).map(ByteBufferInputStream::new).iterator();
				}
				@Override
				public int size() {
					return data.size();
				}
			}
		));
	}
	public ByteBuffer toByteBuffer(){
		int len = 0;
		for(ByteBuffer b : data){
			len += b.remaining();
		}
		ByteBuffer fin = ByteBuffer.allocate(len);
		for(ByteBuffer b : data){
			fin.put(b.duplicate());
		}
		return fin.flip();
	}
	public void printData(){
		System.out.println("=====");
		for(ByteBuffer buf : data){
			ByteBuffer k = buf.duplicate();
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
