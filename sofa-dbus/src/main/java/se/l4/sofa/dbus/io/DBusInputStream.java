package se.l4.sofa.dbus.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.sofa.dbus.spi.Endian;

/**
 * Input stream adapted to DBus unmarshalling, wraps another {@link InputStream}
 * and allows for easy access to DBus-types. The stream can operate with
 * different endians, use {@link #setEndian(Endian)} to define the currently
 * active one.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusInputStream
	extends InputStream
{
	private static final Logger logger = LoggerFactory.getLogger(DBusInputStream.class);
	
	private static final Charset UTF8 = Charset.forName("UTF-8");
	
	/** Stream to read from. */
	private final InputStream stream;
	/** Temporary buffer for reads */
	private final byte[] buffer;
	
	/** Current endian, default to {@link Endian#BIG}. */
	private Endian endian;
	
	private long bytesRead;
	
	public DBusInputStream(InputStream stream)
	{
		this.stream = stream;
		
		buffer = new byte[8];
		endian = Endian.BIG;
	}
	
	@Override
	public int read()
		throws IOException
	{
		bytesRead++;
		int i = stream.read();
		return i;
	}
	
	@Override
	public int read(byte[] b)
		throws IOException
	{
		int read = stream.read(b);
		bytesRead += read;
		return read;
	}
	
	@Override
	public int read(byte[] b, int off, int len)
		throws IOException
	{
		int read = stream.read(b, off, len);
		bytesRead += read;
		return read;
	}
	
	/**
	 * Set the currently active endian.
	 * 
	 * @param endian
	 */
	public void setEndian(Endian endian)
	{
		this.endian = endian;
	}
	
	/**
	 * Get the currently active endian.
	 * 
	 * @return
	 */
	public Endian getEndian()
	{
		return endian;
	}
	
	/**
	 * Read a single byte from the stream.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int readByte()
		throws IOException
	{
		readPad(1);
		
		int i = read();
		if(i == -1)
		{
			throw new EOFException();
		}
		
		return i;
	}
	
	/**
	 * Read a boolean from the stream.
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean readBoolean()
		throws IOException
	{
		long l = readUInt32();
		
		return l == 1;
	}
	
	/**
	 * Read a signed 16-bit integer from the stream.
	 * 
	 * @return
	 * 		16-bit signed integer, with same range as {@link Short}
	 * @throws IOException
	 * 		if unable to read from stream
	 */
	public int readInt16()
		throws IOException
	{
		readPad(2);
		if(read(buffer, 0, 2) < 2)
		{
			throw new EOFException();
		}
		
		switch(endian)
		{
			case BIG:
				return (short) ((buffer[0] & 0xFF) << 8 | (buffer[1] & 0xFF));
			case LITTLE:
				return (short) ((buffer[1] & 0xFF) << 8 | (buffer[0] & 0xFF));
			default:
				throw new IllegalArgumentException("Unknown endian " + endian);
		}
	}
	
	/**
	 * Read an unsigned 16-bit integer from the stream.
	 * 
	 * @return
	 * @throws IOException
	 */
	public int readUInt16()
		throws IOException
	{
		int i = readInt16();
		return i & 0xFFFF;
	}
	
	/**
	 * Read a signed 32-bit integer from the stream.
	 * 
	 * @return
	 * 		signed 32-bit integer
	 * @throws IOException
	 * 		if unable to read from the stream
	 */
	public int readInt32()
		throws IOException
	{
		readPad(4);
		if(read(buffer, 0, 4) < 4)
		{
			throw new EOFException();
		}
		
		switch(endian)
		{
			case BIG:
				return (buffer[0] & 0xFF) << 24
					| (buffer[1] & 0xFF) << 16
					| (buffer[2] & 0xFF) << 8
					| (buffer[3] & 0xFF);
			case LITTLE:
				return (buffer[3] & 0xFF) << 24
					| (buffer[2] & 0xFF) << 16
					| (buffer[1] & 0xFF) << 8
					| (buffer[0] & 0xFF);
			default:
				throw new IllegalArgumentException("Unknown endian " + endian);
		}
	}
	
	/**
	 * Read an unsigned 32-bit integer from the stream.
	 * 
	 * @return
	 * @throws IOException
	 */
	public long readUInt32()
		throws IOException
	{
		int i = readInt32();
		
		return i & 0xFFFFFFFFL;
	}
	
	/**
	 * Read a signed 64-bit integer from the stream.
	 * 
	 * @return
	 * 		signed 64-bit integer
	 * @throws IOException
	 */
	public long readInt64()
		throws IOException
	{
		readPad(8);
		if(read(buffer, 0, 8) < 8)
		{
			throw new EOFException();
		}
		
		switch(endian)
		{
			case BIG:
				return (long) (buffer[0] & 0xFF) << 56
					| (buffer[1] & 0xFF) << 48
					| (buffer[2] & 0xFF) << 40
					| (buffer[3] & 0xFF) << 32
					| (buffer[4] & 0xFF) << 24
					| (buffer[5] & 0xFF) << 16
					| (buffer[6] & 0xFF) << 8
					| (buffer[7] & 0xFF);
			case LITTLE:
				return (long) (buffer[7] & 0xFF) << 56
					| (buffer[6] & 0xFF) << 48
					| (buffer[5] & 0xFF) << 40
					| (buffer[4] & 0xFF) << 32
					| (buffer[3] & 0xFF) << 24
					| (buffer[2] & 0xFF) << 16
					| (buffer[1] & 0xFF) << 8
					| (buffer[0] & 0xFF);
			default:
				throw new IllegalArgumentException("Unknown endian " + endian);
		}
	}
	
	public BigInteger readUInt64()
		throws IOException
	{
		long l = readInt64();
		
//		BigInteger i = BigInteger.valueOf(l)
//			.
		throw new UnsupportedOperationException("UInt64 not implemented yet");
	}
	
	/**
	 * Read a IEEE 754 double from the stream.
	 * 
	 * @return
	 * @throws IOException
	 */
	public double readDouble()
		throws IOException
	{
		long l = readInt64();
		
		return Double.longBitsToDouble(l);
	}
	
	/**
	 * Read a string from the stream.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String readString()
		throws IOException
	{
		long length = readUInt32();
		
		// TODO: Are we breaking the spec as we can't read larger strings?
		byte[] buffer = new byte[(int) length];
		int len = read(buffer);
		if(len < length)
		{
			throw new EOFException();
		}
		
		int terminator = read();
		if(terminator != 0)
		{
			throw new IOException("Expected NUL terminator for string");
		}
		
		return new String(buffer, UTF8);
	}
	
	/**
	 * Read an object path from the stream (same as {@link #readString()}.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String readObjectPath()
		throws IOException
	{
		return readString();
	}
	
	/**
	 * Read a signature string from the stream. Same as {@link #readString()}
	 * but with a length of just one byte.
	 * 
	 * @return
	 * @throws IOException
	 */
	public String readSignature()
		throws IOException
	{
		int length = readByte();
		
		byte[] buffer = new byte[length];
		int len = read(buffer);
		if(len < length)
		{
			throw new EOFException();
		}
		
		int terminator = read();
		if(terminator != 0)
		{
			throw new IOException("Expected NUL terminator for string");
		}
		
		return new String(buffer, UTF8);
	}
	
	/**
	 * Read a number of padding bytes to align the type.
	 * 
	 * @param alignment
	 * @throws IOException
	 */
	public void readPad(int alignment)
		throws IOException
	{
		int pad = (int) (bytesRead % alignment);
		
		if(pad > 0)
		{
			if(logger.isTraceEnabled())
			{
				logger.trace("Padding " + (alignment - pad) + " from " + bytesRead + " with align " + alignment);
			}
			
			int n = read(buffer, 0, alignment - pad);
			
			
			for(int i=0; i<n; i++)
			{
				if(buffer[i] != 0)
				{
					throw new IOException("Byte " + buffer[i] + " found at " + i + " where NUL was expected");
				}
			}
		}
	}

	/**
	 * Get the number of bytes read since last reset.
	 * 
	 * @return
	 */
	public long getBytesRead()
	{
		return bytesRead;
	}
	
	/**
	 * Clear the number of bytes read (to ready the stream for the next message).
	 */
	public void resetBytesRead()
	{
		bytesRead = 0;
	}
}
