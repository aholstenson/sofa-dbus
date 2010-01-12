package se.l4.sofa.dbus.viewer.tree;

import org.jdom.Element;

import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.Signature.SubSignature;

public class ArgumentNode
{
	public enum Direction
	{
		IN,
		OUT,
		UNKNOWN
	}
	
	private final String name;
	private final Direction direction;
	private final Signature type;
	
	public ArgumentNode(Element arg)
	{
		this.name = arg.getAttributeValue("name");
		
		String dir = arg.getAttributeValue("direction");
		direction = dir == null
			? Direction.UNKNOWN
			: dir.equalsIgnoreCase("in")
				? Direction.IN
				: dir.equalsIgnoreCase("out")
					? Direction.OUT
					: Direction.UNKNOWN;
		
		type = Signature.parse(arg.getAttributeValue("type"));
	}
	
	public Direction getDirection()
	{
		return direction;
	}
	
	public String getName()
	{
		return name;
	}
	
	public Signature getType()
	{
		return type;
	}
	
	public SubSignature getSignatureType()
	{
		return type.getSignatures()[0];
	}
}
