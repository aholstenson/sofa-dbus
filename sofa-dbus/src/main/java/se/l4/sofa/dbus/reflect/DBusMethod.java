package se.l4.sofa.dbus.reflect;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import se.l4.sofa.dbus.DBusException;
import se.l4.sofa.dbus.DType;
import se.l4.sofa.dbus.Holder;
import se.l4.sofa.dbus.In;
import se.l4.sofa.dbus.Name;
import se.l4.sofa.dbus.Out;
import se.l4.sofa.dbus.io.DBusOutputStream;
import se.l4.sofa.dbus.spi.Channel;
import se.l4.sofa.dbus.spi.DBusHelper;
import se.l4.sofa.dbus.spi.Endian;
import se.l4.sofa.dbus.spi.Marshalling;
import se.l4.sofa.dbus.spi.Message;
import se.l4.sofa.dbus.spi.ObjectPath;
import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.Signature.SubSignature;

/**
 * Object for easy remote method calls, wraps a {@link Method} and handles
 * message sending and reply handling.
 *  
 * @author Andreas Holstenson
 *
 */
public class DBusMethod
{
	private final String interfaceName;
	private final String name;
	
	private final boolean needReply;
	private final Method method;
	
	private Signature returnSignature;
	private int[] returnIndexes;
	
	private Signature requestSignature;
	private int[] requestIndexes;
	
	/**
	 * Create a new DBus-method based on the given Java-method.
	 * 
	 * @param m
	 */
	public DBusMethod(Method m)
	{
		this.method = m;
		
		Class<?> d = m.getDeclaringClass();
		interfaceName = DBusHelper.getNameForInterface(d);
		
		name = m.isAnnotationPresent(Name.class)
			? m.getAnnotation(Name.class).value()
			: m.getName();
		
		getRequestSignature(m);
		getReturnSignature(m);
		
		needReply = m.getReturnType() != void.class 
			|| returnIndexes.length > 0;
		
		boolean hasException = false;
		for(Class<?> c : m.getExceptionTypes())
		{
			if(c == DBusException.class)
			{
				hasException = true;
				break;
			}
		}
		
		if(false == hasException)
		{
			throw new IllegalArgumentException(m + " must throw DBusException");
		}
	}
	
	/**
	 * Get the member name of this method.
	 * 
	 * @return
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Get the DBus interface this method belongs to.
	 * 
	 * @return
	 */
	public String getInterfaceName()
	{
		return interfaceName;
	}
	
	/**
	 * Get the return signature of the method.
	 * 
	 * @return
	 */
	public Signature getReturnSignature()
	{
		return returnSignature;
	}
	
	/**
	 * Get the request signature of the method.
	 * 
	 * @return
	 */
	public Signature getRequestSignature()
	{
		return requestSignature;
	}
	
	/**
	 * Get the actual Java-method.
	 * 
	 * @return
	 */
	public Method getJavaMethod()
	{
		return method;
	}
	
	/**
	 * Create a message to invoke this method and send it over the channel.
	 * 
	 * @param c
	 * 		channel to send message on
	 * @param bus
	 * 		bus name to send it to
	 * @param path
	 * 		path on bus to send to
	 * @param args
	 * 		arguments of the method
	 * @return
	 * 		result retrieved if any, will be converted into a suitable type
	 * @throws Exception
	 * 		if unable to invoke the method for any reason
	 */
	public Object invoke(Channel c, String bus, String path, Object[] args)
		throws Exception
	{
		SubSignature[] subs = requestSignature.getSignatures();
		// Build argument array for actual message
		Object[] o = new Object[requestIndexes.length];
		for(int i=0, n=o.length; i<n; i++)
		{
			o[i] = DBusConverter.convertToDType(
				args[requestIndexes[i]],
				subs[i]
			);
		}
		
		// Serialize the arguments according to signature
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DBusOutputStream dbusOut = new DBusOutputStream(out);
		Marshalling.serialize(requestSignature, o, dbusOut);
		byte[] data = out.toByteArray();
		
		// Create and send the request message
		long serial = c.nextSerial();
		
		int flags = 0;
		
		Message m = new Message(Endian.BIG, Message.TYPE_METHOD_CALL, flags, serial, data);
		m.addField(Message.FIELD_DESTINATION, bus);
		m.addField(Message.FIELD_PATH, new ObjectPath(path));
		m.addField(Message.FIELD_MEMBER, name);
		
		// Add a signature if we have one
		if(requestSignature != null)
		{
			m.addField(Message.FIELD_SIGNATURE, requestSignature);
		}
		
		Message reply = c.sendBlocking(m);
		return interpretReply(reply, args);
	}
	
