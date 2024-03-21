package net.balintgergely.remote.security.ssh;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class WireReader implements Closeable{
	private InputStream in;
	public WireReader(InputStream input){
		this.in = input;
	}
	public byte readByte() throws IOException{
		int v = in.read();
		if(v < 0){
			throw new EOFException();
		}
		return (byte)v;
	}
	public boolean readBoolean() throws IOException{
		return readByte() != 0;
	}
	public int readInt() throws IOException{
		int a = readByte() & (int)0xff;
		int b = readByte() & (int)0xff;
		int c = readByte() & (int)0xff;
		int d = readByte() & (int)0xff;
		return (a << 24) | (b << 16) | (c << 8) | d;
	}
	public long readLong() throws IOException{
		long a = readInt() & (long)0xffffffff;
		long b = readInt() & (long)0xffffffff;
		return (a << 32) | b;
	}
	public byte[] readByteArray() throws IOException{
		int len = readInt();
		byte[] data = new byte[len];
		if(in.readNBytes(data, 0, len) != len){
			throw new EOFException();
		}
		return data;
	}
	public void skip(int bytes) throws IOException{
		in.skipNBytes(bytes);
	}
	public BigInteger readBigInteger() throws IOException{
		return new BigInteger(readByteArray());
	}
	public String readString() throws IOException{
		return new String(readByteArray(),StandardCharsets.UTF_8);
	}
	/**
	 * Let's do more of those!
	 */
	public WireReader readBlob() throws IOException{
		return new WireReader(new ByteArrayInputStream(readByteArray()));
	}
	@Override
	public void close() throws IOException {
		in.close();
	}
}
