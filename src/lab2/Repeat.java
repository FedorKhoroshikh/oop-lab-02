package lab2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that has to be called several times through reflection.
 * <p>
 * {@link RetentionPolicy#RUNTIME} is the part that makes the whole lab work:
 * with the default CLASS retention the annotation is dropped by the JVM and
 * {@code isAnnotationPresent} would never see it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Repeat {

    /** How many times the annotated method should be called. */
    int value();
}
