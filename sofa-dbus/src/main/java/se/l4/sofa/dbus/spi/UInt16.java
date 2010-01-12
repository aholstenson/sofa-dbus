package se.l4.sofa.dbus.spi;

/**
 * Unsigned 16-bit integer.
 * 
 * @author Andreas Holstenson
 *
 */
public class UInt16
{
	private final int value;
	
	public UInt16(int value)
	{
		this.value = value;
	}
	
	public int getValue()
	{
		return value;
	}
}
