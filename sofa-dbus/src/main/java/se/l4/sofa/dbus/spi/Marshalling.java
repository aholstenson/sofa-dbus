package se.l4.sofa.dbus.spi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import se.l4.sofa.dbus.DType;
import se.l4.sofa.dbus.io.DBusInputStream;
import se.l4.sofa.dbus.io.DBusOutputStream;
import se.l4.sofa.dbus.spi.Signature.SubSignature;

/**
 * Class to handle marshalling and unmarshalling of objects.
 * 
 * @author Andreas Holstenson
 *
 */
public class Marshalling
{
	private static final char TYPE_BYTE = 'y';
	private static final char TYPE_BOOLEAN = 'b';
	private static final char TYPE_INT16 = 'n';
	private static final char TYPE_UINT16 = 'q';
	private static final char TYPE_INT32 = 'i';
	private static final char TYPE_UINT32 = 'u';
	private static final char TYPE_INT64 = 'x';
	private static final char TYPE_UINT64 = 't';
	private static final char TYPE_DOUBLE = 'd';
	private static final char TYPE_STRING = 's';
	private static final char TYPE_OBJECT_PATH = 'o';
	private static final char TYPE_SIGNATURE = 'g';
	private static final char TYPE_ARRAY = 'a';
	private static final char TYPE_VARIANT = 'v';
	private static final char TYPE_STRUCT = 'r';
	private static final char TYPE_STRUCT_BEGIN = '(';
	private static final char TYPE_STRUCT_END = ')';
	private static final char TYPE_DICT_ENTRY = 'e';
	private static final char TYPE_DICT_ENTRY_BEGIN = '{';
	private static final char TYPE_DICT_ENTRY_END = '}';
	
	private static int MAX_ARRAY_LEN = 67108864;
	
	public static String getSignature(Type type)
	{
		StringBuilder b = new StringBuilder();
		getSignature(type, b);
		
		return b.toString();
	}
	
