package se.l4.sofa.dbus.viewer.panel;

import java.awt.Cursor;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import se.l4.sofa.dbus.spi.Signature;
import se.l4.sofa.dbus.spi.Signature.SubSignature;
import se.l4.sofa.dbus.viewer.DBusIcons;
import se.l4.sofa.dbus.viewer.DBusTypeGenerator;
import se.l4.sofa.dbus.viewer.java.JavaGenerator;
import se.l4.sofa.dbus.viewer.tree.ArgumentNode;
import se.l4.sofa.dbus.viewer.tree.MethodNode;

public class MethodPanel
	extends JPanel
{
	private MethodNode method;
	
	private JTextArea dbusReadable;
	private JLabel dbusNormal;
	
	private JLabel interfaceName;
	private JLabel methodName;
	private JTextArea dbusJava;
	
	public MethodPanel()
	{
		setLayout(new MigLayout());
		
		dbusReadable = new JTextArea();
		dbusReadable.setEditable(false);
		dbusReadable.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		dbusNormal = new JLabel();
		dbusJava = new JTextArea();
		dbusJava.setEditable(false);
		dbusJava.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		
		interfaceName = new JLabel(DBusIcons.INTERFACE);
		methodName = new JLabel(DBusIcons.METHOD);
		
		JLabel dbusReadableHeader = new JLabel("DBus:");
		dbusReadableHeader.setFont(UIManager.getFont("TitledBorder.font"));
		
		JLabel javaHeader = new JLabel("Java:");
		javaHeader.setFont(UIManager.getFont("TitledBorder.font"));
		
		add(interfaceName, "");
		add(methodName, "");

		add(dbusNormal, "wrap");
		add(dbusReadableHeader, "wrap");
		add(dbusReadable, "wrap, span");
		add(javaHeader, "wrap, span");
		add(dbusJava, "wrap, span");
	}
	
	public void setMethod(MethodNode method)
	{
		this.method = method;

		interfaceName.setText(method.getParent().toString());
		methodName.setText(method.getName());
		
		dbusNormal.setText(generateDBus());
		dbusReadable.setText(DBusTypeGenerator.toReadableDBus(method));
		dbusJava.setText(JavaGenerator.toJavaMethod(method));
	}
	
	private String generateDBus()
	{
		StringBuilder builder = new StringBuilder();
		builder
			.append("(");
		
		List<ArgumentNode> in = method.getInArguments();
		if(false == in.isEmpty())
		{
			builder
				.append(" in: ")
				.append(createSignature(in).getValue());
		}
		
		List<ArgumentNode> out = method.getOutArguments();
		if(false == out.isEmpty())
		{
			builder
				.append(" out: ")
				.append(createSignature(out).getValue());
		}
		
		builder.append(" )");
			
		return builder.toString();
	}
	
	private Signature createSignature(List<ArgumentNode> args)
	{
		SubSignature[] inSig = new SubSignature[args.size()];
		for(int i=0, n=args.size(); i<n; i++)
		{
			inSig[i] = args.get(i).getSignatureType();
		}
		
		return Signature.from(inSig);
	}
	
}
