package se.l4.sofa.dbus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import se.l4.sofa.dbus.spi.UInt32;

/**
 * Annotation that can be used when the type of a parameter of a DBus method
 * needs to be changed. This allows the usage of standard Java-types instead
 * of types such as {@link UInt32}, instead the parameter can be annotated 
 * with <code>{@literal @In(DType.UINT32)}</code>.
 * 
 * @author Andreas Holstenson
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface In
{
	/**
	 * The type of this parameter.
	 * 
	 * @return
	 */
	DType value();
}
