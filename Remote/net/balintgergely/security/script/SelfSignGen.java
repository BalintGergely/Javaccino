package net.balintgergely.security.script;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.Duration;
import java.time.OffsetDateTime;

import javax.security.auth.x500.X500Principal;

import net.balintgergely.security.CertificateBuilder;
import net.balintgergely.security.asn1.Asn1Integer;
import net.balintgergely.security.asn1.Asn1Item;
import net.balintgergely.security.asn1.Asn1RawItem;
import net.balintgergely.security.asn1.Asn1Utils;

public class SelfSignGen {
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

		builder.setSerialNumber(new Asn1Integer(BigInteger.valueOf(0)));
		builder.setIssuerIdentity(new X500Principal("CN=Test issuer"));
		builder.setSubjectIdentity(new X500Principal("CN=Test subject"));
		builder.setIssuerKey(myKeyPair.getPrivate());
		builder.setSubjectKey(myKeyPair.getPublic());
		builder.setValidity(OffsetDateTime.now(),Duration.ofHours(1));

		Asn1Item certificate = builder.build();

		File keyFile = new File("key.pem");
		File certFile = new File("cert.pem");

		try(Writer writer = new FileWriter(keyFile,StandardCharsets.UTF_8)){
			Asn1Item privateKey = new Asn1RawItem(ByteBuffer.wrap(myKeyPair.getPrivate().getEncoded()));
			writer.write(Asn1Utils.encode("PRIVATE KEY", privateKey));
		}

		try(Writer writer = new FileWriter(certFile,StandardCharsets.UTF_8)){
			writer.write(Asn1Utils.encode("CERTIFICATE", certificate));
		}
	}
}
