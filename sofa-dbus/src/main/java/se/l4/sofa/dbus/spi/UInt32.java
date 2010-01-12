package se.l4.sofa.dbus.spi;

/**
 * Unsigned 32-bit integer.
 * 
 * @author Andreas Holstenson
 *
 */
public class UInt32
	extends Number
{
	private final long value;
	
	public UInt32(long value)
	{
		this.value = value;
	}
	
	public long getValue()
	{
		return value;
	}

	@Override
	public double doubleValue()
	{
		return (double) value;
	}

	@Override
	public float floatValue()
	{
		return (float) value;
	}

	@Override
	public int intValue()
	{
		return (int) value;
	}

	@Override
	public long longValue()
	{
		return value;
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(value);
	}
}
