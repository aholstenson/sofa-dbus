package se.l4.sofa.dbus;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import se.l4.sofa.dbus.reflect.PathImpl;
import se.l4.sofa.dbus.reflect.SignalMessageHandler;
import se.l4.sofa.dbus.spi.Endian;

/**
 * A DBus connection that is local and works only within the program starting
 * it. A local connection is useful for testing and in the case where the
 * system communicates via DBus internally but no system/session bus is running.
 * 
 * <p>
 * Example of using {@link LocalConnection} as a fallback for the session bus:
 * 
 * <pre>
 * public Connection connectToDBus() throws DBusException {
 * 	try {
 * 		return BusConnection.session();
 * 	} catch(IOException e) {
 * 		return new LocalConnection();
 * 	}
 * }
 * </pre>
 * 
 * @author Andreas Holstenson
 *
 */
public class LocalConnection
	implements Connection
{
	private final SignalMessageHandler signals;
	private final LocalDBus dbus;
	private final String name;
	private final Set<String> names;
	private final Set<String> unmodifiableNames;
	
	private final Map<String, DBusInterface> exported;
	
	public LocalConnection()
	{
		dbus = new LocalDBus();
		signals = new SignalMessageHandler(dbus, Endian.BIG);
		
		name = ":local";
		names = new CopyOnWriteArraySet<String>();
		names.add(name);
		
		unmodifiableNames = Collections.unmodifiableSet(names);
		
		exported = new ConcurrentHashMap<String, DBusInterface>();
	}
	
	public <T extends DBusSignal> void addSignalListener(Class<T> signal,
			SignalListener<T> listener) 
		throws DBusException
	{
		signals.addListener(signal, listener);
	}

	public <T extends DBusSignal> void addSignalListener(String path,
			Class<T> signal, SignalListener<T> listener)
		throws DBusException
	{
		signals.addListener(path, signal, listener);
	}

	public void connect()
		throws IOException
	{
	}

	public void export(String path, DBusInterface object)
		throws DBusException
	{
		exported.put(path, object);
		
		if(object instanceof DBusLifecycle)
		{
			Path p = getLocal(path);
			((DBusLifecycle) object).interfaceExported(p);
		}
	}
	
	public void unexport(String path)
		throws DBusException
	{
		Object object = exported.remove(path);
		
		if(object instanceof DBusLifecycle)
		{
			Path p = getLocal(path);
			((DBusLifecycle) object).interfaceUnexported(p);
		}
	}

	public Path get(String bus, String path)
	{
		return new PathImpl(this, bus, path);
	}
	
	public Path getLocal(String path)
	{
		return get(getFirstName(), path);
	}

	public <T extends DBusInterface> T get(String bus, String path,
			Class<T> dbusInterface) 
		throws DBusException
	{
		if(false == names.contains(bus))
		{
			throw new DBusException("Unknown bus " + bus + ", not a local name");
		}
		
		return (T) exported.get(path);
	}

	public DBus getDBus()
	{
		return dbus;
	}
	
	public String getFirstName()
	{
		return name;
	}

	public Set<String> getNames()
	{
		return unmodifiableNames;
	}

	public <T extends DBusSignal> void removeSignalListener(Class<T> signal,
			SignalListener<T> listener)
		throws DBusException
	{
		signals.removeListener(null, signal, listener);
	}

	public void sendSignal(String path, DBusSignal signal)
		throws DBusException
	{
		signals.triggerListeners(path, signal);
	}

	private class LocalDBus
		implements DBus
	{
	
		public void addMatch(String rule)
			throws DBusException
		{
		}
	
		public long getConnectionUnixUser(String connectionName)
			throws DBusException
		{
			return 0;
		}
	
		public String getId()
			throws DBusException
		{
			return null;
		}
	
		public String getNameOwner(String name)
			throws DBusException
		{
			if(names.contains(name))
			{
				return name;
			}
			
			throw new NameHasNoOwnerException("No owner for " + name);
		}
	
		public String hello()
			throws DBusException
		{
			return null;
		}
	
		public String[] listActivatableNames()
			throws DBusException
		{
			return new String[0];
		}
	
		public String[] listNames()
			throws DBusException
		{
			return names.toArray(new String[names.size()]);
		}
	
		public boolean nameHasOwner(String name)
			throws DBusException
		{
			return names.contains(name);
		}
	
		public ReleaseNameResult releaseName(String name)
			throws DBusException
		{
			if(names.remove(name))
			{
				return ReleaseNameResult.RELEASED;
			}
			
			return ReleaseNameResult.NON_EXISTENT;
		}
	
		public void removeMatch(String rule)
			throws DBusException
		{
		}
	
		public RequestNameResult requestName(String name, long flags)
			throws DBusException
		{
			names.add(name);
			return RequestNameResult.PRIMARY_OWNER;
		}
	
		public StartServiceResult startServiceByName(String name, long flags)
			throws DBusException
		{
			// TODO: Throw exception?
			return StartServiceResult.ALREADY_RUNNING;
		}
	}
}
