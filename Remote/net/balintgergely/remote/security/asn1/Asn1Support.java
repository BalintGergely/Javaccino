package net.balintgergely.remote.security.asn1;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import static net.balintgergely.remote.security.asn1.Asn1ContextSequence.wrap;

public class Asn1Support {
	public static record AlgorithmId(Asn1AlgorithmId algorithm,AlgorithmParameterSpec parameters){}
	public static Asn1Item makeAlgorithmId(Object algorithm,AlgorithmParameterSpec spec){
		Asn1Item algorithmIdentifier = Asn1AlgorithmId.lookup(algorithm);
		Asn1Item algorithmParameters;
		if(spec == null){
			algorithmParameters = Asn1Null.INSTANCE;
		}else if(spec instanceof PSSParameterSpec ps){

			// https://www.oss.com/asn1/resources/asn1-made-simple/asn1-quick-reference/asn1-tags.html

			ArrayList<Asn1Item> params = new ArrayList<>(4);

			Asn1AlgorithmId digestAlgorithm = Asn1AlgorithmId.lookup(ps.getDigestAlgorithm());
			Asn1AlgorithmId mgfAlgorithm = Asn1AlgorithmId.lookup(ps.getMGFAlgorithm());
			AlgorithmParameterSpec mgfAlgorithmSpec = ps.getMGFParameters();
			int saltLength = ps.getSaltLength();
			int trailerField = ps.getTrailerField();

			// This logic is to avoid encoding any default values.

			if(digestAlgorithm != Asn1AlgorithmId.SHA1){
				params.add(wrap(0xA0,makeAlgorithmId(digestAlgorithm, null)));
			}
			if(!(mgfAlgorithm == Asn1AlgorithmId.MGF1 &&
				mgfAlgorithmSpec instanceof MGF1ParameterSpec mgf1spec &&
				Asn1AlgorithmId.lookup(mgf1spec.getDigestAlgorithm()) == Asn1AlgorithmId.SHA1)){
				params.add(wrap(0xA1, makeAlgorithmId(mgfAlgorithm, mgfAlgorithmSpec)));
			}
			if(saltLength != 20){
				params.add(wrap(0xA2, new Asn1Integer(saltLength)));
			}
			if(trailerField != PSSParameterSpec.TRAILER_FIELD_BC){
				params.add(wrap(0xA3, new Asn1Integer(trailerField)));
			}
			algorithmParameters = new Asn1Sequence(params);
		}else if(spec instanceof MGF1ParameterSpec ms){
			algorithmParameters = makeAlgorithmId(ms.getDigestAlgorithm(), null);
		}else{
			throw new RuntimeException();
		}
		return new Asn1Sequence(algorithmIdentifier, algorithmParameters);
	}
	private static final Map<Asn1AlgorithmId,MGF1ParameterSpec> HP_MAP = Map.of(
		Asn1AlgorithmId.SHA1, MGF1ParameterSpec.SHA1,
		Asn1AlgorithmId.SHA256, MGF1ParameterSpec.SHA256
	);
	private static MGF1ParameterSpec hpLookup(Asn1AlgorithmId id){
		return Objects.requireNonNull(HP_MAP.get(id));
	}
	public static AlgorithmId parseAlgorithmId(Asn1Item dspec){
		Asn1ObjectSequence argsSequence = dspec.as(Asn1Sequence.class).ofLength(2);
		Asn1AlgorithmId alg = Asn1AlgorithmId.strictLookup(
			argsSequence.items().get(0).as(Asn1ObjectIdentifier.class).toString()
		);
		if(alg == Asn1AlgorithmId.MGF1){
			AlgorithmId sub = parseAlgorithmId(argsSequence.items().get(1));
			return new AlgorithmId(alg, hpLookup(sub.algorithm));
		}else if(alg == Asn1AlgorithmId.SHA1 || alg == Asn1AlgorithmId.SHA256){
			argsSequence.items().get(1).as(Asn1Null.class);
			return new AlgorithmId(alg, null);
		}else if(alg == Asn1AlgorithmId.PSS){
			Asn1Sequence params = argsSequence.items().get(1).as(Asn1Sequence.class);
			AlgorithmId digestAlgorithm = null;
			AlgorithmId mgfAlgorithm = null;
			BigInteger saltLength = null;
			BigInteger trailerField = null;
			boolean[] seen = new boolean[4];
			for(Asn1Item item : params.items()){
				Asn1ContextSequence cs = item.as(Asn1ContextSequence.class);
				cs.ofLength(1);
				int t = cs.getType() & 0x1f;
				if(seen[t]){
					throw new IllegalArgumentException();
				}
				seen[t] = true;
				switch(t){
					case 0:
						digestAlgorithm = parseAlgorithmId(cs.items().get(0));
						break;
					case 1:
						mgfAlgorithm = parseAlgorithmId(cs.items().get(0));
						break;
					case 2:
						saltLength = cs.items().get(0).as(Asn1Integer.class).value;
						break;
					case 3:
						trailerField = cs.items().get(0).as(Asn1Integer.class).value;
						break;
					default:
						throw new IllegalArgumentException("Unknown context tag");
				}
			}
			String mdname = Asn1AlgorithmId.SHA1.stdName;
			String mgfname = Asn1AlgorithmId.MGF1.stdName;
			MGF1ParameterSpec mgfspec = MGF1ParameterSpec.SHA1;
			int salt = 20;
			int trailer = PSSParameterSpec.TRAILER_FIELD_BC;
			if(digestAlgorithm != null){
				hpLookup(digestAlgorithm.algorithm);
				mdname = digestAlgorithm.algorithm.stdName;
			}
			if(mgfAlgorithm != null){
				mgfspec = (MGF1ParameterSpec)mgfAlgorithm.parameters;
			}
			if(saltLength != null){
				salt = saltLength.intValueExact();
			}
			if(trailerField != null){
				trailer = trailerField.intValueExact();
			}
			return new AlgorithmId(alg, new PSSParameterSpec(mdname, mgfname, mgfspec, salt, trailer));
		}else{
			throw new IllegalArgumentException("Unknown algorithm");
		}
	}
	public static AlgorithmId parseSignatureAlgorithmId(Asn1Item item){
		AlgorithmId algid = parseAlgorithmId(item);
		if(algid.algorithm != Asn1AlgorithmId.PSS){
			throw new IllegalArgumentException("Only PSS is supported now.");
		}
		return algid;
	}
	public static Signature createSignature(AlgorithmId algid) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException{
		Signature sig = Signature.getInstance(algid.algorithm.stdName);
		sig.setParameter(algid.parameters);
		return sig;
	}
}
