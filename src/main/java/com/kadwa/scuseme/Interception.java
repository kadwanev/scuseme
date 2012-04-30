package com.kadwa.scuseme;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Neville Kadwa.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Interception {
    public CallType type();
    public static enum CallType {
        CHAIN,
        BEFORE,
        AFTER,
        ASYNC
    }
    public int delay() default 3000; // Async delay in millis
    public String mName() default "";
    public static enum BeforeMode {
        NO_EFFECT,
        INTERRUPTABLE    // If return of interceptor is not-null, bypass method invocation
    }
    public BeforeMode before() default BeforeMode.NO_EFFECT; // Only applicable for Before

    public static enum AfterMode {
        NONE,            // Same signature as intercepted
        RETURN,          // Additional first parameter for non-void return type, must have same return signature
        RETURN_EXCEPTION // RETURN plus next Throwable parameter for raised exceptions
    }
    public AfterMode after() default AfterMode.RETURN; // Only applicable for After/ASync

}
