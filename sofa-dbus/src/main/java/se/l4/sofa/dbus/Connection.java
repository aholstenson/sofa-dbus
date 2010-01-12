package se.l4.sofa.dbus;

import java.io.IOException;
import java.util.Set;


public interface Connection
{

	void connect() throws IOException;

	/**
	 * Get the first name of the connection.
	 * 
	 * @return
	 */
	String getFirstName();
	
	/**
	 * Get the names that this connection currently owns on the bus.
	 * 
	 * @return
	 */
	Set<String> getNames();

	DBus getDBus();
	
	/**
	 * Get a given path on the given bus.
	 * 
	 * @param bus
	 * 		bus to send to
	 * @param path
	 * 		path on the bus
	 * @return
	 */
	Path get(String bus, String path);
	
	/**
	 * Get a given path for this connection. Shortcut for 
	 * {@code conn.get(conn.getFirstName(), path)}.
	 *  
	 * @param path
	 * 		path on the bus
	 * @return
	 */
	Path getLocal(String path);

	<T extends DBusInterface> T get(String bus, String path, Class<T> dbusInterface)
		throws DBusException;

	<T extends DBusSignal> void addSignalListener(Class<T> signal, SignalListener<T> listener)
		throws DBusException;

	<T extends DBusSignal> void addSignalListener(String path, Class<T> signal,
			SignalListener<T> listener) 
		throws DBusException;

	<T extends DBusSignal> void removeSignalListener(Class<T> signal, SignalListener<T> listener)
		throws DBusException;
	
	void export(String path, DBusInterface object)
		throws DBusException;

	void sendSignal(String path, DBusSignal signal)
		throws DBusException;

}