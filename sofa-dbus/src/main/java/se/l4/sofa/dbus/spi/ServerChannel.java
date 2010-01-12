package se.l4.sofa.dbus.spi;

import java.util.List;

/**
 * Channel for servers, enhanced with information about which clients are
 * connected to the server.
 * 
 * @author Andreas Holstenson
 *
 */
public interface ServerChannel
	extends Channel
{
	List<Channel> getClients();
}
