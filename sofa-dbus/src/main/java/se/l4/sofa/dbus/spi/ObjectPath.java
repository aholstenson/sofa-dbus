package se.l4.sofa.dbus.spi;

import se.l4.sofa.dbus.DType;

/**
 * Class representing an object path ({@link DType#OBJECT_PATH}).
 *  
 * @author Andreas Holstenson
 *
 */
public class ObjectPath
{
	private final String path;
	
	public ObjectPath(String path)
	{
		this.path = path;
	}
	
	public String getPath()
	{
		return path;
	}
	
	@Override
	public String toString()
	{
		return "ObjectPath[" + path + "]";
	}
}
