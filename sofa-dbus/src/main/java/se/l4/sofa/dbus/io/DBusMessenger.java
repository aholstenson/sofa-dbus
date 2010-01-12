package se.l4.sofa.dbus.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.sofa.dbus.spi.Endian;
import se.l4.sofa.dbus.spi.Marshalling;
import se.l4.sofa.dbus.spi.Message;
import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.Struct;

/**
 * Messenger class that is used to send and receive messages.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusMessenger
{
	private static final Logger logger = LoggerFactory.getLogger(DBusMessenger.class);
	
	private static final int PROTOCOL_VERSION = 1;
	private static final Signature HEADER_SIG = Signature.parse("a(yv)");
	
	private final DBusInputStream in;
	private final DBusOutputStream out;
	
	public DBusMessenger(InputStream in, OutputStream out)
	{
		this.in = new DBusInputStream(in);
		this.out = new DBusOutputStream(out);
	}
	
	public synchronized void writeMessage(Message m)
		throws IOException, InterruptedException
	{
		logger.debug("Writing message {}", m);
		
		out.resetBytesWritten();
//		out.writePad(8);
		
		// Write endian and update stream to use the endian of the message
		Endian endian = m.getEndian();
		switch(endian)
		{
			case BIG:
				out.writeByte('B');
				break;
			case LITTLE:
				out.writeByte('l');
				break;
			default:
				throw new IllegalArgumentException("Unknown endian " + endian);
		}
		
		out.setEndian(endian);
		
		// Message type, flags and protocol version
		out.writeByte(m.getType());
		out.writeByte(m.getFlags());
		out.writeByte(PROTOCOL_VERSION);

		
		// Length of message
		byte[] body = m.getBody();
		out.writeUInt32(body.length);
		
		// Serial
		out.writeUInt32(m.getSerial());
		
		// Write fields
//		out.writeUInt32(0);
//		out.writePad(8);
		List<Struct> fields = m.getFields();
		Marshalling.serialize(HEADER_SIG, new Object[] { fields }, out);
		
		// Padding
		out.writePad(8);
//		long bytesWritten = out.getBytesWritten();
//		int toWrite = 8 - (int) (bytesWritten % 8);
//		System.out.println(bytesWritten + " " + toWrite);
//		
//		if(toWrite < 8)
//		{
//			out.write(new byte[toWrite]);
//		}
//		
		// Write body
		out.write(body);
		
		// Ensure that we flush
		out.flush();
	}
	
	public Message readMessage()
		throws IOException
	{
		in.resetBytesRead();
		
		char c = (char) in.readByte();
		Endian endian = null;
		switch(c)
		{
			case 'l':
				endian = Endian.LITTLE;
				break;
			case 'B':
				endian = Endian.BIG;
				break;
			default:
				throw new IOException("Unknown endian " + c);
		}
		
		in.setEndian(endian);
		
		int type = in.readByte();
		int flags = in.readByte();
		int protocolVersion = in.readByte();
		long length = in.readUInt32();
		long serial = in.readUInt32();
		
		List<Object> fields = Marshalling.deserialize(HEADER_SIG, in);
		
		in.readPad(8);
		byte[] data = new byte[(int) length];
		in.read(data);
		
		Message msg = new Message(endian, type, flags, serial, data);
		msg.addFields((List<Object>) fields.get(0));
		
		logger.debug("Read message {}", msg);
		
		return msg;
	}
}
