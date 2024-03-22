package net.balintgergely.security.ssh;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class WireWriter implements Closeable, Flushable{
	private OutputStream out;
	public WireWriter(OutputStream out){
		this.out = out;
	}
	public void writeByte(byte b) throws IOException{
		out.write(b);
	}
	public void writeBoolean(boolean value) throws IOException{
		out.write(value ? 1 : 0);
	}
	public void writeInt(int value) throws IOException{
		writeByte((byte)(value >> 24));
		writeByte((byte)(value >> 16));
		writeByte((byte)(value >> 8));
		writeByte((byte)(value >> 0));
	}
	public void writeLong(long value) throws IOException{
		writeInt((int)(value >> 32));
		writeInt((int)(value >> 0));
	}
	public void writeByteArray(byte[] data) throws IOException{
		writeByteArray(data, 0, data.length);
	}
	public void writeByteArray(byte[] data,int off,int len) throws IOException{
		if(len == 0){
			writeInt(0);
			return;
		}
		if(off < 0 || len < 0 || off + len > data.length){
			throw new IOException();
		}
		writeInt(len);
		out.write(data, off, len);
	}
	public void writeBigInteger(BigInteger data) throws IOException{
		writeByteArray(data.toByteArray());
	}
	public void writeString(String data) throws IOException{
		writeByteArray(data.getBytes(StandardCharsets.UTF_8));
	}
	/**
	 * Hell aren't nested length prefixed structures great?!
	 */
	public WireWriter writeBlob(){
		return new WireWriter(new BlobOutput());
	}
	@Override
	public void close() throws IOException {
		out.close();
	}
	@Override
	public void flush() throws IOException {
		out.flush();
	}
	private class BlobOutput extends ByteArrayOutputStream{
		@Override
		public void close() throws IOException{
			writeByteArray(this.buf, 0, this.count);
			this.buf = null;
		}
	}
}
