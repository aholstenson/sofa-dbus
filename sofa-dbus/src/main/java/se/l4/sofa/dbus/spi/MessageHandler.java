package se.l4.sofa.dbus.spi;

/**
 * Handler of DBus messages, a handler takes appropriate action for messages
 * that it can recognize.
 * 
 * @author Andreas Holstenson
 *
 */
public interface MessageHandler
{
	/**
	 * Handle the given message, taking action on messages that are recognized.
	 * There can be multiple handlers on a single connection, a handler must
	 * return {@code true} if it wishes to stop the next handler in the chain
	 * from receiving the message.
	 * 
	 * @param message
	 * 		message to handle
	 * @param connection
	 * 		connection on which message was received
	 * @return
	 * 		{@code true} if the message was handled and the chain should stop,
	 * 		otherwise {@code false}
	 */
	boolean handle(Message message, Channel connection);
}
