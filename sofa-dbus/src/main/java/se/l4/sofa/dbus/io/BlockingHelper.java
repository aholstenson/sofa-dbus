package se.l4.sofa.dbus.io;

import java.util.HashMap;
import java.util.Map;

import se.l4.sofa.dbus.Holder;
import se.l4.sofa.dbus.spi.Message;

/**
 * Helper class for blocking send/receive. This class is used so that we can
 * block a thread until a reply to the given message has been received, the
 * transport layer is still asynchronous.
 * 
 * @author Andreas Holstenson
 *
 */
public class BlockingHelper
{
	private final Map<Long, Holder<Message>> locks;
	
	public BlockingHelper()
	{
		locks = new HashMap<Long, Holder<Message>>();
	}
	
	public boolean handle(Message message)
	{
		Object serialObject = message.getField(Message.FIELD_REPLY_SERIAL);
		
		if(serialObject != null && serialObject instanceof Number)
		{
			long serial = ((Number) serialObject).longValue();
			
			synchronized(locks)
			{
				Holder<Message> h = locks.get(serial);
				
				if(h == null)
				{
					h = new Holder<Message>();
					locks.put(serial, h);
				}
				
				synchronized(h)
				{
					h.setValue(message);
					h.notifyAll();
				}
			}
		}
		
		return false;
	}
	
	public Message getReply(long serial)
	{
		Holder<Message> h;
		synchronized(locks)
		{
			h = locks.get(serial);
			
			if(h == null)
			{
				h = new Holder<Message>();
				locks.put(serial, h);
			}
		}
		
		synchronized(h)
		{
			while(false == Thread.interrupted()
				&& h.getValue() == null)
			{
				try
				{
					h.wait();
				}
				catch(InterruptedException e)
				{
					Thread.currentThread().interrupt();
					return null;
				}
			}
			
			synchronized(locks)
			{
				locks.remove(serial);
			}
			
			return h.getValue();
		}
	}
}