	/**
	 * Interpret a reply to a method invocation, performing conversions on
	 * returned data.
	 * 
	 * @param msg
	 * @param args
	 * @return
	 * @throws Exception
	 */
	private Object interpretReply(Message msg, Object[] args)
		throws Exception
	{
		int type = msg.getType();
		
		if(type == Message.TYPE_METHOD_RETURN)
		{
			// Deserialize body of result
			List<Object> data = msg.getBodyAsObjects();
			
			// Check that we actually match our own parameters
			boolean voidReturn = method.getReturnType() == void.class;
			int minSize = (voidReturn ? 0 : 1) + returnIndexes.length;
			
			if(data.size() != minSize)
			{
				throw new DBusException(
					"Returned data does not match expected data, expected " 
					+ minSize + " objects, got " + data.size()
				);
			}
			
			// Go through parameters and update @Out params
			Type[] parameters = method.getGenericParameterTypes();
			Object returnData = null;
			int i = 0;
			for(Object d : data)
			{
				if(i == 0 && false == voidReturn)
				{
					returnData = DBusConverter.convertFromDType(d, method.getGenericReturnType());
				}
				else
				{
					int idx = returnIndexes[i] + (voidReturn ? 0 : 1); 
					Holder<Object> holder = (Holder<Object>) args[idx];
					if(holder == null)
					{
						throw new IllegalArgumentException("Holder for @Out parameters can not be null");
					}
					
					holder.setValue(
						DBusConverter.convertFromDType(d, parameters[idx])
					);
				}
				
				i++;
			}
			
			return returnData;
		}
		else if(type == Message.TYPE_ERROR)
		{
			String errorName = (String) msg.getField(Message.FIELD_ERROR_NAME);
			List<Object> data = msg.getBodyAsObjects();
			Object[] cArgs = data.toArray();
			
			// Try to find a matching exception
			for(Class<?> c : method.getExceptionTypes())
			{
				String name = DBusHelper.getNameForInterface(c);
				
				if(name.equals(errorName))
				{
					// Found the correct exception
					Exception e = (Exception)
						DBusConverter.create(c, cArgs);
					
					if(e != null)
					{
						throw e;
					}
					
					break;
				}
			}
			
			throw new DBusException("Call failed with " + errorName + "; " + data);
		}
		
		return null;
	}
	
	/**
	 * Invoke this method on a given object return the result of the invocation.
	 * This is used when a client on the bus invokes a request on us.
	 * 
	 * @param instance
	 * 		instance to invoke on
	 * @param data
	 * 		arguments to method
	 * @return
	 * 		results of method
	 * @throws DBusException
	 * 		error to send back to the caller if unable to invoke the method
	 */
	public List<Object> invoke(Object instance, List<Object> data)
		throws DBusException
	{
		Object[] args = new Object[data.size()];
		Type[] types = method.getGenericParameterTypes();
		int i = 0;
		int k = 0;
		
		int lastIdx = 0;
		for(Object o : data)
		{
			int idx = requestIndexes[k];
			for(int j=lastIdx; j<idx; j++)
			{
				args[j] = new Holder<Object>();
			}
			
			args[idx] = DBusConverter.convertFromDType(o, types[i]);
			
			k++; // k keeps track of request index
			if(k > requestIndexes.length)
			{
				throw new DBusException("Number of given arguments does not match number of in parameters in " + method);
			}
		}
		
		try
		{
			// Then invoke the Java method
			List<Object> result = new LinkedList<Object>(); 
			Object methodData = method.invoke(instance, args);
			
			SubSignature[] subs = returnSignature.getSignatures();
			if(needReply)
			{
				result.add(
					DBusConverter.convertToDType(methodData, subs[0])
				);
			}
			
			k = 0;
			for(int n=returnIndexes.length; k<n; k++)
			{
				result.add(
					DBusConverter.convertToDType(args[returnIndexes[k]], subs[k+1])
				);
			}
			
			return result;
		}
		catch(Throwable t)
		{
			if(t instanceof InvocationTargetException)
			{
				// Unwrap invocation target exception
				t = ((InvocationTargetException) t).getCause();
			}
			
			if(t instanceof DBusException)
			{
				throw (DBusException) t;
			}
			else
			{
				throw new DBusException("Call failed; " + t.getMessage(), t);
			}
		}
	}
	
