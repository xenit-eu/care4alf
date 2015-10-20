package eu.xenit.care4alf.module.bulk;

import org.aspectj.lang.annotation.DeclareAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by mhgam on 20/10/2015.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Worker {
    public String action() default "noname";
    public String[] parameterNames() default {};


}
