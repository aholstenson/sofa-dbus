package se.l4.sofa.dbus;

import java.io.IOException;
import java.security.Security;

import javax.security.auth.callback.CallbackHandler;

import se.l4.sofa.dbus.io.DBusClient;
import se.l4.sofa.dbus.io.DBusServer;
import se.l4.sofa.dbus.io.sasl.DBusSaslProvider;
import se.l4.sofa.dbus.spi.Channel;
import se.l4.sofa.dbus.spi.HandlerChain;
import se.l4.sofa.dbus.spi.Message;
import se.l4.sofa.dbus.spi.MessageHandler;

/**
 * Basic connection that does no message handling.
 * 
 * @author Andreas Holstenson
 *
 */
public class BasicConnection
{
	static
	{
		// Register our own SASL mechanisms
		Security.addProvider(new DBusSaslProvider());
	}
	
	protected static final String[] DEFAULT_MECHANISMS = {
		"EXTERNAL", "DBUS_COOKIE_SHA1"
	};
	
	private final BusAddress address;
	private final String[] saslMechanisms;
	private final CallbackHandler authentication;
	private final HandlerChain chain;
	
	private Channel connection;
	
	public BasicConnection(String address)
	{
		this(new BusAddress(address));
	}
	
	public BasicConnection(BusAddress address)
	{
		this(address, DEFAULT_MECHANISMS, null);
	}
	
	public BasicConnection(BusAddress address, String[] saslMechanisms, CallbackHandler authentication)
	{
		this.address = address;
		this.saslMechanisms = saslMechanisms;
		this.authentication = authentication;
		
		this.chain = new HandlerChain();
	}
	
	/**
	 * Add a handler to the connection, the handler will be asked to process
	 * incoming messages.
	 * 
	 * @param handler
	 */
	public void addHandler(MessageHandler handler)
	{
		chain.addHandler(handler);
	}
	
	/**
	 * Connect to the given bus address. This will either connect to a server
	 * or launch a server depending on the {@code listen} parameter of the
	 * {@link BusAddress}.
	 * 
	 * @throws IOException
	 */
	public void connect()
		throws IOException
	{
		boolean listen = address.getBooleanParameter("listen");
		
		if(listen)
		{
			DBusServer server = new DBusServer(address, chain, saslMechanisms, authentication);
			server.start();
			
			connection = server;
		}
		else
		{
			DBusClient client = new DBusClient(address, chain, saslMechanisms, authentication);
			client.connect();
			
			connection = client;
		}
	}
	
	/**
	 * Get if the connection is still active.
	 * 
	 * @return
	 */
	public boolean isConnected()
	{
		return connection != null && connection.isConnected();
	}
	
	/**
	 * Get a channel for the connection.
	 * 
	 * @return
	 */
	public Channel getConnection()
	{
		return connection;
	}
	
	/**
	 * Get the next serial to use for message sending.
	 * 
	 * @return
	 */
	public long nextSerial()
	{
		return connection.nextSerial();
	}
	
	/**
	 * Send a message.
	 * 
	 * @param message
	 */
	public void sendMessage(Message message)
	{
		connection.sendMessage(message);
	}
	
}
