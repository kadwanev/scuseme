package com.kadwa.scuseme;

import java.lang.annotation.Annotation;

/**
 * Created by Neville Kadwa.
 */
public interface InterceptionCapability<A extends Annotation> {

    public Class<A> getAnnotation();

    /**
     * Allows flexible capability
     * @return null if annotation shouldn't be intercepted, else CallType
     */
    public Interception.CallType getCallType(A annotation);

}
