package se.l4.sofa.dbus;

import java.util.HashMap;
import java.util.Map;

/**
 * Address for a DBus connection, has a protocol and zero or more parameters.
 * 
 * Example of addresses:
 * 
 * <ul>
 * 	<li>
 * 		{@code unix:abstract=/tmp/dbus-YJifTziglB} - connection to a Unix socket
 *	</li>
 *	<li>
 * 		{@code tcp:host=localhost,port=4000} - TCP connection
 * 	</li>
 * 	<li>
 * 		{@code tcp:host=localhost,port=4000,listen=true} - connection with TCP,
 * 		acting as a server
 * 	</li>
 * </ul>
 * 
 * @author Andreas Holstenson
 *
 */
public class BusAddress
{
	private final String protocol;
	private final Map<String, String> parameters;
	
	public BusAddress(String address)
	{
		parameters = new HashMap<String, String>();
		
		int idx = address.indexOf(':');
		if(idx < 0)
		{
			// Assuming that only protocol was given
			//throw new IllegalArgumentException("Invalid DBus address");
			protocol = address;
			return;
		}

		// Parse the string
		protocol = address.substring(0, idx);
		
		if(address.length() > idx+1)
		{
			String[] split = address.substring(idx + 1).split(",");
			for(String arg : split)
			{
				idx = arg.indexOf('=');
				if(idx > 0)
				{
					parameters.put(
						arg.substring(0, idx),
						arg.substring(idx + 1)
					);
				}
				else
				{
					parameters.put(arg, "true");
				}
			}
		}
	}
	
	/**
	 * Get the protocol of the address.
	 * 
	 * @return
	 */
	public String getProtocol()
	{
		return protocol;
	}
	
	/**
	 * Set the value of a given parameter.
	 * 
	 * @param key
	 * @param value
	 */
	public void setParameter(String key, String value)
	{
		parameters.put(key, value);
	}
	
	/**
	 * Check if the address has the given parameter.
	 * 
	 * @param key
	 * @return
	 */
	public boolean hasParameter(String key)
	{
		return parameters.containsKey(key);
	}
	
	/**
	 * Get the string value of the given parameter. Will return {@code null}
	 * if the parameter is not set.
	 * 
	 * @param key
	 * @return
	 */
	public String getParameter(String key)
	{
		return parameters.get(key);
	}
	
	/**
	 * Get the given parameter as an integer. If the parameter is not set or
	 * if it can't be converted {@code 0} will be returned.
	 * 
	 * @param key
	 * @return
	 */
	public int getIntParameter(String key)
	{
		String value = parameters.get(key);
		if(value != null)
		{
			try
			{
				return Integer.parseInt(value);
			}
			catch(NumberFormatException e)
			{
			}
		}
		
		return 0;
	}
	
	/**
	 * Get the given parameter as a boolean. 
	 * 
	 * @param key
	 * @return
	 * 		{@code true} if the parameter has the String-value {@code true},
	 * 		otherwise {@code false}
	 */
	public boolean getBooleanParameter(String key)
	{
		return "true".equals(parameters.get(key));
	}
}
