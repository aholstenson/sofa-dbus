package se.l4.sofa.dbus.viewer.tree;

import static se.l4.sofa.dbus.viewer.DBusIcons.BUS;
import static se.l4.sofa.dbus.viewer.DBusIcons.FOLDER;
import static se.l4.sofa.dbus.viewer.DBusIcons.INTERFACE;
import static se.l4.sofa.dbus.viewer.DBusIcons.METHOD;
import static se.l4.sofa.dbus.viewer.DBusIcons.SIGNAL;

import java.awt.Component;
import java.util.concurrent.Executor;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import se.l4.sofa.dbus.Connection;
import se.l4.sofa.dbus.DBus;
import se.l4.sofa.dbus.SignalListener;
import se.l4.sofa.dbus.DBus.NameOwnerChanged;

public class DBusTree
	extends JTree
{
	private final Executor executor;
	private final NameListener nameListener;
	private Connection connection;

	private DefaultTreeModel model;
	
	public DBusTree(Executor executor)
	{
		this.executor = executor;
		
		nameListener = new NameListener();
		
		addTreeWillExpandListener(new ExpandListener());
		setCellRenderer(new CellRenderer());
		setRootVisible(false);
		setShowsRootHandles(true);
	}
	
	public void setConnection(Connection connection0)
	{
		final Connection oldConn = this.connection;
		this.connection = connection0;
		
		final RootNode node = new RootNode(connection);
		model = new DefaultTreeModel(node, true);
		node.setModel(model);
		setModel(model);
		
		executor.execute(new Runnable()
		{
			public void run()
			{
				try
				{
					if(oldConn != null)
					{
						oldConn.removeSignalListener(DBus.NameOwnerChanged.class, nameListener);
					}
					
					connection.addSignalListener(DBus.NameOwnerChanged.class, nameListener);
					
					node.load();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}
	
	private class NameListener
		implements SignalListener<DBus.NameOwnerChanged>
	{
		public void signalReceived(final NameOwnerChanged signal)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					RootNode root = (RootNode) getModel().getRoot();
					if("".equals(signal.getOldOwner()))
					{
						// New bus so add a new node
						BusNode node = new BusNode(connection, signal.getName());
						root.add(node);
						model.nodesWereInserted(
							root, 
							new int[] { root.getChildCount() - 1 }
						);
					}
					else
					{
						// Existing bus has left
						for(int i=0, n=root.getChildCount(); i<n; i++)
						{
							Object o = root.getChildAt(i);
							if(o instanceof BusNode)
							{
								if(((BusNode) o).getBusName().equals(signal.getName()))
								{
									root.remove(i);
									model.nodesWereRemoved(root, new int[] { i }, new Object[] { o });
									break;
								}
							}
						}
					}
					
				}
			});
		}
	}
	
	private class CellRenderer
		extends DefaultTreeCellRenderer
	{
		@Override
		public Component getTreeCellRendererComponent(
				JTree tree, 
				Object value,
				boolean sel, 
				boolean expanded, 
				boolean leaf, 
				int row,
				boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
					row, hasFocus);
			
			if(value instanceof PathNode)
			{
				setIcon(FOLDER);
			}
			else if(value instanceof BusNode)
			{
				setIcon(BUS);
			}
			else if(value instanceof InterfaceNode)
			{
				setIcon(INTERFACE);
			}
			else if(value instanceof MethodNode)
			{
				setIcon(METHOD);
			}
			else if(value instanceof SignalNode)
			{
				setIcon(SIGNAL);
			}
			
			return this;
		}
	}
	
	private class ExpandListener
		implements TreeWillExpandListener
	{
		public void treeWillExpand(TreeExpansionEvent event)
			throws ExpandVetoException
		{
			TreePath path = event.getPath();
			final Object o = path.getLastPathComponent();
			if(o instanceof RootNode)
			{
				executor.execute(new Runnable()
				{
					public void run()
					{
						try
						{
							((RootNode) o).load();
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
				});
			}
		}
		
		public void treeWillCollapse(TreeExpansionEvent event)
			throws ExpandVetoException
		{
		}
	}
}
