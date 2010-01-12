package se.l4.sofa.dbus;

/**
 * Interface that implementations of a {@link DBusInterface} can implement to
 * allow it to be notified when it is either exported or unexported to take
 * extra actions. <b>Note:</b> This is placed on the implementation of a
 * service and not on the interface that defines it.
 * 
 * @author Andreas Holstenson
 *
 */
public interface DBusLifecycle
{
	/**
	 * This interface has now been exported.
	 * 
	 * @param path
	 * @throws DBusException
	 */
	void interfaceExported(Path path)
		throws DBusException;
	
	/**
	 * This interface is no longer exported.
	 * 
	 * @param path
	 * @throws DBusException
	 */
	void interfaceUnexported(Path path)
		throws DBusException;
}
