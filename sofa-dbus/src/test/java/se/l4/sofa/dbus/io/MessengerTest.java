package se.l4.sofa.dbus.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.testng.annotations.Test;

import se.l4.sofa.dbus.spi.Endian;
import se.l4.sofa.dbus.spi.Marshalling;
import se.l4.sofa.dbus.spi.Message;
import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.UInt32;
import se.l4.sofa.dbus.spi.Variant;

@Test
public class MessengerTest
{
	public void testLittle1()
		throws IOException
	{
		test1(Endian.LITTLE);
	}
	
	public void testBig1()
		throws IOException
	{
		test1(Endian.BIG);
	}
	
	public void testLittle2()
		throws IOException
	{
		test2(Endian.LITTLE);
	}
	
	public void testBig2()
		throws IOException
	{
		test2(Endian.BIG);
	}
	
	public void testLittle3()
		throws IOException
	{
		test3(Endian.LITTLE);
	}
	
	public void testBig3()
		throws IOException
	{
		test3(Endian.BIG);
	}
	
	private void test1(Endian endian)
		throws IOException
	{
		compareSymmetry(
			new Message(endian, 1, 0, 1, output(endian, "Test String"))
		);
	}
	
	private void test2(Endian endian)
		throws IOException
	{
		compareSymmetry(
			new Message(endian, 1, 0, 1, output(endian, "Test String", new String[] { "One", "Two", "Three" }, new UInt32(24)))
		);
	}
	
	private void test3(Endian endian)
		throws IOException
	{
		Message msg = new Message(endian, 1, 0, 1, output(endian, "Test String", new String[] { "One", "Two", "Three" }, new UInt32(24)));
		msg.addField(Message.FIELD_MEMBER, new Variant(78));
		compareSymmetry(msg);
	}
	
	private void compareSymmetry(Message m1)
		throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DBusMessenger messenger = new DBusMessenger(null, out);
		messenger.writeMessage(m1);
		byte[] data = out.toByteArray();
		
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		messenger = new DBusMessenger(in, null);
		Message m2 = messenger.readMessage();
		
		assert m1.getEndian() == m2.getEndian()
			: "Endian mismatch";
		
		assert m1.getFlags() == m2.getFlags()
			: "Flag mismatch";
		
		assert m1.getType() == m2.getType()
			: "Type mismatch";
		
		assert m1.getSerial() == m2.getSerial()
			: "Serial mismatch";
		
		assert checkBytes(m1.getBody(), m2.getBody())
			: "Body mismatch";
		
		assert m1.getFields().equals(m2.getFields())
			: "Fields mismatch";
	}
	
	private boolean checkBytes(byte[] first, byte[] second)
	{
		if(first.length != second.length)
		{
			return false;
		}
		
		for(int i=0, n=first.length; i<n; i++)
		{
			if(first[i] != second[i])
			{
				return false;
			}
		}
		
		return true;
	}
	
	private byte[] output(Endian e, Object... items)
		throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DBusOutputStream dbusOut = new DBusOutputStream(out);
		dbusOut.setEndian(e);
		
		Signature sig = Marshalling.getSignatureForObjects(items);
		Marshalling.serialize(sig, items, dbusOut);
		
		return out.toByteArray();
	}
}
