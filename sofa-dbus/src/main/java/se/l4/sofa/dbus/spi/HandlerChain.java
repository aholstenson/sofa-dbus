package se.l4.sofa.dbus.spi;

/**
 * A chain of handlers for messages received over a DBus connection, forwards
 * messages to {@link MessageHandler}. Add handlers via {@link #addHandler(MessageHandler)}
 * and use {@link #handle(Message, Channel)} to let the chain handle a
 * message.
 * 
 * @author Andreas Holstenson
 *
 */
public class HandlerChain
{
	private MessageHandler[] handlers;
	
	public HandlerChain()
	{
		handlers = new MessageHandler[0];
	}
	
	/**
	 * Add a handler to the chain.
	 * 
	 * @param handler
	 */
	public void addHandler(MessageHandler handler)
	{
		MessageHandler[] temp = new MessageHandler[handlers.length + 1];
		System.arraycopy(handlers, 0, temp, 0, handlers.length);
		temp[handlers.length] = handler;
		
		handlers = temp;
	}
	
	/**
	 * Let the chain handle a message by forwarding it to each registered
	 * handler. When a handler returns {@code true} processing is cancelled.
	 * 
	 * @param msg
	 * @param connection
	 */
	public void handle(Message msg, Channel connection)
	{
		for(MessageHandler handler : handlers)
		{
			if(handler.handle(msg, connection))
			{
				return;
			}
		}
	}
}
