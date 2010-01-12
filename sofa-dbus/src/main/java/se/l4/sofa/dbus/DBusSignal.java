package se.l4.sofa.dbus;

/**
 * Marker interface for classes that are DBus signals. Signals require that
 * its fields are annotated with {@link StructPosition}.
 * 
 * <p>
 * Example:
 * <pre>
 * class TestSignal implements DBusSignal {
 * 	{@literal StructPosition(0)}
 * 	private String name;
 * 
 * 	// constructor and getters/setters
 * }
 * </pre>
 * 
 * @author Andreas Holstenson
 *
 */
public interface DBusSignal
{

}
