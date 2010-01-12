package se.l4.sofa.dbus.spi;

public interface Channel
{
	/**
	 * Send the given message asynchronously.
	 * 
	 * @param message
	 */
	void sendMessage(Message message);
	
	/**
	 * Send a message and wait for it's reply. Do not use this method for
	 * messages that are flagged {@link Message#FLAG_NO_REPLY_EXPECTED}.
	 * Replies are based on the serial of the sent message.
	 * 
	 * @param message
	 * 		message to send
	 * @return
	 * 		reply to message
	 */
	Message sendBlocking(Message message);
	
	/**
	 * Retrieve the next serial to use with messages.
	 * 
	 * @return
	 */
	long nextSerial();
	
	/**
	 * Get if this channel is connected.
	 * 
	 * @return
	 */
	boolean isConnected();
}
