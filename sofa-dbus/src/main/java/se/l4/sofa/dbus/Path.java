package se.l4.sofa.dbus;

/**
 * Encapsulates a certain path on a given bus. Delegates to a {@link Connection}
 * but infers certain parameters.
 * 
 * <p>
 * Example:
 * <pre>
 * Connection conn =  // ... get connection 
 * 
 * Path path = conn.get("com.example", "/com/example");
 * 
 * // get a service on the path
 * ExampleService service = path.get(ExampleService.class);
 * 
 * // get a service on a relative path (resolves to /com/example/test) 
 * ExampleService service2 = path.get("test", ExampleService.class);
 * 
 * // get a service on a relative path by getting a new Path object
 * Path testPath = path.resolve("test");
 * ExampleSerivce service3 = testPath.get(ExampleService.class); 
 * </pre>
 * 
 * @author Andreas Holstenson
 *
 */
public interface Path
{
	/**
	 * Get the bus of the path.
	 * 
	 * @return
	 */
	String getBus();
	
	/**
	 * Get the path as a string.
	 * 
	 * @return
	 */
	String getPath();
	
	/**
	 * Retrieve a DBus interface on the given path. 
	 * 
	 * @param <T>
	 * @param dbusInterface
	 * @return
	 * @throws DBusException
	 * @see Connection#get(String, String, Class) 
	 */
	<T extends DBusInterface> T get(Class<T> dbusInterface)
		throws DBusException;
	
	/**
	 * Retrieve a DBus interface on a given relative path.
	 * 
	 * @param <T>
	 * @param relativePath
	 * @param dbusInterface
	 * @return
	 * @throws DBusException
	 * @see Connection#get(String, String, Class)
	 */
	<T extends DBusInterface> T get(String relativePath, Class<T> dbusInterface)
		throws DBusException;
	
	/**
	 * Add a signal listener for this path.
	 * 
	 * @param <T>
	 * @param signal
	 * @param listener
	 * @throws DBusException
	 * @see Connection#addSignalListener(Class, SignalListener)
	 */
	<T extends DBusSignal> void addSignalListener(Class<T> signal, SignalListener<T> listener)
		throws DBusException;
	
	/**
	 * Add a signal listener for a relative path.
	 * 
	 * @param <T>
	 * @param relativePath
	 * @param signal
	 * @param listener
	 * @throws DBusException
	 * @see Connection#addSignalListener(Class, SignalListener)
	 */
	<T extends DBusSignal> void addSignalListener(String relativePath, Class<T> signal, SignalListener<T> listener)
		throws DBusException;
	
	/**
	 * Send a signal for this path.
	 * 
	 * @param signal
	 * @throws DBusException
	 * @see Connection#sendSignal(String, DBusSignal)
	 */
	void sendSignal(DBusSignal signal)
		throws DBusException;
	
	/**
	 * Send a signal for a relative path.
	 * 
	 * @param relativePath
	 * @param signal
	 * @throws DBusException
	 * @see Connection#sendSignal(String, DBusSignal)
	 */
	void sendSignal(String relativePath, DBusSignal signal)
		throws DBusException;
	
	/**
	 * Export a DBus interface.
	 * 
	 * @param object
	 * @throws DBusException
	 * @see Connection#export(String, DBusInterface)
	 */
	void export(DBusInterface object)
		throws DBusException;
	
	/**
	 * Export a DBus interface on a relative path.
	 * 
	 * @param path
	 * @param object
	 * @throws DBusException
	 * @see Connection#export(String, DBusInterface)
	 */
	void export(String relativePath, DBusInterface object)
		throws DBusException;
	
	/**
	 * Resolve a path relative to this one.
	 * 
	 * @param relativePath
	 * @return
	 */
	Path resolve(String relativePath);
}
