package se.l4.sofa.dbus.spi;

/**
 * Variant type, contains another object with a runtime signature.
 * 
 * @author Andreas Holstenson
 *
 */
public class Variant
{
	private final Object value;
	private final Signature signature;
	
	public Variant(Object value)
	{
		this(null, value);
	}
	
	public Variant(Signature s, Object value)
	{
		this.value = value;
		this.signature = s == null
			? Signature.parse(Marshalling.getSignature(value.getClass())) 
			: s;
	}
	
	public Signature getSignature()
	{
		return signature;
	}
	
	public Object getValue()
	{
		return value;
	}
	
	@Override
	public String toString()
	{
		return value.toString();
	}
}
