package se.l4.sofa.dbus.spi;

import java.util.Arrays;

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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data);
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
		Struct other = (Struct) obj;
		if(!Arrays.equals(data, other.data))
			return false;
		return true;
	}
}
