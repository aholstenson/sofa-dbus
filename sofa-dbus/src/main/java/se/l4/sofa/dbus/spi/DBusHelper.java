package se.l4.sofa.dbus.spi;

import se.l4.sofa.dbus.DBusInterface;
import se.l4.sofa.dbus.Name;

/**
 * Helper utilities for working with DBus connections.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusHelper
{
	private static final String INTERFACE_NAME 
		= "[A-Za-z_][A-Za-z0-9_]*\\.([A-Za-z_][A-Za-z0-9_]*\\.)*[A-Za-z_][A-Za-z0-9_]*";
	
	private DBusHelper()
	{
	}
	
	/**
	 * Get the name of a class treating it as a DBus interface. The rules
	 * are as follows:
	 * 
	 * <ol>
	 * 	<li>
	 * 		If the class is declared within another class the name is
	 * 		resolved relative to the name of the declaring class. The
	 * 		interface {@code SubMethods} within {@code test.Methods} will have
	 * 		the name {@code test.Methods.SubMethods}
	 * 	</li>
	 * 	<li>
	 * 		If the class has a {@link Name} annotation the value of that 
	 * 		annotation is used (appending it to any declaring class, see #1)
	 * 	</li>
	 * 	<li>
	 * 		If there is no annotation the full class name (including package)
	 * 		will be used as the name.
	 * 	</li>
	 * </ol>
	 * 
	 * @param d
	 * @return
	 */
	public static String getNameForInterface(Class<?> d)
	{
		Class<?> declarer = d.getDeclaringClass();
		if(declarer != null && DBusInterface.class.isAssignableFrom(declarer))
		{
			String parentName = getNameForInterface(declarer);
			
			return parentName + "." +
				(d.isAnnotationPresent(Name.class)
					? d.getAnnotation(Name.class).value()
					: d.getSimpleName());
		}
		else
		{
			String name = d.isAnnotationPresent(Name.class)
				? d.getAnnotation(Name.class).value()
				: d.getName();
				
			if(false == name.matches(INTERFACE_NAME))
			{
				throw new IllegalArgumentException(
					"Invalid DBus interface name. Must have at least one " +
					"package (test.Methods is ok, but Methods is not) " +
					"and elements may only contain [A-Z][a-z][0-9]_ and can " +
					"not begin with a digit; Name was: " + name);
			}
			
			return name;
		}
	}
	
}
