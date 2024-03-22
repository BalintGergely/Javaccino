package net.balintgergely.security;

import static net.balintgergely.security.asn1.Asn1ContextSequence.wrap;

import java.io.IOException;
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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.x500.X500Principal;

import net.balintgergely.security.asn1.Asn1BitString;
import net.balintgergely.security.asn1.Asn1Collector;
import net.balintgergely.security.asn1.Asn1GeneralizedTime;
import net.balintgergely.security.asn1.Asn1Integer;
import net.balintgergely.security.asn1.Asn1Item;
import net.balintgergely.security.asn1.Asn1RawItem;
import net.balintgergely.security.asn1.Asn1Sequence;
import net.balintgergely.security.asn1.Asn1Support;
import net.balintgergely.security.asn1.Asn1Support.AlgorithmId;

public class CertificateMaker {
	@SuppressWarnings("unchecked")
	private static final <E extends Throwable> void throwUnchecked(Throwable t) throws E{ throw (E)t; }
	private AtomicReference<BigInteger> serialNumber = new AtomicReference<BigInteger>(BigInteger.ZERO);
	//private X500Principal myIdentity;
	private Asn1Item myIdentityEncoded;
	private Asn1Item privateKeyAlgorithmId;
	private AlgorithmId parsedAlgorithmId;
	private PrivateKey privateKey;
	public CertificateMaker(X500Principal myIdentity,PrivateKey privateKey){
		//this.myIdentity = myIdentity;
		this.myIdentityEncoded = new Asn1RawItem(ByteBuffer.wrap(myIdentity.getEncoded()));
		this.privateKey = privateKey;
		Asn1Sequence issuerKeyEncoded = new Asn1RawItem(
			ByteBuffer.wrap(privateKey.getEncoded())).as(Asn1Sequence.class);
		privateKeyAlgorithmId = issuerKeyEncoded.items().get(1)
			.as(Asn1RawItem.class).duplicate()
			.as(Asn1Sequence.class);
		parsedAlgorithmId = Asn1Support.parseSignatureAlgorithmId(privateKeyAlgorithmId);
	}
	public X509Certificate makeX509Certificate(X500Principal subject,PublicKey subjectKey,Temporal validUntil) throws GeneralSecurityException, IOException{
		// https://www.rfc-editor.org/rfc/rfc5280
		// Subject key encodes itself.
		Asn1Item publicKeyInfo = new Asn1RawItem(ByteBuffer.wrap(subjectKey.getEncoded()));
		//Asn1BitString issuerUID = new Asn1BitString(new byte[1], 0);
		//Asn1BitString subjectUID = new Asn1BitString(new byte[1], 0);
		// Issuer key encodes with itself the signature algorithm id.
		Asn1Item subjectName = new Asn1RawItem(ByteBuffer.wrap(subject.getEncoded()));
		Temporal validFrom = 
			validUntil instanceof OffsetDateTime ? OffsetDateTime.now() : LocalDateTime.now();
		Asn1Item validity = new Asn1Sequence(
			new Asn1GeneralizedTime(validFrom),
			new Asn1GeneralizedTime(validUntil)
		);
		BigInteger serial = serialNumber.accumulateAndGet(BigInteger.ONE,BigInteger::add);
		Asn1Item tbs = new Asn1Sequence(
			wrap(0x80,new Asn1Integer(1)), // Version number
			new Asn1Integer(serial),
			privateKeyAlgorithmId,
			myIdentityEncoded,
			validity,
			subjectName,
			publicKeyInfo
		);
		Asn1Collector tbsCollector = new Asn1Collector();
		tbs.writeTo(tbsCollector);
		Signature sign = Asn1Support.createSignature(parsedAlgorithmId);
		sign.initSign(privateKey);
		tbsCollector.stream().forEach(t -> {
			try{
				sign.update(t);
			}catch(SignatureException e){
				throwUnchecked(e);
			}
		});
		Asn1Collector rootCollector = new Asn1Collector();
		Asn1Item root = new Asn1Sequence(
			tbs,
			privateKeyAlgorithmId,
			new Asn1BitString(sign.sign(), 0)
		);
		root.writeTo(rootCollector);
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate cert = (X509Certificate)cf.generateCertificate(rootCollector.toInputStream());
		return cert;
	}
	public static void main(String[] atgs) throws Exception{
		KeyPairGenerator keygen = KeyPairGenerator.getInstance("PSS");
		PSSParameterSpec parameterSpec = new PSSParameterSpec(
			"SHA256",
			"MGF1",
				new MGF1ParameterSpec("SHA256"), 20, 1);
		RSAKeyGenParameterSpec keygenSpec = new RSAKeyGenParameterSpec(
			4096, RSAKeyGenParameterSpec.F4, parameterSpec);
		keygen.initialize(keygenSpec);
		KeyPair myKeyPair = keygen.generateKeyPair();
		//makeCertificate(myKeyPair,myKeyPair.getPrivate());
		X509Certificate cert = new CertificateMaker(
			new X500Principal("CN=Test issuer"),
			myKeyPair.getPrivate()).makeX509Certificate(
			new X500Principal("CN=Test subject"),
			myKeyPair.getPublic(),
			OffsetDateTime.now().plusHours(1));
		cert.verify(myKeyPair.getPublic());
		Asn1Item item = new Asn1RawItem(ByteBuffer.wrap(cert.getEncoded())).resolve(true);
		System.out.println(item.toString());
	}
}
