package net.balintgergely.remote.security.asn1;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public enum Asn1AlgorithmId implements Asn1Item{
	SHA1("1.3.14.3.2.26","SHA-1", "SHA", "SHA1"),
	SHA256("2.16.840.1.101.3.4.2.1", "SHA-256", "SHA256"),
	PSS("1.2.840.113549.1.1.10","RSASSA-PSS", "PSS"),
	MGF1("1.2.840.113549.1.1.8","MGF1");
	@SuppressWarnings("unchecked")
	private static final Map<Object,Asn1AlgorithmId> LOOKUP = Map.ofEntries(
			Stream.of(Asn1AlgorithmId.values())
			.mapMulti((a,c) -> {
				c.accept(Map.entry(a, a));
				c.accept(Map.entry(a.objectId,a));
				a.names.stream().map(n -> Map.entry(n, a)).forEach(c);
			})
			.toArray(Map.Entry[]::new));
	public final String objectId;
	public final String stdName;
	public final Set<String> names;
	private Asn1AlgorithmId(String id,String... names){
		this.objectId = id;
		this.stdName = names[0];
		this.names = Set.of(names);
	}
	public static Asn1AlgorithmId lookup(Object data){
		return LOOKUP.get(data);
	}
	public static Asn1AlgorithmId strictLookup(Object data){
		return Objects.requireNonNull(LOOKUP.get(data),"Strict lookup");
	}
	@Override
	public void writeTo(Asn1Collector collector) {
		new Asn1ObjectIdentifier(objectId).writeTo(collector);
	}
}
