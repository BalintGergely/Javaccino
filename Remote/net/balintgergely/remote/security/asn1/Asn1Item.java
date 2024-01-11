package net.balintgergely.remote.security.asn1;

/**
 * An abstract Asn1Item.
 */
public interface Asn1Item {
	/**
	 * Encodes and appends this Asn1Item to the specified collector.
	 */
	public void writeTo(Asn1Collector collector);
	/**
	 * Creates a deep copy of this Asn1Item if it is mutable.
	 * If this Asn1Item is immutable, itself is returned.
	 */
	public default Asn1Item duplicate(){
		return this;
	}
	/**
	 * Reinterpret this Asn1Item as the specified type.
	 */
	public default <E extends Asn1Item> E as(Class<E> type){
		return type.cast(this);
	}
}
