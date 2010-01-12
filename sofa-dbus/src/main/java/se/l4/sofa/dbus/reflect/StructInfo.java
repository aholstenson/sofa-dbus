package se.l4.sofa.dbus.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import se.l4.sofa.dbus.DBusException;
import se.l4.sofa.dbus.DType;
import se.l4.sofa.dbus.StructPosition;
import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.Struct;
import se.l4.sofa.dbus.spi.Signature.SubSignature;

/**
 * Information about a struct (and for signals and exceptions) that is used
 * to read and write these objects.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public class StructInfo<T>
{
	private final Class<T> target;
	private final Field[] fields;
	private final Constructor<T> constructor;
	private final Signature signature;
	
	public StructInfo(Class<T> target)
	{
		this.target = target;
		
		this.fields = getFieldsForClass(target);
		this.constructor = (Constructor<T>) findConstructorForClass(target, fields);
		
		if(constructor == null)
		{
			// Verify that no field is final
			for(Field f : fields)
			{
				if(Modifier.isFinal(f.getModifiers()))
				{
					throw new IllegalArgumentException("Field " + f.getName() 
						+ " in " + target + " can not be final as there is no suitable constructor");
				}
			}
		}
		
		this.signature = createSignature();
	}
	
	public Signature getSignature()
	{
		return signature;
	}
	
	public Struct toStruct(T o)
	{
		SubSignature[] subs = signature.getSignatures();
		Object[] data = new Object[fields.length];
		for(int i=0, n=fields.length; i<n; i++)
		{
			Field f = fields[i];
			f.setAccessible(true);
			
			try
			{
				Object value = f.get(o);
				data[i] = DBusConverter.convertToDType(value, subs[i]);
			}
			catch(Exception e)
			{
				throw new IllegalArgumentException("Unable to convert DType.STRUCT; " + e.getMessage(), e);
			}
		}
		
		return new Struct(data);
	}
	
	public T create(Object[] data)
		throws DBusException
	{
		if(data.length != fields.length)
		{
			throw new DBusException(target + " expects " + fields.length 
				+ " arguments, but data contains " + data.length);
		}
		
		try
		{
			if(constructor == null)
			{
				T instance = target.newInstance();
				
				for(int i=0, n=fields.length; i<n; i++)
				{
					Field f = fields[i];
					Object conv = DBusConverter.convertFromDType(data[i], f.getGenericType());
					
					f.setAccessible(true);
					f.set(instance, conv);
				}
				
				return instance;
			}
			else
			{
				Object[] args = new Object[fields.length];
				Type[] params = constructor.getGenericParameterTypes();
				Annotation[][] annotations = constructor.getParameterAnnotations();
				
				for(int i=0, n=args.length; i<n; i++)
				{
					StructPosition sp = getStructPosition(annotations[i]);
					Object o = sp == null ? data[i] : data[sp.value()];
					
					args[i] = DBusConverter.convertFromDType(o, params[i]);
				}
				
				return constructor.newInstance(args);
			}
		}
		catch(DBusException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new DBusException("Unable to create " + target + "; " + e.getMessage(), e);
		}
	}
	
	private static Field[] getFieldsForClass(Class<?> target)
	{
		// Start with figuring out which fields are part of the Struct
		List<Field> structFields = new ArrayList<Field>(10);
		
		Class<?> parent = target;
		while(parent != null)
		{
			Field[] fields = parent.getDeclaredFields();
			for(Field f : fields)
			{
				if(f.isAnnotationPresent(StructPosition.class))
				{
					structFields.add(f);
				}
			}
			
			parent = parent.getSuperclass();
		}
		
		Collections.sort(structFields, new Comparator<Field>()
		{
			public int compare(Field o1, Field o2)
			{
				return o1.getAnnotation(StructPosition.class).value()
					- o2.getAnnotation(StructPosition.class).value();
			}
		});
		
		// Verify that we have them all
		int prev = -1;
		for(Field f : structFields)
		{
			int pos = f.getAnnotation(StructPosition.class).value();
			if(pos != prev + 1)
			{
				throw new IllegalArgumentException("Struct-class " + target + " has invalid positions, jumps from " + prev + " to " + pos);
			}
			prev = pos;
		}
		
		return structFields.toArray(new Field[structFields.size()]);
	}
	
	private Signature createSignature()
	{
		SubSignature[] subs = new SubSignature[fields.length];
		for(int i=0, n=fields.length; i<n; i++)
		{
			Field f = fields[i];
			
			StructPosition sp = f.getAnnotation(StructPosition.class);
			if(sp.type() == DType.INVALID)
			{
				subs[i] = DBusConverter.getSignature(f.getGenericType());
			}
			else
			{
				subs[i] = DBusConverter.getSignature(sp.type());
			}
		}
		
		return Signature.from(subs);
	}
	
	private Constructor<?> findConstructorForClass(Class<?> target, Field[] fields)
	{
		for(Constructor<?> c : target.getConstructors())
		{
			Class<?>[] params = c.getParameterTypes();
			Annotation[][] annotations = c.getParameterAnnotations();
			
			if(params.length != fields.length)
			{
				continue;
			}
			
			boolean hasAnnotations = false;
			boolean allAnnotated = true;
			
			for(int i=0, n=params.length; i<n; i++)
			{
				if(getStructPosition(annotations[i]) != null)
				{
					if(i > 0)
					{
						allAnnotated = false;
					}
						
					hasAnnotations = true;
					break;
				}
				else
				{
					allAnnotated = false;
				}
			}
			
			if(hasAnnotations && allAnnotated)
			{
				return c;
			}
			else if(! hasAnnotations)
			{
				return c;
			}
		}
		
		return null;
	}
	
	private StructPosition getStructPosition(Annotation[] annotations)
	{
		for(Annotation a : annotations)
		{
			if(a.annotationType() == StructPosition.class)
			{
				return (StructPosition) a;
			}
		}
		
		return null;
	}
	
}
