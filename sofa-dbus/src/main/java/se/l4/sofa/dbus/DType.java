package se.l4.sofa.dbus;

/**
 * Different types of DBus types, used together with {@link In} to define
 * the type of a parameter.
 * 
 * @author Andreas Holstenson
 *
 */
public enum DType
{
	INVALID,
	BYTE,
	BOOLEAN,
	INT16,
	UINT16,
	INT32,
	UINT32,
	INT64,
	UINT64,
	DOUBLE,
	STRING,
	OBJECT_PATH,
	SIGNATURE,
	ARRAY,
	STRUCT,
	VARIANT,
	DICT_ENTRY;
}
