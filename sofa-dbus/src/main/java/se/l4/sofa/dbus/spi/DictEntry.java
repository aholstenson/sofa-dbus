package se.l4.sofa.dbus.spi;

import se.l4.sofa.dbus.DType;

/**
 * Dictionary entry ({@link DType#DICT_ENTRY}).
 * 
 * @author Andreas Holstenson
 *
 */
public class DictEntry
{
	private final Object key;
	private final Object value;
	
	public DictEntry(Object key, Object value)
	{
		this.key = key;
		this.value = value;
	}
	
	public Object getKey()
	{
		return key;
	}
	
	public Object getValue()
	{
		return value;
	}
	
	@Override
	public String toString()
	{
		return "{" + key + ", " + value + "}";
	}
}
