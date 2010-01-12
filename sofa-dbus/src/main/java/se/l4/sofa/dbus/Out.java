package se.l4.sofa.dbus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for the result of a DBus method call. Can either be used on message
 * parameters where it denotes an extra output. When placed on a method it is
 * used to override the DBus type by giving a {@link DType} to {@link #value()}.
 * 
 * <p>
 * Example usage (both method and parameter):
 * <pre>
 * {@literal @Out(DType.UINT32)}
 * long getData(String in, @{literal @Out(DType.INT16)} int key)
 * </pre>
 * 
 * @author Andreas Holstenson
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.PARAMETER })
public @interface Out
{
	DType value() default DType.INVALID;
}
