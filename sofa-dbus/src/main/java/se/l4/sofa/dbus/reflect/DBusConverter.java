package se.l4.sofa.dbus.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import se.l4.sofa.dbus.DBusException;
import se.l4.sofa.dbus.DType;
import se.l4.sofa.dbus.In;
import se.l4.sofa.dbus.Name;
import se.l4.sofa.dbus.Out;
import se.l4.sofa.dbus.StructPosition;
import se.l4.sofa.dbus.spi.DictEntry;
import se.l4.sofa.dbus.spi.Marshalling;
import se.l4.sofa.dbus.spi.ObjectPath;
import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.Struct;
import se.l4.sofa.dbus.spi.UInt16;
import se.l4.sofa.dbus.spi.UInt32;
import se.l4.sofa.dbus.spi.UInt64;
import se.l4.sofa.dbus.spi.Variant;
import se.l4.sofa.dbus.spi.Signature.SubSignature;

/**
 * Converter utilities for converting to and from the raw DBus types that
 * {@link Marshalling} can handle.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusConverter
{
	private DBusConverter()
	{
	}
	
	private static Map<Class<?>, StructInfo<?>> structCache
		= new ConcurrentHashMap<Class<?>, StructInfo<?>>();
	
	@SuppressWarnings("unchecked")
	private static <T> StructInfo<T> getStructInfo(Class<T> type)
	{
		StructInfo<?> info = structCache.get(type);
		if(info == null)
		{
			info = new StructInfo<T>(type);
			structCache.put(type, info);
		}
		
		return (StructInfo<T>) info;
	}
	
	/**
	 * Attempt to create an instance of the given class using the given data.
	 * This will attempt to invoke any one of the constructors on the class.
	 * 
	 * @param <T>
	 * @param c
	 * @param data
	 * @return
	 */
	public static <T> T create(Class<T> c, Object... data)
		throws DBusException
	{
		StructInfo<T> info = getStructInfo(c);
		
		return info.create(data);
	}
	
	/**
	 * Build a DBus signature for a class mapping to structs and errors,
	 * will look at fields annotated with {@link StructPosition}.
	 * 
	 * @param target
	 * 		class to build signature for
	 * @return
	 */
	public static Signature getSignatureForClass(Class<?> target)
	{
		StructInfo<?> si = getStructInfo(target);
		return si.getSignature();
	}
	
	/**
	 * Extract the data stored in an instance of a class that uses 
	 * {@link StructPosition} annotations.
	 * 
	 * @param o
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Object[] getDataInClass(Object o)
	{
		StructInfo<Object> si = (StructInfo) getStructInfo(o.getClass());
		Struct s = si.toStruct(o);
		return s.getData();
	}
	
	/**
	 * Get a DBus signature based on an array of generic types and their
	 * annotations.
	 * 
	 * @param types
	 * 		array of types
	 * @param annotation
	 * 		annotation for the types
	 * @return
	 */
	public static Signature getSignature(Type[] types, Annotation[][] annotation)
	{
		List<SubSignature> subs = new ArrayList<SubSignature>(types.length);
		
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
			
			if(in == null)
			{
				subs.add(getSignature(types[i]));
			}
			else
			{
				subs.add(getSignature(in.value()));				
			}
		}
		
		return Signature.from(subs);
	}
	
	/**
	 * Request a conversion of the given object into the given target.
	 * 
	 * @param o
	 * 		object to convert
	 * @param target
	 * 		target type to convert into
	 * @return
	 * 		converted type
	 * @throws DBusException
	 * 		if unable to convert
	 */
	public static Object convertFromDType(Object o, Type target)
		throws DBusException
	{
		if(target == byte.class || target == Byte.class)
		{
			if(o instanceof Number)
			{
				return ((Number) o).byteValue();
			}
		}
		else if(target == boolean.class || target == Boolean.class)
		{
			if(o instanceof Boolean)
			{
				return o;
			}
		}
		else if(target == short.class || target == Short.class)
		{
			if(o instanceof Number)
			{
				return ((Number) o).shortValue();
			}
		}
		else if(target == int.class || target == Integer.class)
		{
			if(o instanceof Number)
			{
				return ((Number) o).intValue();
			}
			else if(o instanceof UInt16)
			{
				return ((UInt16) o).getValue();
			}
		}
		else if(target == long.class || target == Long.class)
		{
			if(o instanceof Number)
			{
				return ((Number) o).longValue();
			}
			else if(o instanceof UInt32)
			{
				return ((UInt32) o).getValue();
			}
		}
		else if(target == double.class || target == Double.class)
		{
			if(o instanceof Number)
			{
				return ((Number) o).doubleValue();
			}
		}
		else if(target == float.class || target == Float.class)
		{
			if(o instanceof Number)
			{
				return ((Number) o).floatValue();
			}
		}
		else if(target == String.class)
		{
			if(o instanceof String)
			{
				return o;
			}
			else if(o instanceof ObjectPath)
			{
				return ((ObjectPath) o).getPath();
			}
			else if(o instanceof Signature)
			{
				return ((Signature) o).getValue();
			}
		}
		else if(target == Variant.class)
		{
			if(o instanceof Variant)
			{
				return o;
			}
		}
		else if(o instanceof Variant)
		{
			Object result = ((Variant) o).getValue();
			return convertFromDType(result, target);
		}
		else if(target instanceof ParameterizedType
				&&  ((ParameterizedType) target).getRawType() == Map.class)
		{
			ParameterizedType pt = (ParameterizedType) target;
			Type[] typeArgs = pt.getActualTypeArguments();
			
			// Hopefully we have dictionary entries
			if(o instanceof List<?>)
			{
				Map<Object, Object> map = new HashMap<Object, Object>();
				for(Object e : (List<?>) o)
				{
					if(false == e instanceof DictEntry)
					{
						throw new DBusException("Can not convert array to map, does not contain DictEntry");
					}
					
					DictEntry de = (DictEntry) e;
					map.put(
						convertFromDType(de.getKey(), typeArgs[0]), 
						convertFromDType(de.getValue(), typeArgs[1])
					);
				}
				
				return map;
			}
		}
		else if(o instanceof List<?>)
		{
			// Handle arrays
			if(target instanceof Class<?>)
			{
				Class<?> c = (Class<?>) target;
				if(c.isArray())
				{
					List<?> list = (List<?>) o;
					Class<?> l = c.getComponentType();
					
					Object result = Array.newInstance(l, list.size());
					int i=0;
					for(Object e : list)
					{
						Object conv = convertFromDType(e, l);
						
						Array.set(result, i, conv);
						i++;
					}
					
					return result;
				}
			}
			
			// Treat the rest as potential collections
			if(false == target instanceof ParameterizedType)
			{
				throw new DBusException("List and/or maps are required to have a generic type, eg List<String> instead of List");
			}
			
			ParameterizedType type = (ParameterizedType) target;
			Type rawType = type.getRawType();
			Type actualType = type.getActualTypeArguments()[0];
			Collection<Object> result = null;
			
			// Select the type of collection
			if(rawType == List.class || rawType == Collection.class 
				|| rawType == LinkedList.class)
			{
				result = new LinkedList<Object>();
			}
			else if(rawType == Set.class)
			{
				if(actualType instanceof Class<?> 
					&& ((Class<?>) actualType).isEnum())
				{
					result = (Collection<Object>) EnumSet.noneOf((Class) actualType);
				}
				else
				{
					result = new HashSet<Object>();
				}
			}
			else if(rawType == ArrayList.class)
			{
				result = new ArrayList<Object>();
			}
			else if(rawType == HashSet.class)
			{
				result = new HashSet<Object>();
			}
			else if(rawType == TreeSet.class)
			{
				result = new TreeSet<Object>();
			}
			
			if(result != null)
			{
				for(Object e : (List<?>) o)
				{
					result.add(
						convertFromDType(e, actualType)
					);
				}
				
				return result;
			}
		}
		else if(o instanceof byte[])
		{
			if(target == byte[].class)
			{
				return o;
			}
		}
		else if(o instanceof Struct)
		{
			if(target instanceof Class<?>)
			{
				return createStruct((Struct) o, (Class<?>) target);
			}
		}
		else if(target instanceof Class<?> && ((Class<?>) target).isEnum())
		{
			if(o instanceof Number)
			{
				Class<?> c = (Class<?>) target;
				Object[] constants = c.getEnumConstants();
				
				int i = ((Number) o).intValue() - 1;
				if(i >= constants.length)
				{
					throw new DBusException("Value " + i + " larger than size of " + c);
				}
				
				return constants[i];
			}
		}
		
		throw new DBusException("Don't know how to convert to " + target + " from " + o);
	}
	
	@SuppressWarnings("unchecked")
	public static Object convertToDType(Object o, SubSignature sig)
	{
		if(o == null)
		{
			return o;
		}
		
		DType type = sig.getType();
		SubSignature[] subs = sig.getSignatures();
		switch(type)
		{
			case BOOLEAN:
				if(o instanceof Boolean)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType.BOOLEAN may only be of type java.lang.Boolean");
				}
				
			case BYTE:
			case UINT16:
			case INT32:
			case INT64:
			case DOUBLE:
				if(o instanceof Number)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType." + type + " can only handle classes that extend java.lang.Number");
				}
				
			case INT16:
				if(o instanceof Number || o instanceof UInt16)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType." + type + " can only handle values of type java.lang.Number and UInt16");
				}
				
			case UINT32:
				if(o instanceof Number || o instanceof UInt32)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType." + type + " can only handle values of type java.lang.Number and UInt32");
				}
				
			case UINT64:
				if(o instanceof Number || o instanceof BigInteger)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType.UINT64 can only handle values of type java.lang.Number and java.math.BigInteger");
				}
				
			case STRING:
				if(o instanceof String)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType.STRING can only handle strings");
				}
				
			case OBJECT_PATH:
				if(o instanceof String)
				{
					return new ObjectPath((String) o);
				}
				else if(o instanceof ObjectPath)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType.OBJECT_PATH can only handle values of type String and ObjectPath");
				}
			
			case SIGNATURE:
				if(o instanceof String || o instanceof Signature)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType.SIGNATURE can only handle values of type String and Signature");
				}
				
			case VARIANT:
				if(o instanceof Variant)
				{
					return o;
				}
				else
				{
					Signature variantSig = 
						Signature.from(getSignature(o.getClass()));
					
					return new Variant(variantSig, o);
				}
				
			case ARRAY:
				// Arrays might require some processing
				SubSignature arraySig = subs[0];
				DType arrayType = arraySig.getType();
				if(arrayType == DType.BYTE && o instanceof byte[])
				{
					// byte arrays are optimized
					return o;
				}
				else if(o instanceof Collection<?>)
				{
					Collection<?> list = (Collection<?>) o;
					Object[] result = new Object[list.size()];
					int i = 0;
					for(Object c : list)
					{
						result[i] = convertToDType(c, arraySig);
						i++;
					}
					
					return result;
				}
				else if(o.getClass().isArray())
				{
					int size = Array.getLength(o);
					Object[] result = new Object[size];
					for(int i=0; i<size; i++)
					{
						result[i] = convertToDType(
							Array.get(o, i), 
							arraySig
						);
					}
					
					return result;
				}
				else if(arrayType == DType.DICT_ENTRY && o instanceof Map<?, ?>)
				{
					// This is a map, so create a array with all values
					Map<?, ?> map = (Map<?, ?>) o;
					Object[] result = new Object[map.size()];
					int i = 0;
					for(Map.Entry<?, ?> e : map.entrySet())
					{
						result[i] = new DictEntry(e.getKey(), e.getValue());
						i++;
					}
					
					return result;
				}
				
				throw new IllegalArgumentException("DType.ARRAY may only be arrays, instances of java.util.Collection or java.util.Map");
				
			case DICT_ENTRY:
				if(o instanceof DictEntry)
				{
					return o;
				}
				else
				{
					throw new IllegalArgumentException("DType.DICT_ENTRY can only be of type DictEntry");
				}
			
			case STRUCT:
				StructInfo<Object> si = (StructInfo) getStructInfo(o.getClass());
				return si.toStruct(o);
				
			default:
				throw new IllegalArgumentException("Unknown type " + type);
		}
	}
	
	/**
	 * Create an object of the given type based on the raw {@link Struct}.
	 * 
	 * @param s
	 * @param target
	 * @return
	 * @throws DBusException
	 */
	private static Object createStruct(Struct s, Class<?> target)
		throws DBusException
	{
		StructInfo<?> info = getStructInfo(target);
		return info.create(s.getData());
	}

	/**
	 * Get a signature based on a {@link Type}.
	 * 
	 * @param type
	 * @return
	 */
	public static SubSignature getSignature(Type type)
	{
		if(type == byte.class || type == Byte.class)
		{
			return SubSignature.get(DType.BYTE);
		}
		else if(type == boolean.class || type == Boolean.class)
		{
			return SubSignature.get(DType.BOOLEAN);
		}
		else if(type == short.class || type == Short.class)
		{
			return SubSignature.get(DType.INT16);
		}
		else if(type == UInt16.class)
		{
			return SubSignature.get(DType.UINT16);
		}
		else if(type == int.class || type == Integer.class)
		{
			return SubSignature.get(DType.INT32);
		}
		else if(type == UInt32.class)
		{
			return SubSignature.get(DType.UINT32);
		}
		else if(type == long.class || type == Long.class)
		{
			return SubSignature.get(DType.INT64);
		}
		else if(type == UInt64.class)
		{
			return SubSignature.get(DType.UINT64);
		}
		else if(type == double.class || type == Double.class 
			|| type == float.class || type == Float.class)
		{
			return SubSignature.get(DType.DOUBLE);
		}
		else if(type == String.class)
		{
			return SubSignature.get(DType.STRING);
		}
		else if(type == ObjectPath.class)
		{
			return SubSignature.get(DType.OBJECT_PATH);
		}
		else if(type == Signature.class)
		{
			return SubSignature.get(DType.SIGNATURE);
		}
		else if(type == Variant.class)
		{
			return SubSignature.get(DType.VARIANT);
		}
		else if(type instanceof Class<?> && ((Class<?>) type).isArray())
		{
			// Generic array
			
			Class<?> arraytype = ((Class<?>) type).getComponentType();
			SubSignature[] subs = new SubSignature[] {
				getSignature(arraytype)
			};
			
			return new SubSignature(DType.ARRAY, subs);
		}
		else if(type instanceof ParameterizedType
			&& Collection.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))
		{
			// Lists are treated as arrays if we can find the parameter
			Type t = ((ParameterizedType) type).getActualTypeArguments()[0];
			SubSignature[] subs = new SubSignature[] {
				getSignature(t)	
			};
			
			return new SubSignature(DType.ARRAY, subs);
		}
		else if(type instanceof ParameterizedType
			&& Map.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))
		{
			// Maps are treated as dictionaries
			// Get arguments for DICT_ENTRY
			Type[] args = ((ParameterizedType) type).getActualTypeArguments();
			SubSignature[] subs = new SubSignature[] {
				getSignature(args[0]),
				getSignature(args[1])
			};
			
			subs = new SubSignature[] { new SubSignature(DType.DICT_ENTRY, subs) }; 
			
			return new SubSignature(DType.ARRAY, subs);
		}
		else if(type instanceof Class<?>)
		{
			StructInfo<?> si = getStructInfo((Class<?>) type);
			Signature sig = si.getSignature();
			return new SubSignature(DType.STRUCT, sig.getSignatures());
		}

		throw new IllegalArgumentException("Can not serialize " + type + " (note: arrays and lists need to be typed)");
	}
	
	/**
	 * Get a signature based on a {@link DType}.
	 * 
	 * @param type
	 * @return
	 */
	public static SubSignature getSignature(DType type)
	{
		switch(type)
		{
			case INVALID:
			case ARRAY:
			case DICT_ENTRY:
			case STRUCT:
				throw new IllegalArgumentException("Can not serialize " + type + " requires parameters");
		}
		
		return new SubSignature(type);
	}
	
	public static String getMemberName(Class<?> c)
	{
		return c.isAnnotationPresent(Name.class)
			? c.getAnnotation(Name.class).value()
			: c.getSimpleName();
	}
}
