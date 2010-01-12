package se.l4.sofa.dbus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define the name of a {@link DbusInterface} or method within the interface.
 * When this annotation is used the implicit name of the interface or method
 * will be overridden with the value of the annotation. This useful to map
 * DBus naming conventions to interfaces with Java naming conventions.
 * 
 * @author Andreas Holstenson
 *
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Name
{
	String value();
}
