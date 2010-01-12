package se.l4.sofa.dbus.viewer;

import java.util.List;

import se.l4.sofa.dbus.spi.Signature.SubSignature;
import se.l4.sofa.dbus.viewer.tree.ArgumentNode;
import se.l4.sofa.dbus.viewer.tree.MethodNode;
import se.l4.sofa.dbus.viewer.tree.SignalNode;

public class DBusTypeGenerator
{
	private DBusTypeGenerator()
	{
	}
	
	public static String toReadableDBus(MethodNode method)
	{
		List<ArgumentNode> in = method.getInArguments();
		List<ArgumentNode> out = method.getOutArguments();
		
		StringBuilder builder = new StringBuilder();
		if(false == out.isEmpty())
		{
			ArgumentNode arg = out.get(0);
			
			DBusTypeGenerator.signatureToDBus(builder, arg.getSignatureType());
			builder.append(" ");
		}
		
		builder.append(method.getName())
			.append(" (");
		
		boolean first = true;
			
		for(int i=0, n=in.size(); i<n; i++)
		{
			if(false == first)
			{
				builder.append(", ");
			}
			else
			{
				first = false;
			}
			
			builder.append("in ");
			
			ArgumentNode arg = in.get(i);
			DBusTypeGenerator.signatureToDBus(builder, arg.getSignatureType());
			
			if(arg.getName() != null)
			{
				builder
					.append(" ")
					.append(arg.getName());
			}
		}
		
		for(int i=1, n=out.size(); i<n; i++)
		{
			if(false == first)
			{
				builder.append(", ");
			}
			else
			{
				first = false;
			}
			
			builder.append("out ");
			
			ArgumentNode arg = out.get(i);
			DBusTypeGenerator.signatureToDBus(builder, arg.getSignatureType());
			
			if(arg.getName() != null)
			{
				builder
					.append(" ")
					.append(arg.getName());
			}
		}
		
		builder.append(")");
		
		return builder.toString();
	}

	public static String toReadableDBus(SignalNode signal)
	{
		List<ArgumentNode> args = signal.getArguments();
		
		StringBuilder builder = new StringBuilder();
		builder.append(signal.getName())
			.append(" (");
		
		boolean first = true;
			
		for(int i=0, n=args.size(); i<n; i++)
		{
			if(false == first)
			{
				builder.append(", ");
			}
			else
			{
				first = false;
			}
			
			ArgumentNode arg = args.get(i);
			DBusTypeGenerator.signatureToDBus(builder, arg.getSignatureType());
			
			if(arg.getName() != null)
			{
				builder
					.append(" ")
					.append(arg.getName());
			}
		}
		
		builder.append(")");
		
		return builder.toString();
	}
	
	public static void signatureToDBus(StringBuilder builder, SubSignature s)
	{
		switch(s.getType())
		{
			case ARRAY:
				builder.append("ARRAY of ");
				signatureToDBus(builder, s.getSignatures()[0]);
				break;
			case DICT_ENTRY:
				builder.append("DICT_ENTRY of (");
				signatureToDBus(builder, s.getSignatures()[0]);
				builder.append(",");
				signatureToDBus(builder, s.getSignatures()[1]);
				builder.append(")");
				break;
			case STRUCT:
				builder.append("STRUCT of (");
				SubSignature[] subs = s.getSignatures();
				for(int i=0, n=subs.length; i<n; i++)
				{
					if(i > 0)
					{
						builder.append(", ");
					}
					
					signatureToDBus(builder, subs[i]);
				}
				builder.append(")");
				break;
			default:
				builder.append(s.getType().name().toUpperCase());
				break;
		}
	}
}
