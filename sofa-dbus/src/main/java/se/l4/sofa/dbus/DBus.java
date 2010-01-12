package se.l4.sofa.dbus;


/**
 * Standard DBus interface for interacting with the message bus.
 * 
 * @author Andreas Holstenson
 *
 */
@Name("org.freedesktop.DBus")
public interface DBus
	extends DBusInterface
{
	/**
	 * Signal sent when the bus connection acquires a new name.
	 */
	class NameAcquired implements DBusSignal
	{
		@StructPosition(0)
		private final String name;
		
		public NameAcquired(String name)
		{
			this.name = name;
		}
		
		public String getName()
		{
			return name;
		}
		
		@Override
		public String toString()
		{
			return "NameAcquired[" + name + "]";
		}
	}
	
	/** 
	 * Signal sent when the bus connection loses a name. 
	 */
	class NameLost implements DBusSignal
	{
		@StructPosition(0)
		private final String name;
		
		public NameLost(String name)
		{
			this.name = name;
		}
		
		public String getName()
		{
			return name;
		}
		
		@Override
		public String toString()
		{
			return "NameLost[" + name + "]";
		}
	}
	
	/** 
	 * Signal sent on a name change from any client connected to the bus 
	 */
	class NameOwnerChanged implements DBusSignal
	{
		@StructPosition(0)
		private final String name;
		@StructPosition(1)
		private final String oldOwner;
		@StructPosition(2)
		private final String newOwner;
		
		public NameOwnerChanged(String name, String oldOwner, String newOwner)
		{
			this.name = name;
			this.oldOwner = oldOwner;
			this.newOwner = newOwner;
		}
		
		public String getName()
		{
			return name;
		}
		
		public String getNewOwner()
		{
			return newOwner;
		}
		
		public String getOldOwner()
		{
			return oldOwner;
		}
		
		@Override
		public String toString()
		{
			return "NameOwnerChanged[name=" + name + ", oldOwner=" + oldOwner
				+ ", newOwner=" + newOwner + "]";
		}
	}
	
	/**
	 * Result of a request to acquire a new name.
	 */
	enum RequestNameResult
	{
		PRIMARY_OWNER,
		REPLY_IN_QUEUE,
		REPLY_EXISTS,
		REPLY_ALREADY_OWNER
	}
	
	/**
	 * Result of a request to release a name.
	 */
	enum ReleaseNameResult
	{
		RELEASED,
		NON_EXISTENT,
		NOT_OWNER
	}
	
	enum StartServiceResult
	{
		SUCCESS,
		ALREADY_RUNNING
	}
	
	/**
	 * Marks those interfaces that support introspection, allowing us to
	 * explore how services on the bus look like.
	 * 
	 * @author Andreas Holstenson
	 *
	 */
	interface Introspectable
		extends DBusInterface
	{
		/**
		 * Introspect this service, returning XML describing which interfaces,
		 * methods and signals that are exposed over the bus.
		 * 
		 * @return
		 * 		XML as string
		 * @throws DBusException
		 */
		@Name("Introspect")
		String introspect()
			throws DBusException;
	}
	
	@Name("Hello")
	String hello()
		throws DBusException;

	@Name("ListNames")
	String[] listNames()
		throws DBusException;
	
	@Name("RequestName")
	@Out(DType.UINT32)
	RequestNameResult requestName(String name, @In(DType.UINT32) long flags)
		throws DBusException;
	
	@Name("ReleaseName")
	@Out(DType.UINT32)
	ReleaseNameResult releaseName(String name)
		throws DBusException;
	
	@Name("NameHasOwner")
	boolean nameHasOwner(String name)
		throws DBusException;
	
	@Name("GetNameOwner")
	String getNameOwner(String name)
		throws NameHasNoOwnerException, DBusException;
	
	@Name("AddMatch")
	void addMatch(String rule)
		throws DBusException;
	
	@Name("RemoveMatch")
	void removeMatch(String rule)
		throws DBusException;

	@Name("ListActivatableNames")
	String[] listActivatableNames()
		throws DBusException;
	
	@Name("StartServiceByName")
	@Out(DType.UINT32)
	StartServiceResult startServiceByName(String name, @In(DType.UINT32) long flags)
		throws DBusException;
	
	@Name("GetConnectionUnixUser")
	@Out(DType.UINT32)
	long getConnectionUnixUser(String connectionName)
		throws DBusException;
	
	@Name("GetId")
	String getId()
		throws DBusException;

	@Name("Error.NameHasNoOwner")
	class NameHasNoOwnerException extends DBusException
	{
		public NameHasNoOwnerException(String message)
		{
			super(message);
		}
	}
}
