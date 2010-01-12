package se.l4.sofa.dbus.reflect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.sofa.dbus.Connection;
import se.l4.sofa.dbus.DBusException;
import se.l4.sofa.dbus.DBusLifecycle;
import se.l4.sofa.dbus.Holder;
import se.l4.sofa.dbus.Path;
import se.l4.sofa.dbus.UnknownMethodException;
import se.l4.sofa.dbus.io.DBusOutputStream;
import se.l4.sofa.dbus.spi.Channel;
import se.l4.sofa.dbus.spi.DBusHelper;
import se.l4.sofa.dbus.spi.Endian;
import se.l4.sofa.dbus.spi.Marshalling;
import se.l4.sofa.dbus.spi.Message;
import se.l4.sofa.dbus.spi.MessageHandler;
import se.l4.sofa.dbus.spi.ObjectPath;
import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.UInt32;

/**
 * Handler that implements the support for dynamically creating implementations
 * of DBus services as Java-objects.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusProxyHandler
	implements MessageHandler
{
	private static final Logger logger = LoggerFactory.getLogger(DBusProxyHandler.class);
	
	private final Connection connection;
	private final Endian endian;
	private final Channel channel;
	private final MethodCache cache;
	
	private final Map<String, ExportedInterface> paths;
	private final Introspection introspection;
	
	public DBusProxyHandler(Connection connection, Endian endian, Channel channel)
	{
		this.connection = connection;
		this.endian = endian;
		this.channel = channel;
		
		cache = new MethodCache();
		paths = new HashMap<String, ExportedInterface>();
		introspection = new Introspection();
	}
	
	public void export(String path, Object o)
		throws DBusException
	{
		ExportedInterface exported = new ExportedInterface(cache, o);
		paths.put(path, exported);
		introspection.addService(path, exported);
		
		if(o instanceof DBusLifecycle)
		{
			Path p = connection.getLocal(path);
			((DBusLifecycle) o).interfaceExported(p);
		}
	}
	
	public void unexport(String path)
		throws DBusException
	{
		Object o = paths.get(path);
		if(o == null)
		{
			return;
		}
		
		paths.remove(path);
		introspection.removeService(path);
		
		if(o instanceof DBusLifecycle)
		{
			Path p = connection.getLocal(path);
			((DBusLifecycle) o).interfaceUnexported(p);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T createProxy(String bus, String path, Class<T> proxyClass)
	{
		return (T) Proxy.newProxyInstance(
			proxyClass.getClassLoader(), 
			new Class[] { proxyClass }, 
			new Handler(bus, path)
		);
	}
	
	public boolean handle(Message message, Channel channel)
	{
		if(message.getType() == Message.TYPE_METHOD_CALL)
		{
			try
			{
				Signature sig = (Signature) message.getField(Message.FIELD_SIGNATURE);
				ObjectPath path = (ObjectPath) message.getField(Message.FIELD_PATH);
				String member = (String) message.getField(Message.FIELD_MEMBER);
				String sender = (String) message.getField(Message.FIELD_SENDER);
				String destination = (String) message.getField(Message.FIELD_DESTINATION);
				
				// Make sure we only handle our own method calls
				Set<String> names = connection.getNames();
				if(false == names.contains(destination))
				{
					return false;
				}
				
				long serial = message.getSerial();
				
				ExportedInterface exported = paths.get(path.getPath());
				try
				{
					boolean introspect = "Introspect".equals(member) 
						&& (sig == null || sig.getSignatures().length == 0);
					
					if(exported != null && exported.getMethod("introspect", "") != null)
					{
						introspect = false;
					}
					
					if(exported != null || introspect)
					{
						/*
						 * If we have an object or we are introspecting we
						 * attempt to handle the method call 
						 */
						
						List<Object> data = message.getBodyAsObjects();
						Holder<Signature> returnSig = new Holder<Signature>();
						
						List<Object> result;
						if(introspect)
						{
							String xml = introspection.introspect(path.getPath());
							result = (List) Collections.singletonList(xml);
							returnSig.setValue(Signature.parse("s"));
						}
						else
						{
							result = exported.invoke(
									member, 
									sig == null ? Signature.EMTPY_SIGNATURE : sig, 
									data, 
									returnSig
							);
						}
						
						// Let's marshall the result
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						DBusOutputStream dbusOut = new DBusOutputStream(out);
						dbusOut.setEndian(endian);
						Marshalling.serialize(returnSig.getValue(), result.toArray(), dbusOut);
						
						Message msg = new Message(
							endian, 
							Message.TYPE_METHOD_RETURN, 
							Message.FLAG_NO_REPLY_EXPECTED, 
							channel.nextSerial(), 
							out.toByteArray()
						);
						
						msg.addField(Message.FIELD_REPLY_SERIAL, new UInt32(serial));
						msg.addField(Message.FIELD_SIGNATURE, returnSig.getValue());
						msg.addField(Message.FIELD_DESTINATION, sender);
		//					msg.addField(Message.FIELD_SENDER, sender);
						
						channel.sendMessage(msg);
					}
					else
					{
						throw new UnknownMethodException("Unknown path " + path.getPath());
					}
				}
				catch(DBusException e)
				{
					logger.warn("Method call failed: " + message + "; " + e.getMessage(), e);
					
					Class<?> c = e.getClass();
					Signature classSig = DBusConverter.getSignatureForClass(c);
					Object[] data = DBusConverter.getDataInClass(e);
						
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					DBusOutputStream dbusOut = new DBusOutputStream(out);
					dbusOut.setEndian(endian);
					Marshalling.serialize(classSig, data, dbusOut);
						
					Message msg = new Message(
						endian, 
						Message.TYPE_ERROR, 
						Message.FLAG_NO_REPLY_EXPECTED, 
						channel.nextSerial(), 
						out.toByteArray()
					);
						
					msg.addField(Message.FIELD_REPLY_SERIAL, new UInt32(serial));
					msg.addField(Message.FIELD_SIGNATURE, classSig);
					msg.addField(Message.FIELD_DESTINATION, sender);
					msg.addField(Message.FIELD_ERROR_NAME, DBusHelper.getNameForInterface(c));
					
					channel.sendMessage(msg);
				}
				
				return true;
			}
			catch(IOException e )
			{
				logger.error("Unable to handle message " + message + ";" + e.getMessage(), e);
			}
			
		}
		
		return false;
	}
	
	/**
	 * Proxy invocation handler, ensures that calls are sent to the bus
	 * for invocation.
	 * 
	 * @author Andreas Holstenson
	 *
	 */
	private class Handler
		implements InvocationHandler
	{
		private final String bus;
		private final String path;
		
		public Handler(String bus, String path)
		{
			this.bus = bus;
			this.path = path;
		}
		
		public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable
		{
			if(method.getDeclaringClass() == Object.class)
			{
				// Handle equals and hashCode
				String name = method.getName();
				if("equals".equals(name))
				{
					// Equals is implemented as a simple identity equals
					Object o = args[0];
					return proxy == o;
				}
				else if("hashCode".equals(name))
				{
					return System.identityHashCode(proxy);
				}
				
			}
			
			DBusMethod dbusMethod = cache.get(method);
			
			try
			{
				return dbusMethod.invoke(channel, endian, bus, path, args);
			}
			catch(Exception e)
			{
				boolean found = false;
				Class<?> ec = e.getClass();
				for(Class<?> dec : method.getExceptionTypes())
				{
					if(dec.isAssignableFrom(ec))
					{
						found = true;
						break;
					}
				}
				
				if(found)
				{
					throw e;
				}
				else
				{
					throw new DBusException("Unable to invoke method; " + e.getMessage(), e);
				}
			}
		}
	}
}
