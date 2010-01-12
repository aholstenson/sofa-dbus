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
import se.l4.sofa.dbus.viewer.tree.ArgumentNode;
import se.l4.sofa.dbus.viewer.tree.SignalNode;

public class SignalPanel
	extends JPanel
{

	private JTextArea dbusReadable;
	private JLabel dbusNormal;
	private JLabel interfaceName;
	private JLabel signalName;
	private SignalNode signal;

	public SignalPanel()
	{
		setLayout(new MigLayout());
		
		dbusReadable = new JTextArea();
		dbusReadable.setEditable(false);
		dbusReadable.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		dbusNormal = new JLabel();
		
		interfaceName = new JLabel(DBusIcons.INTERFACE);
		signalName = new JLabel(DBusIcons.SIGNAL);
		
		JLabel dbusReadableHeader = new JLabel("DBus:");
		dbusReadableHeader.setFont(UIManager.getFont("TitledBorder.font"));
		
		add(interfaceName, "");
		add(signalName, "");

		add(dbusNormal, "wrap");
		add(dbusReadableHeader, "wrap");
		add(dbusReadable, "wrap, span");
	}
	
	public void setSignal(SignalNode signal)
	{
		this.signal = signal;
		
		interfaceName.setText(signal.getParent().toString());
		signalName.setText(signal.getName());
		
		List<ArgumentNode> args = signal.getArguments();
		dbusNormal.setText("( " + createSignature(args).getValue() + " )");
		
		dbusReadable.setText(DBusTypeGenerator.toReadableDBus(signal));
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
