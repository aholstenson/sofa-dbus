package se.l4.sofa.dbus.viewer.tree;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jdom.Element;

public class InterfaceNode
	extends DefaultMutableTreeNode
{
	public InterfaceNode(Element interfaceElement)
	{
		String name = interfaceElement.getAttributeValue("name");
		setUserObject(name);
		
		for(Element e : (List<Element>) interfaceElement.getChildren("signal"))
		{
			add(new SignalNode(e));
		}
		
		for(Element e : (List<Element>) interfaceElement.getChildren("method"))
		{
			add(new MethodNode(e));
		}
	}
}
