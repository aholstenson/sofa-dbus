package se.l4.sofa.dbus.io.unix;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

import se.l4.sofa.dbus.io.unix.UnixSocket.SocketLibC.SocketAddressUn;

/**
 * Unix socket implementation using JNA. Currently only supports reading from
 * a local UNIX socket.
 * 
 * <p>
 * This class is tested on UNIX systems that are using 
 * <a href="http://www.gnu.org/software/libc">GNU C Library</a>. It might
 * work with other UNIX-like systems, but no guarantees are given.
 * 
 * @author Andreas Holstenson
 *
 */
public class UnixSocket
{
	/**
	 * Native facade to {@code libc} that we use to create our own socket. Just
	 * acts as a wrapper via {@code JNA}. The <a
	 * href="http://www.gnu.org/software/libc/manual/">GNU C Library homepage</a>
	 * contains good documentation for the library.
	 * 
	 * 
	 * @author Andreas Holstenson
	 * 
	 */
	public static interface SocketLibC
		extends Library
	{
		/** Local (Unix) socket. */
		public static final int PF_LOCAL = 1;
		/** Inet socket. */
		public static final int PF_INET = 2;

		/** Socket style of Stream. */
		public static final int SOCK_STREAM = 1;
		/** Socket style of Datagram. */
		public static final int SOCK_DGRAM = 2;
		/** Socket style of Raw. */
		public static final int SOCK_RAW = 3;

		/**
		 * Create a socket.
		 * 
		 * @param namespace
		 * 		namespace to use, either {@link #PF_LOCAL} or
		 * 		{@link #PF_INET}
		 * @param style
		 * 		style of socket, one of {@link #SOCK_STREAM},
		 * 		{@link #SOCK_DGRAM} or {@link #SOCK_RAW}.
		 * @param protocol
		 * 		protocol to use, usually zero
		 * @return
		 * 		socket file descriptor, or -1 if error
		 */
		int socket(int namespace, int style, int protocol);
		
		/**
		 * Connect via socket, using {@link SocketAddressUn} to determine
		 * where.
		 * 
		 * @param socket
		 * 		socket file descriptor
		 * @param address
		 * 		address
		 * @param addressLen
		 * 		length of {@code address}, use {@link SocketAddressUn#size()}.
		 * @return
		 */
		int connect(int socket, SocketAddressUn address, int addressLen);

		/**
		 * Receive data from the socket.
		 * 
		 * @param socket
		 * @param buf
		 * @param len
		 * @param flags
		 * @return
		 */
		int recv(int socket, Buffer buf, int len, int flags);

		/**
		 * Sen data through socket.
		 * 
		 * @param socket
		 * @param msg
		 * @param len
		 * @param flags
		 * @return
		 */
		int send(int socket, Buffer msg, int len, int flags);

		/**
		 * Close socket.
		 * 
		 * @param s
		 * @return
		 */
		int close(int s);

		/**
		 * Socket address, same as {@code sockaddr_un}.
		 * 
		 * @author Andreas Holstenson
		 *
		 */
		public static class SocketAddressUn
			extends Structure
		{
			public final static int LENGTH = 106;
			
			public short sun_family;

			public byte[] sun_path = new byte[LENGTH - 2];
		}
	}
	
	/** Library instance. */
	private static final SocketLibC LIB;

	/** Path where to connect. */
	private final String path;
	
	/** File descriptor of Unix socket. */
	private int socket;
	
	/** Channel used for communication. */
	private UnixSocketChannel channel;
	
	/** Static loading of library. */
	static
	{
		SocketLibC lib;
		try
		{
			lib = (SocketLibC) Native.loadLibrary("c", SocketLibC.class);
		}
		catch(Throwable t)
		{
			lib = null;
		}
		
		LIB = lib;
	}
	
	/**
	 * Check if it is possible to use UNIX sockets.
	 * 
	 * @return
	 * 		{@code true} if available, otherwise {@code false}.
	 */
	public static boolean isAvailable()
	{
		return LIB != null;
	}
	
	public UnixSocket(String path)
	{
		if(false == isAvailable())
		{
			throw new IllegalStateException("Can not create UNIX socket; Native library is unavailable");
		}
		
		this.path = path;
	}
	
