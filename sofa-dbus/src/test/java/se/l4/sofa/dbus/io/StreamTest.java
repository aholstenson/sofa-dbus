package se.l4.sofa.dbus.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import se.l4.sofa.dbus.spi.Endian;

public class StreamTest
{
	public static void main(String[] args)
		throws Exception
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DBusOutputStream dbusOut = new DBusOutputStream(out);
		dbusOut.setEndian(Endian.LITTLE);
		dbusOut.writeUInt32(78);
		byte[] data = out.toByteArray();
		
		DBusInputStream in = new DBusInputStream(
			new ByteArrayInputStream(data)
		);
		in.setEndian(Endian.LITTLE);
		
		System.out.println(in.readUInt32());
	}
}
