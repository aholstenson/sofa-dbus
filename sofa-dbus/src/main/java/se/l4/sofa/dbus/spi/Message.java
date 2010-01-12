package se.l4.sofa.dbus.spi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.l4.sofa.dbus.io.DBusInputStream;

/**
 * Representation of a DBus message. The message is used to carry different
 * types of commands between peers. Handling of messages is usually done via
 * a {@link MessageHandler}.
 * 
 * @author Andreas Holstenson
 *
 */
public class Message
{
	public static final int TYPE_INVALID = 0;
	public static final int TYPE_METHOD_CALL = 1;
	public static final int TYPE_METHOD_RETURN = 2;
	public static final int TYPE_ERROR = 3;
	public static final int TYPE_SIGNAL = 4;
	
	public static final int FLAG_NO_REPLY_EXPECTED = 0x1;
	public static final int FLAG_NO_AUTO_START = 0x2;
	
	public static final int FIELD_INVALID = 0;
	public static final int FIELD_PATH = 1;
	public static final int FIELD_INTERFACE = 2;
	public static final int FIELD_MEMBER = 3;
	public static final int FIELD_ERROR_NAME = 4;
	public static final int FIELD_REPLY_SERIAL = 5;
	public static final int FIELD_DESTINATION = 6;
	public static final int FIELD_SENDER = 7;
	public static final int FIELD_SIGNATURE = 8;
	
	private final Endian endian;
	private final int type;
	private final int flags;
	private final long serial;
	private final List<Struct> fields;
	
	private final byte[] body;
	
	public Message(Endian endian, int type, int flags, long serial,
			byte[] body)
	{
		this.endian = endian;
		this.type = type;
		this.flags = flags;
		this.serial = serial;
		this.body = body;
		
		fields = new ArrayList<Struct>(8);
	}
	
	/**
	 * Get the endian of the message.
	 * 
	 * @return
	 */
	public Endian getEndian()
	{
		return endian;
	}
	
	/**
	 * Get the type of the message.
	 * 
	 * @return
	 */
	public int getType()
	{
		return type;
	}
	
	/**
	 * Get the flags of the message.
	 * 
	 * @return
	 */
	public int getFlags()
	{
		return flags;
	}
	
	/**
	 * Get the serial of the message.
	 * 
	 * @return
	 */
	public long getSerial()
	{
		return serial;
	}
	
	/**
	 * Get the raw data of the body.
	 * 
	 * @return
	 */
	public byte[] getBody()
	{
		return body;
	}
	
	/**
	 * Add a new header field.
	 * 
	 * @param field
	 * @param data
	 */
	public void addField(int field, Object data)
	{
		Struct s = new Struct(field, new Variant(data));
		fields.add(s);
	}
	
	/**
	 * Get all header fields.
	 * 
	 * @return
	 */
	public List<Struct> getFields()
	{
		return fields;
	}
	
	/**
	 * Add several header fields in raw format which is a list of structs
	 * which contain an integer and a {@link Variant} with data.
	 * 
	 * @param fields
	 */
	public void addFields(List<Object> fields)
	{
		for(Object o : fields)
		{
			if(o instanceof Struct)
			{
				Struct s = (Struct) o;
				Object[] data = s.getData();
				
				if(data.length > 0 && data[0] instanceof Integer)
				{
					this.fields.add(s);
				}
			}
		}
	}
	
	/**
	 * Get the given field.
	 * 
	 * @param field
	 * @return
	 */
	public Object getField(int field)
	{
		for(Struct s : fields)
		{
			Object[] data = s.getData();
			if(data[0].equals(field))
			{
				return ((Variant) data[1]).getValue();
			}
		}
		
		return null;
	}
	
	/**
	 * Get the body as a stream.
	 *  
	 * @return
	 */
	private DBusInputStream getBodyAsStream()
	{
		ByteArrayInputStream in = new ByteArrayInputStream(body);
		DBusInputStream dbusIn = new DBusInputStream(in);
		dbusIn.setEndian(endian);
		
		return dbusIn;
	}
	
	/**
	 * Get the body of this message as a list of objects. This method
	 * will deserialize based of the {@link #FIELD_SIGNATURE} in the
	 * header.
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<Object> getBodyAsObjects()
		throws IOException
	{
		Signature sig = (Signature) getField(Message.FIELD_SIGNATURE);
		if(sig == null)
		{
			return Collections.emptyList();
		}
		else
		{
			DBusInputStream in = getBodyAsStream();
			return Marshalling.deserialize(sig, in);
		}
	}
	
	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder();
		b
			.append("Message[endian=")
			.append(endian)
			.append(", type=");
		
		switch(type)
		{
			case TYPE_ERROR:
				b.append("ERROR");
				break;
			case TYPE_METHOD_CALL:
				b.append("METHOD_CALL");
				break;
			case TYPE_INVALID:
				b.append("INVALID");
				break;
			case TYPE_METHOD_RETURN:
				b.append("METHOD_RETURN");
				break;
			case TYPE_SIGNAL:
				b.append("SIGNAL");
				break;
			default:
				b.append(type);
		}
		
		// Flags
		b
			.append(", flags=")
			.append(flags)
			.append(" [");
		
		boolean flagged = false;
		if((flags & FLAG_NO_AUTO_START) > 0)
		{
			if(flagged) b.append(" ");
			b.append("NO_AUTO_START");
			flagged = true;
		}
		
		if((flags & FLAG_NO_REPLY_EXPECTED) > 0)
		{
			if(flagged) b.append(" ");
			b.append("NO_REPLY_EXPECTED");
			flagged = true;
		}
		b.append("]");
		
		b
			.append(", serial=")
			.append(serial)
			.append(", length=")
			.append(body.length)
			.append(", fields=")
			.append(fields)
			.append("]");
			
		return b.toString();
	}
}
