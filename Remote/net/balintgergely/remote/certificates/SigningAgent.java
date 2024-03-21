package net.balintgergely.remote.certificates;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.Spliterator;

import net.balintgergely.remote.security.asn1.Asn1Item;

public interface SigningAgent {
	public Asn1Item getAlgorithmId();
	public byte[] sign(Spliterator<ByteBuffer> data) throws GeneralSecurityException;
}
