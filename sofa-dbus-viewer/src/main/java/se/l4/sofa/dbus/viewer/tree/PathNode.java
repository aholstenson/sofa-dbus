package se.l4.sofa.dbus.viewer.tree;

import se.l4.sofa.dbus.Connection;

public class PathNode
	extends BusNode
{
	private final String path;

	public PathNode(Connection connection, String busName, String name, String path)
	{
		super(connection, busName);
		
		setUserObject(name);
		this.path = path;
	}
	
	@Override
	public String getDBusPath()
	{
		return path;
	}
}
