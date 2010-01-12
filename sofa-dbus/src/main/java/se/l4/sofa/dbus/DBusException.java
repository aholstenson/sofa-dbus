package se.l4.sofa.dbus;


/**
 * Base exception for DBus, thrown by most methods and is required to be
 * thrown by methods in a {@link DBusInterface}. Maps to the DBus error
 * named {@code org.freedesktop.DBus.Error.Failed}.
 * 
 * @author Andreas Holstenson
 *
 */
@Name("org.freedesktop.DBus.Error.Failed")
public class DBusException
	extends Exception
{
	@StructPosition(0)
	private final String message;

	public DBusException(String message)
	{
		this(message, null);
	}

	public DBusException(Throwable cause)
	{
		this(null, cause);
	}
	
	public DBusException(String message, Throwable cause)
	{
		super(message, cause);
		
		this.message = message == null ? "" : message;
	}
}
