package se.l4.sofa.dbus;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;

import se.l4.sofa.dbus.DBus.NameAcquired;
import se.l4.sofa.dbus.DBus.NameLost;
import se.l4.sofa.dbus.reflect.DBusProxyHandler;
import se.l4.sofa.dbus.reflect.PathImpl;
import se.l4.sofa.dbus.reflect.SignalMessageHandler;
import se.l4.sofa.dbus.spi.Channel;

/**
 * Connection to a DBus server, usually the session or system bus.
 * 
 * @author Andreas Holstenson
 *
 */
public class BusConnection
	extends BasicConnection
	implements Connection
{
	private SignalMessageHandler signals;
	private DBusProxyHandler proxying;
	private DBus dbus;
	
	private final Set<String> names;
	private final Set<String> unmodifiableNames;
	private String firstName;
	
	public BusConnection(BusAddress address, String[] saslMechanisms,
			CallbackHandler authentication)
	{
		super(address, saslMechanisms, authentication);
		
		names = new HashSet<String>();
		unmodifiableNames = Collections.unmodifiableSet(names);
	}

	public BusConnection(BusAddress address)
	{
		this(address, DEFAULT_MECHANISMS, null);
	}

	public BusConnection(String address)
	{
		this(new BusAddress(address));
	}
	
	@Override
	public void connect()
		throws IOException
	{
		super.connect();
		
		Channel c = getConnection();
		if(c != null)
		{
			try
			{
				proxying = new DBusProxyHandler(this, c);
				addHandler(proxying);
				
				dbus = get("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
				
				signals = new SignalMessageHandler(dbus);
				addHandler(signals);
				
				String name = dbus.hello();
				this.firstName = name;
				names.add(name);
				
				// Start listening for changes in our own name
				addSignalListener(NameAcquired.class, new SignalListener<NameAcquired>()
				{
					public void signalReceived(NameAcquired signal)
					{
						names.add(signal.getName());
					}
				});
				
				addSignalListener(NameLost.class, new SignalListener<NameLost>()
				{
					
					public void signalReceived(NameLost signal)
					{
						names.remove(signal.getName());
					}
				});
			}
			catch(DBusException e)
			{
				throw new IOException("Could not say hello() to DBus-server; " + e.getMessage());
			}
		}
	}
	
	public String getFirstName()
	{
		return firstName;
	}
	
	/**
	 * Get the names that this connection currently owns on the bus.
	 * 
	 * @return
	 */
	public Set<String> getNames()
	{
		return unmodifiableNames;
	}
	
	public DBus getDBus()
	{
		return dbus;
	}
	
	public Path get(String bus, String path)
	{
		return new PathImpl(this, bus, path);
	}
	
	public Path getLocal(String path)
	{
		return get(getFirstName(), path);
	}
	
	public <T extends DBusInterface> T get(String bus, String path, Class<T> dbusInterface)
		throws DBusException
	{
		if(proxying == null)
		{
			throw new IllegalStateException("Can't create proxies without being connected");
		}
		
		if(path.endsWith("/") && path.length() != 1)
		{
			path = path.substring(0, path.length() - 1);
		}
		
		return proxying.createProxy(bus, path, dbusInterface);
	}
	
	public <T extends DBusSignal> void addSignalListener(Class<T> signal, SignalListener<T> listener)
		throws DBusException
	{
		if(signals == null)
		{
			throw new IllegalStateException("Can't add signal listeners without being connected");
		}
		
		signals.addListener(signal, listener);
	}
	
	public <T extends DBusSignal> void addSignalListener(String path, Class<T> signal, SignalListener<T> listener)
		throws DBusException
	{
		if(signals == null)
		{
			throw new IllegalStateException("Can't add signal listeners without being connected");
		}
		
		signals.addListener(path, signal, listener);
	}
	
	public <T extends DBusSignal> void removeSignalListener(Class<T> signal, SignalListener<T> listener) 
		throws DBusException
	{
		signals.removeListener(null, signal, listener);
	}
	
	public void export(String path, DBusInterface object)
		throws DBusException
	{
		if(proxying == null)
		{
			throw new IllegalStateException("Can't export objects without being connected");
		}
		
		proxying.export(path, object);
	}
	
	public void sendSignal(String path, DBusSignal signal) 
		throws DBusException
	{
		if(signals == null)
		{
			throw new IllegalStateException("Can't send signal without being connected");
		}
		
		signals.send(getConnection(), path, signal);
	}
	
	public static BusConnection session()
		throws IOException
	{
		String addr = System.getenv("DBUS_SESSION_BUS_ADDRESS");
		BusConnection c = new BusConnection(addr);
		c.connect();
		
		return c;
	}
}