	/**
	 * Get the signature that should be used when invoking the given
	 * method.
	 * 
	 * @param m
	 * @return
	 */
	private void getRequestSignature(Method m)
	{
		Type[] types = m.getGenericParameterTypes();
		Annotation[][] annotation = m.getParameterAnnotations();
		int[] indexes = new int[types.length];

		List<SubSignature> subs = new ArrayList<SubSignature>(types.length);
		int k = 0;
		
		_outer:
		for(int i=0, n=types.length; i<n; i++)
		{
			In in = null;
			for(Annotation a : annotation[i])
			{
				if(a.annotationType() == In.class)
				{
					in = (In) a;
					break;
				}
				else if(a.annotationType() == Out.class)
				{
					continue _outer;
				}
			}
			
			// Increase number of IN parameters
			indexes[k++] = i;
			
			if(in == null)
			{
				subs.add(DBusConverter.getSignature(types[i]));
			}
			else
			{
				subs.add(DBusConverter.getSignature(in.value()));				
			}
		}
		
		int[] temp = new int[k];
		System.arraycopy(indexes, 0, temp, 0, temp.length);
		
		this.requestIndexes = temp; 
		this.requestSignature = Signature.from(subs);
	}
	
	/**
	 * Get the return signature of the method.
	 * 
	 * @param m
	 */
	private void getReturnSignature(Method m)
	{
		Type[] types = m.getGenericParameterTypes();
		Annotation[][] annotation = m.getParameterAnnotations();
		int[] indexes = new int[types.length];
		
		List<SubSignature> subs = new ArrayList<SubSignature>(types.length);
		Type returnType = m.getGenericReturnType();
		if(returnType != void.class)
		{
			Out out = m.getAnnotation(Out.class);
			if(out != null && out.value() != DType.INVALID)
			{
				subs.add(DBusConverter.getSignature(out.value()));
			}
			else
			{
				subs.add(DBusConverter.getSignature(returnType));				
			}
		}
		
		int k = 0;
		for(int i=0, n=types.length; i<n; i++)
		{
			Out out = null;
			for(Annotation a : annotation[i])
			{
				if(a.annotationType() == Out.class)
				{
					out = (Out) a;
					break;
				}
			}
			
			if(out != null)
			{
				if(types[i] instanceof ParameterizedType)
				{
					ParameterizedType t = (ParameterizedType) types[i];
					if(t.getRawType() == Holder.class)
					{
						DType type = out.value();
						if(type == DType.INVALID)
						{
							subs.add(DBusConverter.getSignature(t.getActualTypeArguments()[0]));
						}
						else
						{
							subs.add(DBusConverter.getSignature(type));
						}
						
						indexes[k] = i;
						k++;
						
						// Make sure we continue so we don't get an exception
						continue;
					}
				}
				
				throw new IllegalArgumentException("Parameter " + (i+1) + " in " + m + " has @Out-annotation, type is required to be Holder<T>");
			}
		}
		
		int[] temp = new int[k];
		System.arraycopy(indexes, 0, temp, 0, temp.length);
		
		this.returnIndexes = temp; 
		this.returnSignature = Signature.from(subs);
	}
}