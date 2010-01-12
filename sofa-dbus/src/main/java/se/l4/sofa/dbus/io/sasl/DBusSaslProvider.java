package se.l4.sofa.dbus.io.sasl;

import java.security.Provider;

public class DBusSaslProvider
	extends Provider
{

	public DBusSaslProvider()
	{
		super("DBus SASL", 0.1, "Provider of DBUS_COOKIE_SHA1 for SASL");
		
		put("SaslClientFactory.DBUS_COOKIE_SHA1", "se.l4.sofa.dbus.io.sasl.DBusClientFactory");
	}
	
}
