package se.l4.sofa.dbus.viewer.tree;

import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jdom.Element;

public class SignalNode
	extends DefaultMutableTreeNode
{
	private final List<ArgumentNode> arguments;
	
	public SignalNode(Element method)
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
}
