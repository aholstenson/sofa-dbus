package se.l4.sofa.dbus.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.sofa.dbus.BusAddress;
import se.l4.sofa.dbus.io.unix.UnixSocket;
import se.l4.sofa.dbus.spi.Channel;
import se.l4.sofa.dbus.spi.HandlerChain;
import se.l4.sofa.dbus.spi.Message;

/**
 * Client handling for DBus, connects to a DBus server, performs authentication
 * and then enters send/receive mode. Call {@link #connect()} to attempt
 * connection to server.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusClient
	implements Channel
{
	enum State
	{
		WAITING_FOR_MECHANISMS,
		NEGOTIATE_MECHANISM,
		WAITING_FOR_OK,
		DONE
	}
	
	private static final Logger logger = LoggerFactory.getLogger(DBusClient.class);
	
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	
	private final BusAddress addr;
	private final HandlerChain chain;
	private final String[] mechanisms;
	private final CallbackHandler handler;
	private final BlockingQueue<Message> sendQueue;
	private final BlockingHelper blocking;
	
	private Map<String, Object> saslProperties;
	
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private String serverUuid;
	private SaslClient sc;
	private String hostname;
	
	private long serial;
	
	private Thread writerThread;
	private Thread readerThread;
	private boolean connected;

	private ExecutorService executor;
	
	/**
	 * Create a new client.
	 * 
	 * @param addr
	 * 		bus address to connect to
	 * @param chain
	 * 		handler chain for messages
	 * @param mechanisms
	 * 		array of SASL mechanisms to support
	 * @param handler
	 * 		SASL callback, if mechanism requires things like username and
	 * 		password
	 */
	public DBusClient(BusAddress addr, 
			HandlerChain chain,
			String[] mechanisms,
			CallbackHandler handler)
	{
		this.addr = addr;
		this.mechanisms = mechanisms;
		this.handler = handler;
		this.chain = chain;
		this.sendQueue = new LinkedBlockingQueue<Message>();
		
		this.blocking = new BlockingHelper();
		
		this.executor = Executors.newFixedThreadPool(2, new ThreadFactory()
		{
			private AtomicInteger count = new AtomicInteger();
			
			public Thread newThread(Runnable r)
			{
				return new Thread(r, "dbus-client-" + count.incrementAndGet());
			}
		});
		
		saslProperties = new HashMap<String, Object>();
		
		// Start serial at one
		serial = 1;
	}
	
	/**
	 * Set a custom SASL property.
	 * 
	 * @param prop
	 * @param value
	 */
	public void setSaslProperty(String prop, Object value)
	{
		saslProperties.put(prop, value);
	}
	
	/**
	 * Get a custom SASL property.
	 * 
	 * @param prop
	 * @return
	 */
	public Object getSaslProperty(String prop)
	{
		return saslProperties.get(prop);
	}
	
	/**
	 * Connect and authenticate with the DBus server.
	 * 
	 * @throws IOException
	 * 		if unable to connect or authenticate
	 */
	public void connect()
		throws IOException
	{
		String name;
		
		String proto = addr.getProtocol();
		if("tcp".equals(proto))
		{
			hostname = addr.getParameter("host");
			int port = addr.getIntParameter("port");
			
			if(hostname == null)
			{
				throw new IllegalArgumentException("No hostname specified");
			}
			
			if(port <= 0 || port >= 65535)
			{
				throw new IllegalArgumentException("No valid port specified");
			}
			
			socket = new Socket();
			socket.connect(new InetSocketAddress(hostname, port));
			
			in = socket.getInputStream();
			out = socket.getOutputStream();
			
			name = socket.toString();
		}
		else if("unix".equals(proto))
		{
			String conn = null;
			String abstractPath = addr.getParameter("abstract");
			String normalPath = addr.getParameter("path");
			
			if(abstractPath != null)
			{
				conn = "\0" + abstractPath; // Linux abstract path
			}
			else if(normalPath != null)
			{
				conn = normalPath;
			}
			else
			{
				throw new IllegalArgumentException("Unix connection, requires socket in either path or abstract variable");
			}
			
			UnixSocket socket = new UnixSocket(conn);
			socket.connect();
			
			in = socket.getInputStream();
			out = socket.getOutputStream();
			
			name = abstractPath == null ? normalPath : "abstract:" + abstractPath;
		}
		else
		{
			throw new IllegalArgumentException("Unknown transport " + proto);
		}
		
		negotiate();
		
		// If negotiation succeeded start the connection handling
		DBusMessenger messenger = new DBusMessenger(in, out);
		
		MessageWriter writer = new MessageWriter(messenger, sendQueue);
		MessageReader reader = new MessageReader(this, messenger, chain, blocking);
		
		connected = true;
		
		writerThread = new Thread(writer, "dbus-sender [" + name + "]");
		writerThread.start();
		readerThread = new Thread(reader, "dbus-receiver [" + name + "]");
		readerThread.start();
	}
	
	/**
	 * Attempt reconnected if we are still supposed to be connected.
	 * 
	 * @throws IOException
	 */
	private void reconnect()
	{
		if(false == connected)
		{
			return;
		}
		
		logger.debug("Requesting reconnection with server");
		
		try
		{
			connect();
		}
		catch(IOException e)
		{
			logger.debug("Reconnection failed, disconnecting client");
			
			try
			{
				disconnect();
			}
			catch(IOException e1)
			{
			}
		}
	}
	
	/**
	 * Disconnect from the DBus server.
	 * 
	 * @throws IOException
	 */
	public void disconnect()
		throws IOException
	{
		connected = false;
		
		socket.close();
		socket = null;
		
		writerThread.interrupt();
		readerThread.interrupt();
		
		if(sc != null)
		{
			sc.dispose();
		}
	}
	
	public boolean isConnected()
	{
		return socket != null;
	}
	
	/**
	 * Get the UUID of the connected server.
	 * 
	 * @return
	 * 		UUID of server, {@code null} if not connected
	 */
	public String getServerUuid()
	{
		return serverUuid;
	}
	
	/**
	 * Negotiate authentication via SASL. Implements the SASL profile from the
	 * DBus specification.
	 * 
	 * @throws IOException
	 */
	private void negotiate()
		throws IOException
	{
		State state = State.WAITING_FOR_MECHANISMS;
		
		out.write(0);
		
		sendSaslCommand("AUTH");
		
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(in)
		);
		
		sc = null;
		List<String> validMechanisms = new LinkedList<String>();
		
		String line;
		_outer:
		while((line = reader.readLine()) != null)
		{
			logger.debug("S: {}", line);
			logger.trace("State {}", state);
			
			switch(state)
			{
				case WAITING_FOR_MECHANISMS:
					if(line.startsWith("REJECTED"))
					{
						// Locate the mechanisms supported by both us and the server
						String[] split = line.split(" ");
						
						for(int i=1, n=split.length; i<n; i++)
						{
							String current = split[i];
							
							for(String m : mechanisms)
							{
								if(m.equals(current))
								{
									validMechanisms.add(m);
								}
							}
						}
						
						state = State.NEGOTIATE_MECHANISM;
						sc = sendAuth(validMechanisms);
						if(sc == null)
						{
							sendSaslCommand("CANCEL");
							disconnect();
							break _outer;
						}
					}
					else
					{
						// Cancel the communication
						disconnect();
						break _outer;
					}
					
					break;
					
				case NEGOTIATE_MECHANISM:
					if(line.startsWith("REJECTED") || line.startsWith("ERROR"))
					{
						// Authentication rejected
						sc.dispose();
						
						sc = sendAuth(validMechanisms);
						if(sc == null)
						{
							sendSaslCommand("CANCEL");
							disconnect();
							break _outer;
						}
					}
					else if(line.startsWith("DATA"))
					{
						// We received DATA for the SASL client
						String encodedData = line.substring(5);
						byte[] data = Hex.decodeHex(encodedData.toCharArray());
						
						try
						{
							data = sc.evaluateChallenge(data);
	
							if(data != null && data.length > 0)
							{
								StringBuilder b = new StringBuilder();
								b.append("DATA ");
								b.append(Hex.encodeHex(data));
								
								sendSaslCommand(b.toString());
							}
							
							if(sc.isComplete())
							{
								state = State.WAITING_FOR_OK;
							}
						}
						catch(SaslException e)
						{
							if(logger.isErrorEnabled())
							{
								logger.error("Authentication failed: " + e.getMessage());
							}
							
							sendSaslCommand("CANCEL");
						}
					}
					
					break;
					
				case WAITING_FOR_OK:
					if(false == line.startsWith("OK "))
					{
						disconnect();
						break _outer;
					}
					
					serverUuid = line.substring(3);
					
					sendSaslCommand("BEGIN");
					state = State.DONE;
					
					break _outer;
			}
		}
		
		if(state != State.DONE)
		{
			throw new IOException("Unable to authenticate with server");
		}
	}
	
	/**
	 * Attempt to send authentication request to the first mechanism in
	 * the list.
	 * 
	 * @param mechanisms
	 * 		list of available mechanisms
	 * @return
	 * @throws IOException
	 */
	private SaslClient sendAuth(List<String> mechanisms)
		throws IOException
	{
		SaslClient sc = null;
		
		while(sc == null && false == mechanisms.isEmpty())
		{
			String mechanism = mechanisms.remove(0);
			
			sc = Sasl.createSaslClient(
				new String[] { mechanism },
				"unknown", 
				"dbus",
				hostname,
				saslProperties,
				handler
			);
			
			// If mechanism isn't supported by javax.security continue onto next
			if(sc == null)
			{
				continue;
			}
			
			byte[] response = 
			    sc.hasInitialResponse() 
			    	? sc.evaluateChallenge(EMPTY_BYTE_ARRAY) 
			    	: EMPTY_BYTE_ARRAY;
			
			StringBuilder command = new StringBuilder()
				.append("AUTH ")
				.append(mechanism);
			
			if(response.length > 0)
			{
				command.append(' ');
				command.append(Hex.encodeHex(response));
			}
			
			sendSaslCommand(command.toString());
			
			return sc;
		}

		return null;
	}
	
	private void sendSaslCommand(String s)
		throws IOException
	{
		logger.debug("C: {}", s);
		
		out.write(s.getBytes());
		out.write('\r');
		out.write('\n');
		out.flush();
	}
	
	public synchronized long nextSerial()
	{
		return serial++;
	}

	public void sendMessage(Message message)
	{
		sendQueue.add(message);
	}
	
	public Message sendBlocking(Message message)
	{
		long serial = message.getSerial();
		sendMessage(message);
		
		return blocking.getReply(serial);
	}
	
	private class MessageReader
		implements Runnable
	{
		private final DBusMessenger messenger;
		private final HandlerChain chain;
		private final Channel connection;
		private final BlockingHelper blocking;
		
		public MessageReader(Channel c,
				DBusMessenger messenger,
				HandlerChain chain,
				BlockingHelper blocking)
		{
			this.connection = c;
			this.messenger = messenger;
			this.chain = chain;
			this.blocking = blocking;
		}

		public void run()
		{
			try
			{
				while(false == Thread.interrupted())
				{
					final Message msg = messenger.readMessage();
					blocking.handle(msg);
					
					executor.execute(new Runnable()
					{
						public void run()
						{
							chain.handle(msg, connection);
						}
					});
				}
			}
			catch(IOException e)
			{
				logger.error("Error while reading message; " + e.getMessage(), e);
				
				// Attempt reconnection
				reconnect();
			}
		}
	}
	
	private class MessageWriter
		implements Runnable
	{
		private final DBusMessenger messenger;
		private final BlockingQueue<Message> queue;
		
		public MessageWriter(DBusMessenger messenger, BlockingQueue<Message> queue)
		{
			this.messenger = messenger;
			this.queue = queue;
		}
		
		public void run()
		{
			while(false == Thread.interrupted())
			{
				Message msg;
				try
				{
					msg = queue.take();
					logger.debug("Sending {}", msg);
					
					messenger.writeMessage(msg);
				}
				catch(InterruptedException e)
				{
					Thread.currentThread().interrupt();
					continue;
				}
				catch(IOException e)
				{
					logger.error("Caught IOException while writing message;" + e.getMessage(), e);
					
					// Attempt reconnection
					reconnect();
				}
			}
		}
	}
	
}
