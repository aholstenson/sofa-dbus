package se.l4.sofa.dbus;

/**
 * Holder class with generic type, used together with {@link Out} to enable
 * output of multiple parameters.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class Holder<T>
{
	private T value;
	
	public Holder()
	{
	}

	public void setValue(T value)
	{
		this.value = value;
	}
	
	public T getValue()
	{
		return value;
	}
}
