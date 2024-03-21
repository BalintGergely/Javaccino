package net.balintgergely.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class SequenceInputStream extends InputStream{
	private InputStream in;
	private Iterator<? extends InputStream> input;
	public SequenceInputStream(Iterator<? extends InputStream> input){
		this.input = input;
	}
	private boolean checkStream(){
		while(in == null){
			if(!input.hasNext()){
				return false;
			}
			in = input.next();
		}
		return true;
	}
	@Override
	public int available() throws IOException {
		if(checkStream()){
			return in.available();
		}else{
			return 0;
		}
	}
	@Override
	public void close() throws IOException {
		IOException ioe = null;
		while(checkStream()){
			try{
				in.close();
			}catch(IOException e){
				if(ioe == null){
					ioe = e;
				}else{
					ioe.addSuppressed(e);
				}
			}finally{
				in = null;
			}
		}
		if(ioe != null){
			throw ioe;
		}
	}
	@Override
	public int read() throws IOException {
		while(checkStream()){
			int v = in.read();
			if(0 <= v){
				return v;
			}
			in = null;
		}
		return -1;
	}
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		while(checkStream()){
			int v = in.read(b, off, len);
			if(0 <= v){
				return v;
			}
			in = null;
		}
		return -1;
	}
	@Override
	public long skip(long n) throws IOException {
		if(checkStream()){
			return in.skip(n);
		}else{
			return 0;
		}
	}
}
