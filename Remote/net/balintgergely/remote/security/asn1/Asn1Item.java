package net.balintgergely.remote.security.asn1;

public interface Asn1Item {
	public void writeTo(Asn1Collector collector);
	public default Asn1Item duplicate(){
		return this;
	}
	public default <E extends Asn1Item> E as(Class<E> type){
		return type.cast(this);
	}
}
