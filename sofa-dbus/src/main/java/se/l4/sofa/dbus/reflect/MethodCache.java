package se.l4.sofa.dbus.reflect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import se.l4.sofa.dbus.DBusInterface;

/**
 * Cache for the methods in a {@link DBusInterface}.
 * 
 * @author Andreas Holstenson
 *
 */
public class MethodCache
{
	private Map<Method, DBusMethod> cache;
	
	public MethodCache()
	{
		cache = new ConcurrentHashMap<Method, DBusMethod>();
	}
	
	public DBusMethod get(Method m)
	{
		DBusMethod info = cache.get(m);
		if(info == null)
		{
			info = new DBusMethod(m);
			cache.put(m, info);
		}
		
		return info;
	}
}
