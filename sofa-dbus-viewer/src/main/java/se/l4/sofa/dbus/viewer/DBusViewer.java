package se.l4.sofa.dbus.viewer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import com.jgoodies.looks.plastic.PlasticLookAndFeel;

import se.l4.sofa.dbus.BusConnection;
import se.l4.sofa.dbus.Connection;
import se.l4.sofa.dbus.viewer.panel.MethodPanel;
import se.l4.sofa.dbus.viewer.panel.SignalPanel;
import se.l4.sofa.dbus.viewer.tree.DBusTree;
import se.l4.sofa.dbus.viewer.tree.MethodNode;
import se.l4.sofa.dbus.viewer.tree.SignalNode;

public class DBusViewer
	extends JPanel
{
	private static final String METHOD = "METHOD";
	private static final String SIGNAL = "SIGNAL";
	private static final String EMPTY = "EMPTY";
	
	private final Connection connection;
	private final DBusTree tree;
	private final Executor executor;
	
	private final JPanel panels;
	private CardLayout panelLayout;
	private MethodPanel methodPanel;
	private SignalPanel signalPanel;
	
	public DBusViewer(Executor executor0, Connection connection)
	{
		this.executor = executor0;
		this.connection = connection;
		
		tree = new DBusTree(executor0);
		tree.setConnection(connection);
		
		setLayout(new BorderLayout());
		add(new JScrollPane(tree));
		
		panels = new JPanel();
		panelLayout = new CardLayout();
		panels.setLayout(panelLayout);
		panels.setPreferredSize(new Dimension(100, 150));
		
		add(panels, BorderLayout.SOUTH);
		
		
		// Add available panels
		methodPanel = new MethodPanel();
		signalPanel = new SignalPanel();
		
		panels.add(new JPanel(), EMPTY);
		panels.add(methodPanel, METHOD);
		panels.add(signalPanel, SIGNAL);
		
		// Selection listener
		tree.addTreeSelectionListener(new TreeSelectionListener()
		{
			public void valueChanged(TreeSelectionEvent e)
			{
				Object node = e.getPath().getLastPathComponent();
				if(node instanceof MethodNode)
				{
					methodPanel.setMethod((MethodNode) node);
					panelLayout.show(panels, METHOD);
				}
				else if(node instanceof SignalNode)
				{
					signalPanel.setSignal((SignalNode) node);
					panelLayout.show(panels, SIGNAL);
				}
				else
				{
					panelLayout.show(panels, EMPTY);
				}
			}
		});
	}
	
	public static void main(String[] args) throws Exception
	{
		String antialiasing = "swing.aatext";
		if(null == System.getProperty(antialiasing))
		{
			System.setProperty(antialiasing, "true");
		}
		UIManager.setLookAndFeel(new PlasticLookAndFeel());
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Connection c = BusConnection.session();
		
		DBusViewer viewer = new DBusViewer(Executors.newSingleThreadExecutor(), c);
		
		frame.add(viewer);
		frame.setSize(1000, 800);
		frame.setVisible(true);
	}
}
