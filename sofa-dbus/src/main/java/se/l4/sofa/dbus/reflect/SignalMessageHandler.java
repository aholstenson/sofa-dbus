package se.l4.sofa.dbus.reflect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.l4.sofa.dbus.DBus;
import se.l4.sofa.dbus.DBusException;
import se.l4.sofa.dbus.DBusSignal;
import se.l4.sofa.dbus.Name;
import se.l4.sofa.dbus.SignalListener;
import se.l4.sofa.dbus.io.DBusOutputStream;
import se.l4.sofa.dbus.spi.Channel;
import se.l4.sofa.dbus.spi.DBusHelper;
import se.l4.sofa.dbus.spi.Endian;
import se.l4.sofa.dbus.spi.Marshalling;
import se.l4.sofa.dbus.spi.Message;
import se.l4.sofa.dbus.spi.MessageHandler;
import se.l4.sofa.dbus.spi.ObjectPath;
import se.l4.sofa.dbus.spi.Signature;

/**
 * Handler for signal sending and receiving.
 * 
 * @author Andreas Holstenson
 *
 */
public class SignalMessageHandler
	implements MessageHandler
{
	private static final Logger logger = LoggerFactory.getLogger(SignalMessageHandler.class);
	
	private final List<SignalInfo> signals;
	private final DBus dbus;

	private final Endian endian;
	
	public SignalMessageHandler(DBus dbus, Endian endian)
	{
		this.dbus = dbus;
		this.endian = endian;
		
		signals = new CopyOnWriteArrayList<SignalInfo>();
	}
	
	/**
	 * Perform handling, will only handle messages when type is 
	 * {@link Message#TYPE_SIGNAL}.
	 */
	public boolean handle(Message message, Channel connection)
	{
		int type = message.getType();
		
		if(type == Message.TYPE_SIGNAL)
		{
			// Signal, conversion is needed
			ObjectPath path = (ObjectPath) message.getField(Message.FIELD_PATH);
			String i = (String) message.getField(Message.FIELD_INTERFACE);
			String name = (String) message.getField(Message.FIELD_MEMBER);
			
			try
			{
				Object[] data = null;
				for(SignalInfo si : signals)
				{
					if(false == si.match(path, i, name))
					{
						continue;
					}
					
					if(data == null)
					{
						List<Object> body = message.getBodyAsObjects();
						data = body.toArray();
					}
					
					try
					{
						si.trigger(data);
					}
					catch(DBusException e)
					{
						logger.error("Unable to trigger signal; " + e.getMessage(), e);
					}
				}
			}
			catch(IOException e)
			{
				logger.error("Unable to convert body of message; " + e.getMessage(), e);
			}
		}
		
		return false;
	}
	
	/**
	 * Add a new listener for a given signal without binding it to a path.
	 * 
	 * @param <T>
	 * @param signal
	 * @param listener
	 * @throws DBusException
	 */
	public <T extends DBusSignal> void addListener(Class<T> signal, SignalListener<T> listener)
		throws DBusException
	{
		addListener(null, signal, listener);
	}
	
	/**
	 * Add a new listener for a given signal with a specific path.
	 * 
	 * @param <T>
	 * @param path
	 * 		path to listen to, {@code null} for any path
	 * @param signal
	 * 		class of signal
	 * @param listener
	 * 		listener to invoke when signal is received
	 * @throws DBusException
	 * 		if unable to add listener
	 */
	public <T extends DBusSignal> void addListener(String path, Class<T> signal, SignalListener<T> listener)
		throws DBusException
	{
		synchronized(signals)
		{
			boolean added = false;
			for(SignalInfo si : signals)
			{
				if((path == null || path.equals(si.path)) && si.signal == signal)
				{
					added = true;
					si.addListener(listener);
					break;
				}
			}
			
			if(false == added)
			{
				SignalInfo si = new SignalInfo(path, signal);
				si.addListener(listener);
				signals.add(si);
				
				if(dbus != null)
				{
					dbus.addMatch(si.getMatchRule());
				}
			}
		}
	}
	
	/**
	 * Remove a listener for a given signal and path.
	 * 
	 * @param <T>
	 * @param path
	 * 		path of signal, {@code null} if any
	 * @param signal
	 * 		class of signal
	 * @param listener
	 * 		listener that should be removed
	 * @throws DBusException
	 * 		if unable to remove listener
	 */
	public <T extends DBusSignal> void removeListener(String path, Class<T> signal, SignalListener<T> listener)
		throws DBusException
	{
		synchronized(signals)
		{
			Iterator<SignalInfo> it = signals.iterator();
			while(it.hasNext())
			{
				SignalInfo si = it.next();
				
				if((path == null || path.equals(si.path)) && si.signal == signal)
				{
					si.removeListener(listener);
					
					if(si.listeners.isEmpty())
					{
						if(dbus != null)
						{
							dbus.removeMatch(si.getMatchRule());
						}
						
						it.remove();
					}
					
					break;
				}
			}
		}
	}
	
	/**
	 * Trigger any local listeners for the given signal.
	 * 
	 * @param path
	 * @param signal
	 */
	public void triggerListeners(String path, DBusSignal signal)
	{
		String i = DBusHelper.getNameForInterface(signal.getClass().getDeclaringClass());
		String name = DBusConverter.getMemberName(signal.getClass());
		ObjectPath op = new ObjectPath(path);
		
		for(SignalInfo si : signals)
		{
			if(false == si.match(op, i, name))
			{
				continue;
			}
			
			si.trigger(signal);
		}
	}
	
	/**
	 * Send a signal over the given connection.
	 * 
	 * @param connection
	 * @param path
	 * @param signal
	 * @throws DBusException
	 */
	public void send(Channel connection, String path, Object signal)
		throws DBusException
	{
		try
		{
			Class<?> c = signal.getClass();
			Signature classSig = DBusConverter.getSignatureForClass(c);
			Object[] data = DBusConverter.getDataInClass(signal);
				
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DBusOutputStream dbusOut = new DBusOutputStream(out);
			dbusOut.setEndian(endian);
			Marshalling.serialize(classSig, data, dbusOut);
				
			Message msg = new Message(
				endian, 
				Message.TYPE_SIGNAL, 
				Message.FLAG_NO_REPLY_EXPECTED, 
				connection.nextSerial(), 
				out.toByteArray()
			);
				
			Class<?> declarer = c.getDeclaringClass();
			String interfaceName = DBusHelper.getNameForInterface(declarer);
			String memberName = DBusConverter.getMemberName(c);
			
			msg.addField(Message.FIELD_SIGNATURE, classSig);
			msg.addField(Message.FIELD_INTERFACE, interfaceName);
			msg.addField(Message.FIELD_MEMBER, memberName);
			msg.addField(Message.FIELD_PATH, new ObjectPath(path));
			
			connection.sendMessage(msg);
		}
		catch(IOException e)
		{
			throw new DBusException(e);
		}
	}

	/** Inner class with information about a signals listeners */
	private static class SignalInfo
	{
		private String path;
		private String dbusInterface;
		private String name;
		private Class<?> signal;
		
		private List<SignalListener> listeners;
		
		public SignalInfo(Object path, Class<?> signal)
		{
			listeners = new LinkedList<SignalListener>();
			
			this.signal = signal;
			
			Class<?> declarer = signal.getDeclaringClass();
			if(declarer == null)
			{
				// The signal isn't declared within an interface
				throw new IllegalArgumentException("Signals must be declared within a DBusInterface");
			}
			
			dbusInterface = DBusHelper.getNameForInterface(declarer);
				
			name = signal.isAnnotationPresent(Name.class)
				? signal.getAnnotation(Name.class).value()
				: signal.getSimpleName();
		}
		
		public void addListener(SignalListener l)
		{
			listeners.add(l);
		}
		
		public void removeListener(SignalListener l)
		{
			listeners.remove(l);
		}
		
		public boolean match(ObjectPath path, String dbusInterface, String name)
		{
			if(this.path != null)
			{
				if(false == path.equals(path.getPath()))
				{
					return false;
				}
			}
			
			return this.dbusInterface.equals(dbusInterface)
				&& this.name.equals(name);
		}
		
		public void trigger(Object[] data)
			throws DBusException
		{
			Object o = DBusConverter.create(signal, data);
			if(o == null)
			{
				logger.error("Could not construct " + signal + " with " + data);
				
				return;
			}
			
			trigger((DBusSignal) o);
		}
		
		public void trigger(DBusSignal signal)
		{
			for(SignalListener l : listeners)
			{
				l.signalReceived(signal);
			}
		}
		
		public String getMatchRule()
		{
			return "type='signal',interface='" + dbusInterface + "',member='"
				+ name + "'";
		}
	}
}
