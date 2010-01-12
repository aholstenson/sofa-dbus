package se.l4.sofa.dbus.spi;

/**
 * Struct, contains zero or more objects.
 * 
 * @author Andreas Holstenson
 *
 */
public class Struct
{
	private final Object[] data;
	
	public Struct(Object... data)
	{
		this.data = data;
	}
	
	public Object[] getData()
	{
		return data;
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder();
		b.append('(');
		
		boolean first = true;
		for(Object o : data)
		{
			if(first)
			{
				first = false;
			}
			else
			{
				b.append(", ");
			}
			
			b.append(o);
		}
		
		b.append(')');
		
		return b.toString();
	}
}