	public static void getSignature(Type type, StringBuilder result)
	{
		if(type == byte.class || type == Byte.class)
		{
			result.append(TYPE_BYTE);
		}
		else if(type == boolean.class || type == Boolean.class)
		{
			result.append(TYPE_BOOLEAN);
		}
		else if(type == short.class || type == Short.class)
		{
			// Int16
			result.append(TYPE_INT16);
		}
		else if(type == UInt16.class)
		{
			result.append(TYPE_UINT16);
		}
		else if(type == int.class || type == Integer.class)
		{
			// Int32
			result.append(TYPE_INT32);
		}
		else if(type == UInt32.class)
		{
			result.append(TYPE_UINT32);
		}
		else if(type == long.class || type == Long.class)
		{
			// Int64
			result.append(TYPE_INT64);
		}
		else if(type == UInt64.class)
		{
			result.append(TYPE_UINT64);
		}
		else if(type == double.class || type == Double.class)
		{
			result.append(TYPE_DOUBLE);
		}
		else if(type == String.class)
		{
			result.append(TYPE_STRING);
		}
		else if(type == ObjectPath.class)
		{
			result.append(TYPE_OBJECT_PATH);
		}
		else if(type == Signature.class)
		{
			result.append(TYPE_SIGNATURE);
		}
		else if(type == Variant.class)
		{
			result.append(TYPE_VARIANT);
		}
		else if(type instanceof Class<?> &&
				((Class<?>) type).isArray())
		{
			// Generic array
			result.append(TYPE_ARRAY);
			
			Class<?> arraytype = ((Class<?>) type).getComponentType();
			
			getSignature(arraytype, result);
		}
		else if(type instanceof ParameterizedType
			&& List.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))
		{
			// Lists are treated as arrays if we can find the parameter
			result.append(TYPE_ARRAY);
			
			Type t = ((ParameterizedType) type).getActualTypeArguments()[0];
			getSignature(t, result);
		}
		else if(type instanceof ParameterizedType
			&& Map.class.isAssignableFrom((Class<?>) ((ParameterizedType) type).getRawType()))
		{
			// Maps are treated as dictionaries
			result.append(TYPE_ARRAY);
			result.append(TYPE_DICT_ENTRY_BEGIN);
			
			Type[] args = ((ParameterizedType) type).getActualTypeArguments();
			getSignature(args[0], result);
			getSignature(args[1], result);
			
			result.append(TYPE_DICT_ENTRY_END);
		}
		else if(type instanceof Class<?> &&
			Struct.class.isAssignableFrom((Class<?>) type))
		{
			// Struct
			result.append(TYPE_STRUCT_BEGIN);
			
			
			result.append(TYPE_STRUCT_END);
			
			throw new IllegalArgumentException("STRUCT not implemented");
		}
		else 
		{
			throw new IllegalArgumentException("Can not serialize " + type + " (note: arrays and lists need to be typed)");
		}
	}
	
	
	public static void serialize(Signature signature, Object[] objects, DBusOutputStream stream)
		throws IOException
	{
		SubSignature[] signatures = signature.getSignatures();
		if(objects.length != signatures.length)
		{
			throw new IOException("Signature mismatch, number of objects does " +
				"not match signature (objects=" + objects.length 
				+ ", signature length=" + signatures.length 
				+ ", signature=" + signature);
		}
		
		for(int i=0, n=signatures.length; i<n; i++)
		{
			Object o = objects[i];
			serializeObject(signatures[i], o, stream);
		}
	}
	
	/**
	 * Get the alignment of the given type.
	 * 
	 * @param c
	 * @return
	 */
	public static int getAlignment(DType type)
		throws IOException
	{
		switch(type)
		{
			case BYTE:
				return 1;
			case BOOLEAN:
				return 4;
			case INT16:
			case UINT16:
				return 2;
			case INT32:
			case UINT32:
				return 4;
			case INT64:
			case UINT64:
			case DOUBLE:
				return 8;
			case STRING:
			case OBJECT_PATH:
				return 4;
			case SIGNATURE:
				return 1;
			case ARRAY:
				return 4;
			case STRUCT:
			case DICT_ENTRY:
				return 8;
			case VARIANT:
				return 1;
		}
		
		throw new IOException("Unknown type " + type);
	}
	
	/**
	 * Deserialize a list of objects based on a signature from the stream.
	 * 
	 * @param signature
	 * 		signature to use
	 * @param stream
	 * 		stream to read from
	 * @return
	 * 		deserialized objects
	 * @throws IOException
	 * 		if unable to deserialize
	 */
	public static List<Object> deserialize(Signature signature, DBusInputStream stream)
		throws IOException
	{
		List<Object> result = new LinkedList<Object>();
		
		SubSignature[] subs = signature.getSignatures();
		
		for(int i=0, n=subs.length; i<n; i++)
		{
			Object o = deserializeObject(subs[i], stream);
			result.add(o);
		}
		
		return result;
	}
	
	private static Object deserializeObject(SubSignature sig, DBusInputStream stream)
		throws IOException
	{
		DType type = sig.getType();
		SubSignature[] subs = sig.getSignatures();
		
		switch(type)
		{
			case BYTE:
				return stream.readByte();
			case BOOLEAN:
				return stream.readBoolean();
			case INT16:
				return stream.readInt16();
			case UINT16:
				return stream.readUInt16();
			case INT32:
				return stream.readInt32();
			case UINT32:
				return stream.readUInt32();
			case INT64:
				return stream.readInt64();
			case UINT64:
				return stream.readUInt64();
			case DOUBLE:
				return stream.readDouble();
			case STRING:
				return stream.readString();
			case OBJECT_PATH:
				return new ObjectPath(stream.readObjectPath());
			case SIGNATURE:
				return Signature.parse(stream.readSignature());
			case ARRAY:
				{
					long length = stream.readUInt32();
					if(length > MAX_ARRAY_LEN)
					{
						throw new IOException("Unable to read array, exceeded length of " + MAX_ARRAY_LEN + ", was " + length);
					}
					
					SubSignature valueSig = subs[0];
					stream.readPad(
						getAlignment(valueSig.getType())
					);
					
					long current = stream.getBytesRead();
					long limit = current + length;
					
					List<Object> array = new LinkedList<Object>();
					
					while(stream.getBytesRead() < limit)
					{
						array.add(
							deserializeObject(valueSig, stream)
						);
					}
					
					if(stream.getBytesRead() != limit)
					{
						throw new IOException(
							"Position in stream does not match expected position; " 
							+ stream.getBytesRead() + " != " + limit
						);
					}
					
					return array;
				}
			case VARIANT:
				{
					String variantStr = stream.readSignature();
					Signature variantSig = Signature.parse(variantStr);
					SubSignature[] variantSubs = variantSig.getSignatures();
					
					if(variantSubs.length > 1)
					{
						throw new IOException("Invalid variant, got complex signature: " + variantSig);
					}
					else if(variantSubs.length == 0)
					{
						return new Variant(variantSig, null);
					}
					else
					{
						Object value = deserializeObject(variantSubs[0], stream);
						return new Variant(variantSig, value);
					}
				}
			case DICT_ENTRY:
				{
					// Dictionary entry
					
					stream.readPad(8);
					
					Object key = deserializeObject(subs[0], stream);
					Object value = deserializeObject(subs[1], stream);

					return new DictEntry(key, value);
				}
			case STRUCT:
				{
					// Struct
					
					stream.readPad(8);
					
					List<Object> data = new LinkedList<Object>();
					for(int i=0, n=subs.length; i<n; i++)
					{
						data.add(deserializeObject(subs[i], stream));
					}
					
					return new Struct(data.toArray());
				}
		}
		
		throw new IOException("Unable to deserialize object of type " + type);
	}
	
	/**
	 * Perform serialization of a single object based on the signature.
	 * 
	 * @param sig
	 * @param o
	 * @param stream
	 * @throws IOException
	 */
	private static void serializeObject(SubSignature sig, Object o, 
			DBusOutputStream stream)
		throws IOException
	{
		DType type = sig.getType();
		SubSignature[] subs = sig.getSignatures();
		
		switch(type)
		{
			case BYTE:
				if(false == o instanceof Number)
				{
					throw new IOException("Expected byte, was " + o);
				}
				
				stream.writeByte(((Number) o).byteValue());
				break;
			case BOOLEAN:
				if(false == o instanceof Boolean)
				{
					throw new IOException("Expected boolean, was " + o);
				}
				
				stream.writeBoolean((Boolean) o);
				break;
			case INT16:
				if(false == o instanceof Number)
				{
					throw new IOException("Expected int16, was " + o);
				}
				
				stream.writeInt16(((Number) o).shortValue());
				break;
			case UINT16:
				if(false == o instanceof Number)
				{
					throw new IOException("Expected uint16, was " + o);
				}
				
				stream.writeUInt16(((Number) o).intValue());
				break;
			case INT32:
				if(false == o instanceof Number)
				{
					throw new IOException("Expected int32, was " + o);
				}
				
				stream.writeInt32(((Number) o).intValue());
				break;
			case UINT32:
				if(false == o instanceof Number)
				{
					throw new IOException("Expected uint32, was " + o);
				}
				
				stream.writeUInt32(((Number) o).longValue());
				break;
			case INT64:
				if(false == o instanceof Number)
				{
					throw new IOException("Expected int64, was " + o);
				}
				
				stream.writeInt64(((Number) o).longValue());
				break;
			case UINT64:
				if(o instanceof UInt64)
				{
					stream.writeUInt64(((UInt64) o).getValue());
				}
				else if(o instanceof BigInteger)
				{
					stream.writeUInt64((BigInteger) o);
				}
//				else if(o instanceof Number)
//				{
//					stream.writeUInt64(((Number) o).longValue());
//				}
				else
				{
					throw new IOException("Expected int16, was " + o);
				}
				break;
			case DOUBLE:
				if(false == o instanceof Number)
				{
					throw new IOException("Expected double, was " + o);
				}
				
				stream.writeDouble(((Number) o).doubleValue());
				break;
			case STRING:
				if(false == o instanceof String)
				{
					throw new IOException("Expected string, was " + o);
				}
				
				stream.writeString((String) o);
				break;
			case OBJECT_PATH:
				if(false == o instanceof ObjectPath)
				{
					throw new IOException("Expected object path, was " + o);
				}
				
				stream.writeObjectPath(((ObjectPath) o).getPath());
				break;
			case SIGNATURE:
				if(false == o instanceof Signature)
				{
					throw new IOException("Expected signature, was " + o);
				}
				
				stream.writeSignature(((Signature) o).getValue());
				break;
			case ARRAY:
				if(o == null || (false == o instanceof Collection<?> &&  
					false == o.getClass().isArray()))
				{
					throw new IOException("Expected array or list, was " + o);
				}
				
				if(o instanceof byte[])
				{
					// Speedup writing of byte arrays
					byte[] data = (byte[]) o;
					stream.writeUInt32(data.length);
					stream.writePad(1);
					stream.write(data);
				}
				else
				{
					/* Serialize the array into its own stream (so we can write
					 * the correct length)
					 */
					ByteArrayOutputStream byteStream = 
						new ByteArrayOutputStream();
					DBusOutputStream out = new DBusOutputStream(byteStream);
					
					SubSignature valueSig = subs[0];
					// Find the alignment
					int alignment = getAlignment(valueSig.getType());
					
					if(o instanceof Collection<?>)
					{
						for(Object value : (List<?>) o)
						{
							serializeObject(valueSig, value, out);
						}
					}
					else
					{
						for(int k=0, n=Array.getLength(o); k<n; k++)
						{
							Object value = Array.get(o, k);
							
							serializeObject(valueSig, value, out);
						}
					}
					
					// Now write length, alignment and data
					byte[] data = byteStream.toByteArray();
					stream.writeUInt32(data.length);
					
					// Pad to alignment
					stream.writePad(alignment);
					
					// Write the data
					stream.write(data);
				}
				break;
			case VARIANT:
				if(false == o instanceof Variant)
				{
					throw new IOException("Expected Variant, was " + o);
				}
				
				Variant variant = (Variant) o;
				
				Signature variantSig = variant.getSignature();
				stream.writeSignature(variantSig.getValue());
				
				SubSignature subSig = variantSig.getSignatures()[0];
				serializeObject(subSig, variant.getValue(), stream);
				break;
			case DICT_ENTRY:
				if(false == o instanceof DictEntry)
				{
					throw new IOException("Expected dictionary entry, was " + o);
				}
				
				{
					stream.writePad(8);
					
					DictEntry entry = (DictEntry) o;
					serializeObject(subs[0], entry.getKey(), stream);
					serializeObject(subs[1], entry.getValue(), stream);
				}
				break;
			case STRUCT:
				if(false == o instanceof Struct)
				{
					throw new IOException("Expected struct, was " + o);
				}
				
				{
					stream.writePad(8);
					
					Struct struct = (Struct) o;
					
					Object[] data = struct.getData();
					if(data.length != subs.length)
					{
						throw new IOException("Expected " + subs.length + " objects, got " + data.length);
					}
					
					for(int i=0, n=data.length; i<n; i++)
					{
						serializeObject(subs[i], data[i], stream);
					}
				}
				break;
				
			default:
				throw new IOException("Unknown type " + type + " for " + o);
		}
	}
	
}
