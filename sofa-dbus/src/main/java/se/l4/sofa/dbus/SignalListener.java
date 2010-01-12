package se.l4.sofa.dbus;

/**
 * Listener that is triggered when a signal is received.
 * 
 * @author Andreas Holstenson
 *
 * @param <T>
 */
public interface SignalListener<T extends DBusSignal>
{
	/**
	 * Signal has been received and should be handled.
	 * 
	 * @param signal
	 */
	void signalReceived(T signal);
}
