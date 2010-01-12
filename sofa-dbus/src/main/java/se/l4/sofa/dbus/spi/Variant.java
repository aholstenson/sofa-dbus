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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Variant other = (Variant) obj;
		if(value == null)
		{
			if(other.value != null)
				return false;
		}
		else if(!value.equals(other.value))
			return false;
		return true;
	}	
}
