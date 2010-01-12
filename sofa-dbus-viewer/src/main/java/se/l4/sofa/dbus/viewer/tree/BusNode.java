package se.l4.sofa.dbus.viewer.tree;

import java.io.StringReader;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import se.l4.sofa.dbus.Connection;
import se.l4.sofa.dbus.DBus.Introspectable;

public class BusNode
	extends RootNode
{
	private final String busName;
	private final Connection connection;

	public BusNode(Connection connection, String name)
	{
		super(connection);
		
		this.connection = connection;
		this.busName = name;
		setUserObject(name);
	}
	
	public String getBusName()
	{
		return busName;
	}
	
	public String getDBusPath()
	{
		return "/";
	}
	
	@Override
	public void load0()
		throws Exception
	{
		Introspectable introspectable = connection.get(busName, getDBusPath(), Introspectable.class);
		
		SAXBuilder builder = new SAXBuilder(false);
		
		String xml = introspectable.introspect();
		
		Pattern p = Pattern.compile("<!DOCTYPE.*?>", Pattern.MULTILINE | Pattern.DOTALL);
		xml = p.matcher(xml).replaceAll("");
		
		Document doc = builder.build(new StringReader(xml));
		
		final Element root = doc.getRootElement();
		
		SwingUtilities.invokeAndWait(new Runnable()
		{
			public void run()
			{
				String rootPath = root.getAttributeValue("name");
				if(rootPath == null)
				{
					rootPath = getDBusPath();
				}
				
				if(rootPath.endsWith("/"))
				{
					rootPath = rootPath.substring(0, rootPath.length() - 1);
				}
				
				// TODO: Emit sub nodes if not root
				
				for(Element node : (List<Element>) root.getChildren("node"))
				{
					String name = node.getAttributeValue("name");
					PathNode sub = new PathNode(connection, busName, name, rootPath + "/" + name);
					
					add(sub);
				}
				
				for(Element node : (List<Element>) root.getChildren("interface"))
				{
					InterfaceNode ifn = new InterfaceNode(node);
					add(ifn);
				}
			}
		});
	}
}
