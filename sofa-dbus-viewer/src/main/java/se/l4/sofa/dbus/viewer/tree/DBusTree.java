package se.l4.sofa.dbus.viewer.tree;

import static se.l4.sofa.dbus.viewer.DBusIcons.BUS;
import static se.l4.sofa.dbus.viewer.DBusIcons.FOLDER;
import static se.l4.sofa.dbus.viewer.DBusIcons.INTERFACE;
import static se.l4.sofa.dbus.viewer.DBusIcons.METHOD;
import static se.l4.sofa.dbus.viewer.DBusIcons.SIGNAL;

import java.awt.Component;
import java.util.concurrent.Executor;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import se.l4.sofa.dbus.Connection;

public class DBusTree
	extends JTree
{
	private final Executor executor;

	public DBusTree(Executor executor)
	{
		this.executor = executor;
		
		addTreeWillExpandListener(new ExpandListener());
		setCellRenderer(new CellRenderer());
		setRootVisible(false);
		setShowsRootHandles(true);
	}
	
	public void setConnection(Connection connection)
	{
		final RootNode node = new RootNode(connection);
		DefaultTreeModel model = new DefaultTreeModel(node, true);
		node.setModel(model);
		setModel(model);
		
		executor.execute(new Runnable()
		{
			public void run()
			{
				try
				{
					node.load();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		});
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
