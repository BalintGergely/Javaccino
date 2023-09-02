package net.balintgergely.remote.security.asn1;

import java.nio.ByteBuffer;

public class Asn1Utils {
	public static long getLength(ByteBuffer in){
		if(!in.hasRemaining()){
			return -1;
		}
		byte first = in.get();
		if(first >= 0){
			return first;
		}
		int count = first & 0x7F;
		if(count > 8){
			throw new RuntimeException("Length too big!");
		}
		if(count > in.remaining()){
			in.position(in.position() - 1);
			return -1;
		}
		long cap = 0;
		while(count > 0){
			count--;
			cap = (cap << 8) | (in.get() & 0xFFL);
		}
		return cap;
	}
	public static void putLength(ByteBuffer out,long len){
		if(len < 0x80){
			out.put((byte)len);
		}else{
			int k = 1;
			while(len >= (1 << (k*8))){
				k++;
			}
			out.put((byte)(0x80 + k));
			while(k != 0){
				k--;
				out.put((byte)(len >> (k * 8)));
			}
		}
	}
	public static int lengthOfLength(long len){
		if(len < 0x80){
			return 1;
		}else{
			int k = 1;
			while(len >= (1 << (k*8))){
				k++;
			}
			return k + 1;
		}
	}
}
