package se.l4.sofa.dbus.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.sofa.dbus.BusAddress;
import se.l4.sofa.dbus.spi.Channel;
import se.l4.sofa.dbus.spi.HandlerChain;
import se.l4.sofa.dbus.spi.Message;

/**
 * Simple server implementation that just starts a single thread for receiving
 * incoming clients. After that it forks of client threads for handling.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusServer
	implements Runnable, Channel
{
	private enum State
	{
		WAITING_FOR_AUTH,
		NEGOTIATE_MECHANISM,
		WAITING_FOR_BEGIN
	}
	
	private static final Logger logger = LoggerFactory.getLogger(DBusServer.class);
	
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
	
	private final String[] mechanisms;
	private final CallbackHandler handler;
	private final BusAddress addr;
	private final String uuid;
	private final HandlerChain chain;
	
	private Thread ownThread;
	
	private Map<String, Object> saslProperties;
	
	private long serial;
	
	private List<Channel> clients;
	
	public DBusServer(BusAddress addr, HandlerChain chain,
			String[] mechanisms, CallbackHandler handler)
	{
		this.addr = addr;
		this.mechanisms = mechanisms;
		this.handler = handler;
		this.chain = chain;
	
		// Generate a UUID of the server
		uuid = UUID.randomUUID().toString().replace("-", "");
		
		saslProperties = new HashMap<String, Object>();
		
		serial = 1;
		
		clients = new LinkedList<Channel>();
	}
	
	public boolean isConnected()
	{
		return ownThread != null;
	}
	
	public void start()
	{
		ownThread = new Thread(this, "dbus-server");
		ownThread.start();
	}
	
	public void run()
	{
		String proto = addr.getProtocol();
		if("tcp".equals(proto))
		{
			runTCP();
		}
		else
		{
			throw new IllegalArgumentException("Unsupported transport " + proto);
		}
	}
	
	public void runTCP()
	{
		String hostname = addr.getParameter("host");
		int port = addr.getIntParameter("port");
		
		if(port < 0 || port >= 65535)
		{
			throw new IllegalArgumentException("No valid port specified");
		}
		
		try
		{
			ServerSocket socket = new ServerSocket();
			if(hostname != null)
			{
				socket.bind(new InetSocketAddress(hostname, port));
			}
			else
			{
				socket.bind(new InetSocketAddress(port));
			}
			
			while(false == Thread.interrupted())
			{
				try
				{
					Socket client = socket.accept();
					
					logger.info("New client {}", client.getInetAddress());
					
					ClientHandler handler 
						= new ClientHandler(client, chain, mechanisms);
					
					clients.add(handler);
					
					handler.start();
				}
				catch(IOException e)
				{
					logger.error("Could not accept incoming connetion; " + e.getMessage(), e);
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void close()
		throws IOException
	{
		if(ownThread != null)
		{
			ownThread.interrupt();
		}
	}
	
	private void clientDisconnected(Channel c)
	{
		clients.remove(c);
	}
	
	public List<Channel> getClients()
	{
		return clients;
	}
	
	public void sendMessage(Message message)
	{
		for(Channel c : clients)
		{
			c.sendMessage(message);
		}
	}
	
	public Message sendBlocking(Message message)
	{
		throw new IllegalStateException("DBus server does not support sending blocking messages to all clients");
	}
	
	public long nextSerial()
	{
		return serial++;
	}
	
	private class ClientHandler
		implements Channel, Runnable
	{
		private Socket socket;
		private final String[] mechanisms;
		private final HandlerChain chain;
		
		private final BlockingQueue<Message> sendQueue;
		private final BlockingHelper blocking;
		
		private InputStream in;
		private OutputStream out;
		private long serial;
		
		private Thread writerThread;
		private Thread readerThread;
		
		public ClientHandler(Socket socket, HandlerChain chain, String[] mechanisms)
		{
			this.socket = socket;
			this.mechanisms = mechanisms;
			this.chain = chain;
			sendQueue = new LinkedBlockingQueue<Message>();
			
			this.blocking = new BlockingHelper();
			
			// Our serials start at 1
			serial = 1;
		}
		
		public boolean isConnected()
		{
			return socket != null;
		}
		
		public void start()
		{
			readerThread = new Thread(this, "dbus-reader [" + socket + "]");
			readerThread.start();
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
		
		public void disconnect()
		{
			try
			{
				socket.close();
			}
			catch(IOException e)
			{
			}
			
			writerThread.interrupt();
			readerThread.interrupt();
			
			socket = null;
			
			// Notify server we have disconnected
			clientDisconnected(this);
		}
		
		public void run()
		{
			try
			{
				// First step is to negotiate
				negotiate();

				// If negotation worked, start a write thread
				DBusMessenger messenger = new DBusMessenger(in, out);
				
				MessageWriter writer = new MessageWriter(messenger, sendQueue);
				writerThread = new Thread(writer, "dbus-sender [" + socket + "]");
				writerThread.start();
				
				// This thread is now the read thread
				while(false == Thread.interrupted())
				{
					Message msg = messenger.readMessage();
					blocking.handle(msg);
					
					chain.handle(msg, ClientHandler.this);
				}
			}
			catch(IOException e)
			{
				logger.error("Caught IOException while reading message; " + e.getMessage(), e);
				
				disconnect();
			}
		}
		
		public synchronized long nextSerial()
		{
			return serial++;
		}
		
		private void negotiate()
			throws IOException
		{
			this.in = socket.getInputStream();
			this.out = socket.getOutputStream();
			
			StringBuilder temp = new StringBuilder();
			boolean first = true;
			for(String s : mechanisms)
			{
				if(first)
				{
					first = false;
				}
				else
				{
					temp.append(' ');
				}
				
				temp.append(s);
			}
			
			String combinedMechanisms = temp.toString();
			
			State state = State.WAITING_FOR_AUTH;
			
			int nul = in.read();
			if(nul != 0)
			{
				// Disconnect if first byte is not NUL
				socket.close();
				return;
			}
			
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(in)
			);
			
			SaslServer sasl = null;
			
			String[] data;
			byte[] response;
			
			String line;
			_outer:
			while((line = reader.readLine()) != null)
			{
				logger.debug("C: {}", line);
				logger.trace("State {}", state);
				
				switch(state)
				{
					case WAITING_FOR_AUTH:
						data = line.split(" ");
						if(false == "AUTH".equals(data[0]))
						{
							sendSaslCommand("ERROR");
							continue;
						}
						
						if(data.length == 1 || false == validMechanism(data[1]))
						{
							sendSaslCommand("REJECTED " + combinedMechanisms);
							continue;
						}
						
						// Attempt to authenticate
						sasl = Sasl.createSaslServer(
							data[1], 
							"dbus", 
							"", 
							saslProperties, 
							handler
						);
						
						if(sasl == null)
						{
							sendSaslCommand("REJECTED " + combinedMechanisms);
							continue;
						}
						
						
						response = data.length > 2
							? Hex.decodeHex(data[2].toCharArray())
							: EMPTY_BYTE_ARRAY;
						
						try
						{
							response = sasl.evaluateResponse(response);
							if(sasl.isComplete())
							{
								sendSaslCommand("OK " + uuid);
								state = State.WAITING_FOR_BEGIN;
							}
							else
							{
								if(response != null && response.length > 0)
								{
									StringBuilder b = new StringBuilder();
									b.append("DATA ");
									b.append(Hex.encodeHex(response));
								
									sendSaslCommand(b.toString());
								}
								
								state = State.NEGOTIATE_MECHANISM;
							}
						}
						catch(SaslException e)
						{
							sendSaslCommand("ERROR");
						}
						
						break;
						
					case NEGOTIATE_MECHANISM:
						data = line.split(" ");
						if("CANCEL".equals(data[0]))
						{
							sendSaslCommand("REJECTED");
							state = State.WAITING_FOR_AUTH;
							continue;
						}
						else if(false == "DATA".equals(data[0]))
						{
							sendSaslCommand("ERROR");
							continue;
						}
						
						response = data.length > 1
							? Hex.decodeHex(data[1].toCharArray())
							: EMPTY_BYTE_ARRAY;
							
						try
						{
							response = sasl.evaluateResponse(response);
							if(sasl.isComplete())
							{
								sendSaslCommand("OK " + uuid);
								state = State.WAITING_FOR_BEGIN;
							}
							else
							{
								StringBuilder b = new StringBuilder();
								b.append("DATA ");
								b.append(Hex.encodeHex(response));
								
								sendSaslCommand(b.toString());
							}
						}
						catch(SaslException e)
						{
							if(logger.isErrorEnabled())
							{
								logger.error("SASL error " + e.getMessage(), e);
							}
							
							sendSaslCommand("REJECTED");
						}
					
						break;
						
					case WAITING_FOR_BEGIN:
						
						if(line.equals("BEGIN"))
						{
							break _outer;
						}
						else if(line.equals("CANCEL"))
						{
							sendSaslCommand("REJECTED");
							state = State.WAITING_FOR_AUTH;
						}
						else
						{
							sendSaslCommand("ERROR");
						}
						
						break;
				}
			}
		}
		
		private void sendSaslCommand(String s)
			throws IOException
		{
			logger.debug("S: {}", s);
			
			out.write(s.getBytes());
			out.write('\r');
			out.write('\n');
			out.flush();
		}
		
		private boolean validMechanism(String mechanism)
		{
			for(String mech : mechanisms)
			{
				if(mechanism.equals(mech))
				{
					return true;
				}
			}
			
			return false;
		}
	}
	
	private static class MessageWriter
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
				}
			}
		}
	}
	
}