	public void connect()
		throws IOException
	{
		socket = LIB.socket(SocketLibC.PF_LOCAL, SocketLibC.SOCK_STREAM, 0);
		if(socket < 0)
		{
			throw new IOException("Unable to create UNIX socket; "
				+ UnixSocketMessages.getMessage(Native.getLastError()));
		}

		// Start setting up socket address
		SocketAddressUn addr = new SocketAddressUn();
		addr.sun_family = SocketLibC.PF_LOCAL;
		
		// Check size, just to make sure
		byte[] pathBytes = path.getBytes();
		int pathLen = pathBytes.length;
		
		if(pathLen >= addr.sun_path.length)
		{
			throw new IOException("Unable to open UNIX socket, given path too "
					+ "long (max: " + (pathLen - 1) + "); Path: " + path);
		}
		
		// Copy path into addr and then connect
		int length = 0;
		boolean abstractPath = pathBytes[0] == 0;
		if(abstractPath)
		{
			addr.sun_path = pathBytes;
//			System.arraycopy(pathBytes, 0, addr.sun_path, 0, pathLen);
			length = 2 + pathBytes.length;
		}
		else
		{
			System.arraycopy(pathBytes, 0, addr.sun_path, 0, pathLen);
			length = addr.size();
		}
		
		try
		{
			int status = LIB.connect(socket, addr, length);
			if(status < 0)
			{
				throw new RuntimeException(
					UnixSocketMessages.getMessage(
						Native.getLastError()
					)
				); 
			}
		}
		catch(Exception e)
		{
			LIB.close(socket);
			
			throw new IOException("Unable to bind/connect UNIX socket; " 
				+ e.getMessage());
		}
		
		channel = new UnixSocketChannel(socket);
	}
	
	/**
	 * Close the socket.
	 * 
	 * @throws IOException
	 */
	public void close()
		throws IOException
	{
		try
		{
			if(socket >= 0)
			{
				LIB.close(socket);
			}
		}
		catch(Exception e)
		{
			throw new IOException("Unable to close UNIX socket; "
				+ e.getMessage());
		}
	}
	
	/**
	 * Retrieve a channel that can be used for communicating with this socket.
	 * 
	 * @return
	 * 		channel used for communication
	 */
	public UnixSocketChannel getChannel()
	{
		return channel;
	}
	
	public InputStream getInputStream()
	{
		return new UnixInputStream(channel);
	}
	
	public OutputStream getOutputStream()
	{
		return new UnixOutputStream(channel);
	}
	
	/**
	 * Channel for {@link UnixSocket} that can be used for reading and writing
	 * to and from the socket.
	 * 
	 * @author Andreas Holstenson
	 *
	 */
	public static class UnixSocketChannel
		implements WritableByteChannel, ReadableByteChannel
	{
		private int socket;
		private boolean active;
		
		protected UnixSocketChannel(int socket)
		{
			this.socket = socket;
			active = true;
		}
		
		public int read(ByteBuffer dst) throws IOException
		{
			if(dst.remaining() == 0)
			{
				return 0;
			}
			
			int read = LIB.recv(socket, dst, dst.remaining(), 0);
			
			if(read >= 0)
			{
				int pos = dst.position();
				dst.position(pos + read);
			}
			else
			{
				throw new IOException("Unable to read from UNIX socket; "
					+ UnixSocketMessages.getMessage(Native.getLastError()));
			}
			
			return read;
			
		}
		
		public int write(ByteBuffer src) throws IOException
		{
			int written = LIB.send(socket, src, src.remaining(), 0);
			
			if(written >= 0)
			{
				int pos = src.position();
				src.position(pos + written);
			}
			else
			{
				throw new IOException("Unable to write to UNIX socket; "
					+ UnixSocketMessages.getMessage(Native.getLastError()));
			}
			
			return written;
		}

		public boolean isOpen()
		{
			return active;
		}

		public void close()
			throws IOException
		{
			try
			{
				LIB.close(socket);
				active = false;
			}
			catch(Exception e)
			{
				throw new IOException("Unable to close UNIX socket; " 
					+ e.getMessage());
			}
		}
		
	}
	
	private static class UnixInputStream
		extends InputStream
	{
		private final UnixSocketChannel channel;
		private byte[] data;
		
		public UnixInputStream(UnixSocketChannel channel)
		{
			this.channel = channel;
			data = new byte[1];
		}
		
		@Override
		public int read()
			throws IOException
		{
			int i = read(data);
			
			return i != 1 ? -1 : data[0];
		}
		
		@Override
		public int read(byte[] b)
			throws IOException
		{
			ByteBuffer buf = ByteBuffer.wrap(b);
			return channel.read(buf);
		}
		
		@Override
		public int read(byte[] b, int off, int len)
			throws IOException
		{
			ByteBuffer buf = ByteBuffer.wrap(b, off, len);
			return channel.read(buf);
		}
	}
	
	private static class UnixOutputStream
		extends OutputStream
	{
		private final UnixSocketChannel channel;
		private byte[] data;
		
		public UnixOutputStream(UnixSocketChannel channel)
		{
			this.channel = channel;
			data = new byte[1];
		}
		
		@Override
		public void write(int b)
			throws IOException
		{
			data[0] = (byte) b;
			write(data);
		}
		
		@Override
		public void write(byte[] b)
			throws IOException
		{
			ByteBuffer buf = ByteBuffer.wrap(b);
			channel.write(buf);
		}
		
		@Override
		public void write(byte[] b, int off, int len)
			throws IOException
		{
			ByteBuffer buf = ByteBuffer.wrap(b, off, len);
			channel.write(buf);
		}
	}
}
