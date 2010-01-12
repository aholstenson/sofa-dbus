package se.l4.sofa.dbus.reflect;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import se.l4.sofa.dbus.DBusException;
import se.l4.sofa.dbus.DBusInterface;
import se.l4.sofa.dbus.Holder;
import se.l4.sofa.dbus.UnknownMethodException;
import se.l4.sofa.dbus.spi.DBusHelper;
import se.l4.sofa.dbus.spi.Signature;

/**
 * Information about a {@link DBusInterface} that has been exported.
 * 
 * @author Andreas Holstenson
 *
 */
public class ExportedInterface
{
	private final Object instance;
	private final Map<String, DBusMethod> methods;
	private final List<String> interfaces;
	
	public ExportedInterface(MethodCache cache, Object instance)
	{
		this.instance = instance;
		
		Class<?> c = instance.getClass();
		
		methods = new HashMap<String, DBusMethod>();
		interfaces = new LinkedList<String>();
		
		handle(cache, c);
	}
	
	private void handle(MethodCache cache, Class<?> c)
	{
		if(c.isInterface() && DBusInterface.class.isAssignableFrom(c)
			&& DBusInterface.class != c)
		{
			String interfaceName = DBusHelper.getNameForInterface(c);
				
			interfaces.add(interfaceName);
			
			for(Method m : c.getMethods())
			{
				DBusMethod method = cache.get(m);
				String name = method.getName();
				
				methods.put(name, method);
			}
		}
		
		for(Class<?> t : c.getInterfaces())
		{
			handle(cache, t);
		}
	}
	
	/**
	 * Locate a {@link DBusMethod} for a method with the given signature.
	 * 
	 * @param methodName
	 * @param signature
	 * @return
	 */
	public DBusMethod getMethod(String methodName, String signature)
	{
		DBusMethod method = methods.get(methodName);
		
		if(method != null && method.getRequestSignature()
			.getValue().equals(signature))
		{
			return method;
		}
		
		return null;
	}
	
	/**
	 * Get the instance that is exported.
	 * 
	 * @return
	 */
	public Object getInstance()
	{
		return instance;
	}
	
	/**
	 * Get a list of the names of all the exported interfaces.
	 * 
	 * @return
	 */
	public List<String> getInterfaces()
	{
		return interfaces;
	}
	
	/**
	 * Get all methods that are available in the interface.
	 * 
	 * @return
	 */
	public Collection<DBusMethod> getMethods()
	{
		return methods.values();
	}
	
	/**
	 * Perform an invocation of a method.
	 * 
	 * @param methodName
	 * @param signature
	 * @param data
	 * @param returnSig
	 * @return
	 * @throws DBusException
	 */
	public List<Object> invoke(
			String methodName, 
			Signature signature, 
			List<Object> data,
			Holder<Signature> returnSig)
		throws DBusException
	{
		DBusMethod method = methods.get(methodName);
		
		if(method == null)
		{
			throw new UnknownMethodException("The method " + methodName + " could not be found");
		}
		
		if(false == method.getRequestSignature().equals(signature))
		{
			throw new UnknownMethodException("The method " + methodName 
				+ " does not exist with signature " + signature 
				+ ", has signature " + method.getRequestSignature()
			);
		}
		
		returnSig.setValue(method.getReturnSignature());
		return method.invoke(instance, data);
	}
}
