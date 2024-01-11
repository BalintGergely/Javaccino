package net.balintgergely.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream{
	private ByteBuffer buffer;
	public ByteBufferInputStream(ByteBuffer data){
		this.buffer = data;
	}
	@Override
	public int available(){
		return buffer.remaining();
	}
	@Override
	public void close(){
		buffer = null;
	}
	@Override
	public void mark(int readlimit) {
		buffer.mark();
	}
	@Override
	public boolean markSupported() {
		return true;
	}
	@Override
	public int read(){
		if(buffer.hasRemaining()){
			return buffer.get() & 0xff;
		}else{
			return -1;
		}
	}
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if(!buffer.hasRemaining()){
			return -1;
		}
		if(len > buffer.remaining()){
			len = buffer.remaining();
		}
		buffer.get(b, off, len);
		return len;
	}
	@Override
	public byte[] readAllBytes() throws IOException {
		byte[] k = new byte[buffer.remaining()];
		buffer.get(k);
		return k;
	}
	@Override
	public void reset() throws IOException {
		buffer.reset();
	}
	@Override
	public long skip(long n) throws IOException {
		if(n < 0 || n > buffer.remaining()){
			n = buffer.remaining();
		}
		buffer.position(buffer.position() + (int)n);
		return n;
	}
	@Override
	public long transferTo(OutputStream out) throws IOException {
		if(buffer.hasArray()){
			int r = buffer.remaining();
			out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), r);
			buffer.position(buffer.limit());
			return r;
		}else{
			return super.transferTo(out);
		}
	}
}
