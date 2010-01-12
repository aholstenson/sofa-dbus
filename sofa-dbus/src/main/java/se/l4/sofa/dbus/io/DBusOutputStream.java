package se.l4.sofa.dbus.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.sofa.dbus.spi.Endian;

/**
 * Output stream adapted for DBus marshalling, wraps another {@link OutputStream}
 * and allows for easy access to DBus-types. The stream can operate with
 * different endians, use {@link #setEndian(Endian)} to define the currently
 * active one.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusOutputStream
	extends OutputStream
{
	private static final Logger logger = LoggerFactory.getLogger(DBusOutputStream.class);
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final byte[] PAD_ARRAY = new byte[8];
	
	private final OutputStream stream;
	
	private Endian endian;
	private long bytesWritten;
	
	private final byte[] buffer;
	
	public DBusOutputStream(OutputStream stream)
	{
		this.stream = stream;
		
		endian = Endian.BIG;
		
		buffer = new byte[8];
	}
	
	/**
	 * Get the endian that this output stream uses.
	 * 
	 * @return
	 */
	public Endian getEndian()
	{
		return endian;
	}
	
	/**
	 * Set the endian that this output stream should use.
	 * 
	 * @param endian
	 */
	public void setEndian(Endian endian)
	{
		this.endian = endian;
	}
	
	@Override
	public void write(int b)
		throws IOException
	{
		stream.write(b);
		
		bytesWritten++;
	}
	
	@Override
	public void write(byte[] b, int off, int len)
		throws IOException
	{
		stream.write(b, off, len);
		
		bytesWritten += len;
	}
	
	@Override
	public void write(byte[] b) throws IOException
	{
		stream.write(b);
		
		bytesWritten += b.length;
	}
	
	/**
	 * Write a byte.
	 * 
	 * @param b
	 * @throws IOException
	 */
	public void writeByte(int b)
		throws IOException
	{
		writePad(1);
		
		write(b);
	}
	
	/**
	 * Write a boolean.
	 * 
	 * @param b
	 * @throws IOException
	 */
	public void writeBoolean(boolean b)
		throws IOException
	{
		writeUInt32(b ? 1 : 0);
	}

	/**
	 * Write a signed 16-bit integer to the stream.
	 * 
	 * @param i
	 * @throws IOException
	 */
	public void writeInt16(int i)
		throws IOException
	{
		writePad(2);
		
		switch(endian)
		{
			case BIG:
				write((i >>> 8) & 0xFF);
				write((i >>> 0) & 0xFF);
				break;
			case LITTLE:
				write((i >>> 0) & 0xFF);
				write((i >>> 8) & 0xFF);
				break;
			default:
				throw new IllegalArgumentException("Unknown endian " + endian);
		}
	}
	
	/**
	 * Write an unsigned 16-bit integer to the stream.
	 * 
	 * @param i
	 * @throws IOException
	 */
	public void writeUInt16(int i)
		throws IOException
	{
		writeInt16(i);
	}
	
	/**
	 * Write a signed 32-bit integer to the stream.
	 * 
	 * @param i
	 * @throws IOException
	 */
	public void writeInt32(int i)
		throws IOException
	{
		writePad(4);
		
		switch(endian)
		{
			case BIG:
				buffer[0] = (byte) (i >>> 24);
				buffer[1] = (byte) (i >>> 16);
				buffer[2] = (byte) (i >>>  8);
				buffer[3] = (byte) (i >>>  0);
				break;
			case LITTLE:
				buffer[0] = (byte) (i >>>  0);
				buffer[1] = (byte) (i >>>  8);
				buffer[2] = (byte) (i >>> 16);
				buffer[3] = (byte) (i >>> 24);
				break;
			default:
				throw new IllegalArgumentException("Unknown endian " + endian);
		}
		
		write(buffer, 0, 4);
	}
	
	/**
	 * Write an unsigned 32-bit integer to the stream.
	 * 
	 * @param i
	 * @throws IOException
	 */
	public void writeUInt32(long i)
		throws IOException
	{
		writePad(4);
		
		switch(endian)
		{
			case BIG:
				buffer[0] = (byte) (i >>> 24);
				buffer[1] = (byte) (i >>> 16);
				buffer[2] = (byte) (i >>>  8);
				buffer[3] = (byte) (i >>>  0);
				break;
			case LITTLE:
				buffer[0] = (byte) (i >>>  0);
				buffer[1] = (byte) (i >>>  8);
				buffer[2] = (byte) (i >>> 16);
				buffer[3] = (byte) (i >>> 24);
				break;
			default:
				throw new IllegalArgumentException("Unknown endian " + endian);
		}
		
		write(buffer, 0, 4);
	}
	
	/**
	 * Write a signed 64-bit integer to the stream.
	 * 
	 * @param i
	 * @throws IOException
	 */
	public void writeInt64(long i)
		throws IOException
	{
		writePad(8);
		
		switch(endian)
		{
			case BIG:
				buffer[0] = (byte) (i >>> 56);
				buffer[1] = (byte) (i >>> 48);
				buffer[2] = (byte) (i >>> 40);
				buffer[3] = (byte) (i >>> 32);
				buffer[4] = (byte) (i >>> 24);
				buffer[5] = (byte) (i >>> 16);
				buffer[6] = (byte) (i >>>  8);
				buffer[7] = (byte) (i >>>  0);
				break;
			case LITTLE:
				buffer[0] = (byte) (i >>>  0);
				buffer[1] = (byte) (i >>>  8);
				buffer[2] = (byte) (i >>> 16);
				buffer[3] = (byte) (i >>> 24);
				buffer[4] = (byte) (i >>> 32);
				buffer[5] = (byte) (i >>> 40);
				buffer[6] = (byte) (i >>> 48);
				buffer[7] = (byte) (i >>> 56);
				break;
			default:
				throw new IllegalArgumentException("Unknown endian " + endian);
		}
		
		write(buffer, 0, 8);
	}
	
	public void writeUInt64(BigInteger i)
		throws IOException
	{
		throw new UnsupportedOperationException("UInt64 not supported yet");
	}
	
	/**
	 * Write an IEEE 754 double to the stream.
	 * 
	 * @param d
	 * @throws IOException
	 */
	public void writeDouble(double d)
		throws IOException
	{
		long l = Double.doubleToRawLongBits(d);
		
		writeInt64(l);
	}
	
	/**
	 * Write a string to the stream.
	 * 
	 * @param s
	 * @throws IOException
	 */
	public void writeString(String s)
		throws IOException
	{
		byte[] data = s.getBytes(UTF8);
		
		writeUInt32(data.length);
		
		write(data);
		write(0); // NUL
	}
	
	public void writeObjectPath(String path)
		throws IOException
	{
		writeString(path);
	}
	
	public void writeSignature(String signature)
		throws IOException
	{
		byte[] data = signature.getBytes(UTF8);
		
		writePad(1);
		write(data.length);
		write(data);
		write(0); // NUL
	}
	
	public void writePad(int alignment)
		throws IOException
	{
		int pad = (int) (bytesWritten % alignment);
		
		if(pad > 0)
		{
			if(logger.isTraceEnabled())
			{
				logger.trace("Padding " + (alignment - pad) + " from " + bytesWritten + " with align " + alignment);
			}
			
			write(PAD_ARRAY, 0, alignment - pad);
		}
	}
	
	public long getBytesWritten()
	{
		return bytesWritten;
	}
	
	public void resetBytesWritten()
	{
		bytesWritten = 0;
	}
}

