package se.l4.sofa.dbus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be placed on the fields of class to indicate the place
 * the field has in a struct, {@link DBusSignal} or {@link DBusException}.
 *
 * <p>
 * Example:
 * 
 * <pre>
 * class ExampleStruct {
 * 	@{literal @StructPosition(0)}
 * 	private String name;
 * 	@{literal @StructPosition(value=1, type=DType.INT16)}
 * 	private int age;
 * }
 * </pre>
 * 
 * @author Andreas Holstenson
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface StructPosition
{
	/** Struct position */
	int value();
	
	/** Type of the field. */
	DType type() default DType.INVALID;
}
