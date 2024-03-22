package net.balintgergely.security;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;

import javax.security.auth.x500.X500Principal;

import net.balintgergely.security.asn1.Asn1BitString;
import net.balintgergely.security.asn1.Asn1Collector;
import net.balintgergely.security.asn1.Asn1ContextSequence;
import net.balintgergely.security.asn1.Asn1GeneralizedTime;
import net.balintgergely.security.asn1.Asn1Integer;
import net.balintgergely.security.asn1.Asn1Item;
import net.balintgergely.security.asn1.Asn1RawItem;
import net.balintgergely.security.asn1.Asn1Sequence;
import net.balintgergely.security.asn1.Asn1Support;

public class CertificateBuilder {
	@SuppressWarnings("unchecked")
	private static final <E extends Throwable> void throwUnchecked(Throwable t) throws E{ throw (E)t; }
	private Asn1Integer serialNumber;
	private PrivateKey issuerKey;
	private Asn1Item issuerAlgorithmInfo;
	private Asn1Item issuerIdentity;
	private Asn1Item subjectIdentity;
	private Asn1Item subjectPublicKeyInfo;
	private Asn1GeneralizedTime validFrom;
	private Asn1GeneralizedTime validTo;
	public CertificateBuilder(){}
	public CertificateBuilder setSerialNumber(Asn1Integer serialNumber){
		this.serialNumber = serialNumber;
		return this;
	}
	public CertificateBuilder setIssuerKey(PrivateKey key){
		byte[] keyEncoded = key.getEncoded();

		issuerAlgorithmInfo = new Asn1RawItem(ByteBuffer.wrap(keyEncoded))
			.as(Asn1Sequence.class).items().get(1)
			.as(Asn1RawItem.class).duplicate()
			.as(Asn1Sequence.class);

		Arrays.fill(keyEncoded, (byte)0);

		this.issuerKey = key;
		
		return this;
	}
	public CertificateBuilder setIssuerIdentity(Asn1Item item){
		this.issuerIdentity = item;
		return this;
	}
	public CertificateBuilder setIssuerIdentity(X500Principal issuer){
		this.issuerIdentity = new Asn1RawItem(ByteBuffer.wrap(issuer.getEncoded()));
		return this;
	}
	public CertificateBuilder setSubjectIdentity(Asn1Item item){
		this.subjectIdentity = item;
		return this;
	}
	public CertificateBuilder setSubjectIdentity(X500Principal subject){
		this.subjectIdentity = new Asn1RawItem(ByteBuffer.wrap(subject.getEncoded()));
		return this;
	}
	public CertificateBuilder setSubjectKey(PublicKey key){
		this.subjectPublicKeyInfo = new Asn1RawItem(ByteBuffer.wrap(key.getEncoded()));
		return this;
	}
	public CertificateBuilder setValidFrom(Temporal from){
		this.validFrom = new Asn1GeneralizedTime(from);
		return this;
	}
	public CertificateBuilder setValidTo(Temporal to){
		this.validTo = new Asn1GeneralizedTime(to);
		return this;
	}
	public CertificateBuilder setValidity(Temporal from,TemporalAmount duration){
		setValidFrom(from);
		setValidTo(from.plus(duration));
		return this;
	}

	public Asn1Item build() throws GeneralSecurityException{

		Asn1Item tbs = new Asn1Sequence(
			Asn1ContextSequence.wrap(0x80,new Asn1Integer(1)), // Version number
			serialNumber,
			issuerAlgorithmInfo,
			issuerIdentity,
			new Asn1Sequence(validFrom,validTo),
			subjectIdentity,
			subjectPublicKeyInfo
		);

		Asn1Collector tbsCollector = new Asn1Collector();
		tbs.writeTo(tbsCollector);

		Signature sign = Asn1Support.createSignature(Asn1Support.parseSignatureAlgorithmId(issuerAlgorithmInfo));
		sign.initSign(issuerKey);
		tbsCollector.stream().forEach(t -> {
			try{
				sign.update(t);
			}catch(SignatureException e){
				throwUnchecked(e);
			}
		});

		Asn1Item root = new Asn1Sequence(
			tbs,
			issuerAlgorithmInfo,
			new Asn1BitString(sign.sign(), 0)
		);

		return root;
	}

	public static void main(String[] atgs) throws Throwable{
		KeyPairGenerator keygen = KeyPairGenerator.getInstance("PSS");
		PSSParameterSpec parameterSpec = new PSSParameterSpec(
			"SHA256",
			"MGF1",
				new MGF1ParameterSpec("SHA256"), 20, 1);
		RSAKeyGenParameterSpec keygenSpec = new RSAKeyGenParameterSpec(
			4096, RSAKeyGenParameterSpec.F4, parameterSpec);
		keygen.initialize(keygenSpec);
		KeyPair myKeyPair = keygen.generateKeyPair();

		CertificateBuilder builder = new CertificateBuilder();

		builder.setSerialNumber(new Asn1Integer(BigInteger.valueOf(42)));
		builder.setIssuerIdentity(new X500Principal("CN=Test issuer"));
		builder.setSubjectIdentity(new X500Principal("CN=Test subject"));
		builder.setIssuerKey(myKeyPair.getPrivate());
		builder.setSubjectKey(myKeyPair.getPublic());
		builder.setValidity(OffsetDateTime.now(), Duration.ofHours(1));

		Asn1Item result = builder.build();

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate)cf.generateCertificate(new Asn1Collector(result).toInputStream());

		cert.verify(myKeyPair.getPublic());

		System.out.println(result);
	}
}
