package se.l4.sofa.dbus.spi;

import java.math.BigInteger;

/**
 * Unsigned 64-bit integer.
 * 
 * @author Andreas Holstenson
 *
 */
public class UInt64
{
	private final BigInteger value;
	
	public UInt64(BigInteger value)
	{
		this.value = value;
	}
	
	public BigInteger getValue()
	{
		return value;
	}
}
