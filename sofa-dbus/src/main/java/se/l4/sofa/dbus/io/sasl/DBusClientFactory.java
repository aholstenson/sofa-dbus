package se.l4.sofa.dbus.io.sasl;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

/**
 * Factory for custom DBus SASL-clients.
 * 
 * @author Andreas Holstenson
 *
 */
public class DBusClientFactory
	implements SaslClientFactory
{
	public SaslClient createSaslClient(String[] mechanisms,
			String authorizationId, String protocol, String serverName,
			Map<String, ?> props, CallbackHandler cbh)
			throws SaslException
	{
		for(String s : mechanisms)
		{
			if(DBusCookieSha1.NAME.equals(s))
			{
				return new DBusCookieSha1();
			}
		}
		
		return null;
	}

	public String[] getMechanismNames(Map<String, ?> props)
	{
		return new String[] { DBusCookieSha1.NAME };
	}
}