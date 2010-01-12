package se.l4.sofa.dbus.viewer.tree;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import se.l4.sofa.dbus.Connection;

public class RootNode
	extends DefaultMutableTreeNode
{
	protected final Connection connection;
	protected DefaultTreeModel model;
	
	private boolean loaded;
	
	public RootNode(Connection connection)
	{
		this.connection = connection;
		
		setUserObject("/");
		setAllowsChildren(true);
	}
	
	public void setModel(DefaultTreeModel model)
	{
		this.model = model;
	}
	
	@Override
	public void add(MutableTreeNode newChild)
	{
		super.add(newChild);
		
		if(newChild instanceof RootNode)
		{
			((RootNode) newChild).setModel(model);
		}
	}
	
	public final void load()
		throws Exception
	{
		if(false == loaded)
		{
			load0();
			loaded = true;
			
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					model.nodeStructureChanged(RootNode.this);
				}
			});
		}
	}
	
	protected void load0()
		throws Exception
	{
		final String[] names = connection.getDBus().listNames();
		
		SwingUtilities.invokeAndWait(new Runnable()
		{
			public void run()
			{
				for(String name : names)
				{
					add(new BusNode(connection, name));
				}
			}
		});
	}
}
