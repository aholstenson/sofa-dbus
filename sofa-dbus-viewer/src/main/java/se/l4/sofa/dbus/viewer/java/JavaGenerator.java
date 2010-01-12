package se.l4.sofa.dbus.viewer.java;

import java.util.List;

import se.l4.sofa.dbus.spi.Signature.SubSignature;
import se.l4.sofa.dbus.viewer.tree.ArgumentNode;
import se.l4.sofa.dbus.viewer.tree.MethodNode;

public class JavaGenerator
{
	public static String toJavaMethod(MethodNode method)
	{
		StringBuilder builder = new StringBuilder();
		toJavaMethod(builder, method);
		return builder.toString();
	}
	
	public static void toJavaMethod(StringBuilder builder, MethodNode method)
	{
		List<ArgumentNode> in = method.getInArguments();
		List<ArgumentNode> out = method.getOutArguments();
		
		if(false == out.isEmpty())
		{
			ArgumentNode arg = out.get(0);
			
			signatureToJava(builder, arg.getSignatureType(), true, false);
		}
		else
		{
			builder.append("void");
		}
		builder
			.append(" ")
			.append(method.getName())
			.append("(");
		
		boolean first = true;
		int argCount = 0;
		
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
			
			ArgumentNode arg = in.get(i);
			signatureToJava(builder, arg.getSignatureType(), true, true);
			
			if(arg.getName() != null)
			{
				builder
					.append(" ")
					.append(arg.getName());
			}
			else
			{
				builder.append(" arg").append(argCount++);
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
			
			builder.append("Holder<");
			ArgumentNode arg = out.get(i);
			signatureToJava(builder, arg.getSignatureType(), false, false);
			builder.append(">");
			
			if(arg.getName() != null)
			{
				builder
					.append(" ")
					.append(arg.getName());
			}
			else
			{
				builder.append(" arg").append(argCount++);
			}
		}
		
		builder.append(")");
	}
	
	private static void signatureToJava(StringBuilder builder, SubSignature s, boolean top, boolean in)
	{
		switch(s.getType())
		{
			case ARRAY:
				// Arrays actually depend on their inner type
				builder.append("List<");
				signatureToJava(builder, s.getSignatures()[0], false, in);
				builder.append(">");
				break;
			case BOOLEAN:
				builder.append("boolean");
				break;
			case BYTE:
				builder.append("byte");
				break;
			case DICT_ENTRY:
			{
				SubSignature[] subs = s.getSignatures();
				builder.append("Map<");
				signatureToJava(builder, subs[0], false, in);
				builder.append(", ");
				signatureToJava(builder, subs[1], false, in);
				builder.append(">");
				break;
			}	
			case DOUBLE:
				builder.append("double");
				break;
			case INT16:
				builder.append("short");
				break;
			case UINT16:
				if(top)
				{
					builder
						.append("@")
						.append(in ? "In" : "Out")
						.append("(DType.UINT16) int");
				}
				else
				{
					builder.append("UInt16");
				}
				break;
			case INT32:
				builder.append("int");
				break;
			case UINT32:
				if(top)
				{
					builder
						.append("@")
						.append(in ? "In" : "Out")
						.append("(DType.UINT32) long");
				}
				else
				{
					builder.append("UInt32");
				}
				break;
			case INT64:
				builder.append("long");
				break;
			case UINT64:
				builder.append("UInt64");
				break;
			case OBJECT_PATH:
				if(top)
				{
					builder
						.append("@")
						.append(in ? "In" : "Out")
						.append("(DType.OBJECT_PATH) String");
				}
				else
				{
					builder.append("ObjectPath");
				}
				break;
			case SIGNATURE:
				builder.append("Signature");
				break;
			case STRING:
				builder.append("String");
				break;
			case STRUCT:
				builder.append("Struct<");
				SubSignature[] subs = s.getSignatures();
				for(int i=0, n=subs.length; i<n; i++)
				{
					if(i > 0)
					{
						builder.append(", ");
					}
					
					signatureToJava(builder, subs[i], false, in);
				}
				builder.append(">");
				break;
			case VARIANT:
				if(top)
				{
					builder
						.append("@")
						.append(in ? "In" : "Out")
						.append("(DType.VARIANT) Object");
				}
				else
				{
					builder.append("Variant");
				}
		}		
	}
	
//	private static void signatureToStruct(StringBuilder builder, SubSignature s, int count)
//	{
//		switch(s.getType())
//		{
//			case ARRAY:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				// Arrays actually depend on their inner type
//				builder.append("List<");
//				signatureToStruct(builder, s.getSignatures()[0], -1);
//				builder.append(">");
//				break;
//			case BOOLEAN:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				builder.append("boolean");
//				break;
//			case BYTE:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				builder.append("byte");
//				break;
//			case DICT_ENTRY:
//			{
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				SubSignature[] subs = s.getSignatures();
//				builder.append("Map<");
//				signatureToStruct(builder, subs[0], -1);
//				builder.append(", ");
//				signatureToStruct(builder, subs[1], -1);
//				builder.append(">");
//				break;
//			}	
//			case DOUBLE:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				builder.append("double");
//				break;
//			case INT16:
//				builder.append("@StructPosition(").append(count).append(")\n");
//				builder.append("short");
//				break;
//			case UINT16:
//				if(count >= 0)
//				{
//					builder
//						.append("@StructPosition(value=")
//						.append(count)
//						.append(", type=DType.UINT16)\nint");
//				}
//				else
//				{
//					builder.append("UInt16");
//				}
//				break;
//			case INT32:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				builder.append("int");
//				break;
//			case UINT32:
//				if(count >= 0)
//				{
//					builder
//						.append("@StructPosition(value=")
//						.append(count)
//						.append(", DType.UINT32) long");
//				}
//				else
//				{
//					if(count >= 0)
//					{
//						builder.append("@StructPosition(").append(count).append(")\n");
//					}
//					
//					builder.append("UInt32");
//				}
//				break;
//			case INT64:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				builder.append("long");
//				break;
//			case UINT64:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				builder.append("UInt64");
//				break;
//			case OBJECT_PATH:
//				if(top)
//				{
//					builder
//						.append("@")
//						.append(in ? "In" : "Out")
//						.append("(DType.OBJECT_PATH) String");
//				}
//				else
//				{
//					if(count >= 0)
//					{
//						builder.append("@StructPosition(").append(count).append(")\n");
//					}
//					
//					builder.append("ObjectPath");
//				}
//				break;
//			case SIGNATURE:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				builder.append("Signature");
//				break;
//			case STRING:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				builder.append("String");
//				break;
//			case STRUCT:
//				if(count >= 0)
//				{
//					builder.append("@StructPosition(").append(count).append(")\n");
//				}
//				
//				builder.append("Struct<");
//				SubSignature[] subs = s.getSignatures();
//				for(int i=0, n=subs.length; i<n; i++)
//				{
//					if(i > 0)
//					{
//						builder.append(", ");
//					}
//					
//					signatureToJava(builder, subs[i], false, in);
//				}
//				builder.append(">");
//				break;
//			case VARIANT:
//				if(top)
//				{
//					builder
//						.append("@")
//						.append(in ? "In" : "Out")
//						.append("(DType.VARIANT) Object");
//				}
//				else
//				{
//					if(count >= 0)
//					{
//						builder.append("@StructPosition(").append(count).append(")\n");
//					}
//					
//					builder.append("Variant");
//				}
//		}
//	}
}
