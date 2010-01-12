package se.l4.sofa.dbus;


/**
 * Thrown when a method is unknown.
 * 
 * @author Andreas Holstenson
 *
 */
@Name("org.freedesktop.DBus.Error.UnknownMethod")
public class UnknownMethodException
	extends DBusException
{
	public UnknownMethodException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnknownMethodException(String message)
	{
		super(message);
	}

	public UnknownMethodException(Throwable arg0)
	{
		super(arg0);
	}
	
}
