package se.l4.sofa.dbus.reflect;

import se.l4.sofa.dbus.Connection;
import se.l4.sofa.dbus.DBusException;
import se.l4.sofa.dbus.DBusInterface;
import se.l4.sofa.dbus.DBusSignal;
import se.l4.sofa.dbus.Path;
import se.l4.sofa.dbus.SignalListener;

public class PathImpl
	implements Path
{
	private final Connection conn;
	private final String bus;
	private final String path;

	public PathImpl(Connection conn, String bus, String path)
	{
		this.conn = conn;
		this.bus = bus;
		this.path = path;
	}
	
	public String getBus()
	{
		return bus;
	}

	public String getPath()
	{
		return path;
	}
	
	private String toAbsolute(String relativePath)
	{
		if(relativePath.startsWith("/"))
		{
			return path + relativePath;
		}
		else
		{
			return path + "/" + relativePath;
		}
	}
	
	public <T extends DBusSignal> void addSignalListener(Class<T> signal,
			SignalListener<T> listener)
		throws DBusException
	{
		conn.addSignalListener(path, signal, listener);
	}
	
	public <T extends DBusSignal> void addSignalListener(String relativePath,
			Class<T> signal, SignalListener<T> listener)
		throws DBusException
	{
		conn.addSignalListener(toAbsolute(relativePath), signal, listener);
	}

	public void export(DBusInterface object)
		throws DBusException
	{
		conn.export(path, object);
	}

	public void export(String path, DBusInterface object)
		throws DBusException
	{
		conn.export(toAbsolute(path), object);
	}

	public <T extends DBusInterface> T get(Class<T> dbusInterface)
		throws DBusException
	{
		return conn.get(bus, path, dbusInterface);
	}

	public <T extends DBusInterface> T get(String relativePath,
			Class<T> dbusInterface)
		throws DBusException
	{
		return conn.get(bus, toAbsolute(relativePath), dbusInterface);
	}

	public void sendSignal(DBusSignal signal)
		throws DBusException
	{
		conn.sendSignal(path, signal);
	}
	
	public void sendSignal(String relativePath, DBusSignal signal)
		throws DBusException
	{
		conn.sendSignal(toAbsolute(relativePath), signal);
	}

	public Path resolve(String relativePath)
	{
		return new PathImpl(conn, bus, toAbsolute(relativePath));
	}
}
