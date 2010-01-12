package se.l4.sofa.dbus.reflect;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import se.l4.sofa.dbus.DBusException;
import se.l4.sofa.dbus.DBusInterface;
import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.Signature.SubSignature;

/**
 * Class to handle introspection information for DBus services, currently
 * creates XML to describe any {@link DBusInterface}.
 * 
 * @author Andreas Holstenson
 *
 */
public class Introspection
{
	private static final String DOCTYPE = "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n" +
		"\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">\n";

	private Node root;
	private DocumentBuilderFactory factory;
	private DocumentBuilder builder;
	private Transformer transformer;
	
	public Introspection()
	{
		root = new Node("");
		
		try
		{
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			
			TransformerFactory transFactory = TransformerFactory.newInstance();
			transformer = transFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		}
		catch(ParserConfigurationException e)
		{
			throw new RuntimeException("No XML-parser could be created; " + e.getMessage(), e);
		}
		catch(TransformerConfigurationException e)
		{
			throw new RuntimeException("No XML-parser could be created; " + e.getMessage(), e);
		}
	}
	
	public String introspect(String path)
		throws DBusException
	{
		if(path.startsWith("/"))
		{
			path = path.substring(1);
		}
		
		Node n = getNode(path);
		
		Document d = builder.newDocument();
		Element el = introspect(d, path, n, true);
		d.appendChild(el);
		
		StringWriter buffer = new StringWriter();
		buffer.append(DOCTYPE);
		
		try
		{
			transformer.transform(new DOMSource(d), new StreamResult(buffer));
		}
		catch(TransformerException e)
		{
			throw new DBusException("Unable to create introspection data; " + e.getMessage(), e);
		}
		
		return buffer.toString();
	}
	
	public void addService(String path, ExportedInterface service)
	{
		Node n = addOrGetNode(path);
		n.service = service;
	}
	
	public void removeService(String path)
	{
		Node n = addOrGetNode(path);
		n.service = null;
		if(n.getChildren().isEmpty())
		{
			n.parent.removeChild(n);
		}
	}
	
	private Node addOrGetNode(String path)
	{
		if(path.startsWith("/"))
		{
			path = path.substring(1);
		}
		
		String[] parts = path.split("/");
		Node current = root;
		for(String s : parts)
		{
			boolean found = false;
			for(Node n : current.children)
			{
				if(s.equals(n.name))
				{
					current = n;
					found = true;
					break;
				}
			}
			
			if(false == found)
			{
				Node n = new Node(s);
				current.addChild(n);
				current = n;
			}
		}
		
		return current;
	}
	
	private Node getNode(String path)
	{
		String[] parts = path.split("/");
		Node current = root;
		for(String s : parts)
		{
			if("".equals(s))
			{
				break;
			}
			
			boolean found = false;
			for(Node n : current.children)
			{
				if(s.equals(n.name))
				{
					current = n;
					found = true;
					break;
				}
			}
			
			if(false == found)
			{
				return null;
			}
		}
		
		return current;
	}
	
	private Element introspect(Document doc, String parentPath, Node node, boolean first)
	{
		String ownPath = "".equals(node.name) 
			? ""
			: parentPath + "/" + node.name;
		
		Element nodeElement = doc.createElement("node");
		if(false == first)
		{
			nodeElement.setAttribute("name", node.name);
		}
		
		ExportedInterface service = node.service;
		if(service != null)
		{
			for(String interfaceName : service.getInterfaces())
			{
				Element interfaceElement = doc.createElement("interface");
				interfaceElement.setAttribute("name", interfaceName);
				
				for(DBusMethod method : service.getMethods())
				{
					String methodInterface = method.getInterfaceName();
					if(false == methodInterface.equals(interfaceName))
					{
						// Method interface and current interface does not match
						continue;
					}
					
					String name = method.getName();
					Signature request = method.getRequestSignature();
					Signature reply = method.getReturnSignature();
					Method javaMethod = method.getJavaMethod();
					
					Element methodElement = doc.createElement("method");
					interfaceElement.appendChild(methodElement);
					methodElement.setAttribute("name", name);
					
					// Add all input parameters
					for(SubSignature ss : request.getSignatures())
					{
						Element argElement = doc.createElement("arg");
						argElement.setAttribute("type", ss.toString());
						argElement.setAttribute("direction", "in");
						
						methodElement.appendChild(argElement);
					}
					
					// Add all output parameters
					for(SubSignature ss : reply.getSignatures())
					{
						Element argElement = doc.createElement("arg");
						argElement.setAttribute("type", ss.toString());
						argElement.setAttribute("direction", "out");
						
						methodElement.appendChild(argElement);
					}
					
					// Mark as deprecated if necessary
					if(javaMethod.isAnnotationPresent(Deprecated.class))
					{
						Element annotation = doc.createElement("annotation");
						annotation.setAttribute("name", "org.freedesktop.DBus.Deprecated");
						annotation.setAttribute("value", "true");
					}
				}
				
				nodeElement.appendChild(interfaceElement);
			}
		}
		
		// Go through and add all children
		for(Node child : node.children)
		{
			Element childElement = introspect(doc, ownPath, child, false);
			nodeElement.appendChild(childElement);
		}
		
		return nodeElement;
	}
	
	private static class Node
	{
		private final String name;
		private final List<Node> children;
		private ExportedInterface service;
		private Node parent;
		
		public Node(String name)
		{
			this.name = name;
			this.children = new ArrayList<Node>(10);
		}
		
		public void setService(ExportedInterface service)
		{
			this.service = service;
		}
		
		public ExportedInterface getService()
		{
			return service;
		}
		
		public List<Node> getChildren()
		{
			return children;
		}
		
		public void addChild(Node n)
		{
			children.add(n);
			n.parent = this;
		}
		
		public void removeChild(Node n)
		{
			children.remove(n);

			if(children.isEmpty() && service == null && parent != null)
			{
				// Remove ourselves when we are no longer needed in the tree
				parent.removeChild(this);
			}
			
			n.parent = null;
		}
	}
}
