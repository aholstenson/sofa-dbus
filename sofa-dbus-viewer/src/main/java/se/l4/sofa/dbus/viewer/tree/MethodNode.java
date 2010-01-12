package se.l4.sofa.dbus.viewer.tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jdom.Element;

import se.l4.sofa.dbus.viewer.tree.ArgumentNode.Direction;

public class MethodNode
	extends DefaultMutableTreeNode
{
	private List<ArgumentNode> arguments;
	
	public MethodNode(Element method)
	{
		setUserObject(method.getAttributeValue("name"));
		setAllowsChildren(false);
		
		arguments = new LinkedList<ArgumentNode>();
		for(Element e : (List<Element>) method.getChildren("arg"))
		{
			arguments.add(new ArgumentNode(e));
		}
	}
	
	public String getName()
	{
		return (String) getUserObject();
	}
	
	public List<ArgumentNode> getArguments()
	{
		return arguments;
	}
	
	public List<ArgumentNode> getInArguments()
	{
		List<ArgumentNode> result = new ArrayList<ArgumentNode>();
		for(ArgumentNode n : arguments)
		{
			if(n.getDirection() == Direction.IN)
			{
				result.add(n);
			}
		}
		
		return result;
	}
	
	public List<ArgumentNode> getOutArguments()
	{
		List<ArgumentNode> result = new ArrayList<ArgumentNode>();
		for(ArgumentNode n : arguments)
		{
			if(n.getDirection() == Direction.OUT)
			{
				result.add(n);
			}
		}
		
		return result;
	}
}
