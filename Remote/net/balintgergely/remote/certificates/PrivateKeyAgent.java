package net.balintgergely.remote.certificates;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Spliterator;

import net.balintgergely.remote.security.asn1.Asn1Item;
import net.balintgergely.remote.security.asn1.Asn1RawItem;
import net.balintgergely.remote.security.asn1.Asn1Sequence;
import net.balintgergely.remote.security.asn1.Asn1Support;
import net.balintgergely.remote.security.asn1.Asn1Support.AlgorithmId;

public class PrivateKeyAgent implements SigningAgent{
	@SuppressWarnings("unchecked")
	private static <T extends Throwable,E> E throwUnchecked(Throwable t) throws T{
		throw (T)t;
	}
	private PrivateKey key;
	private Asn1Item algorithmId;
	private AlgorithmId parsedAlgorithmId;
	public PrivateKeyAgent(PrivateKey key){
		this.key = key;
		this.algorithmId = new Asn1RawItem(ByteBuffer.wrap(key.getEncoded()))
			.as(Asn1Sequence.class).items().get(1)
			.as(Asn1RawItem.class).duplicate().toReadOnly()
			.as(Asn1Sequence.class);
		parsedAlgorithmId = Asn1Support.parseSignatureAlgorithmId(algorithmId);
	}
	@Override
	public Asn1Item getAlgorithmId() {
		return algorithmId;
	}
	@Override
	public byte[] sign(Spliterator<ByteBuffer> data) throws GeneralSecurityException{
		Signature sign = Asn1Support.createSignature(parsedAlgorithmId);
		sign.initSign(key);
		data.forEachRemaining(t -> {
			try{
				sign.update(t);
			}catch(SignatureException e){
				throwUnchecked(e);
			}
		});
		return sign.sign();
	}
}
