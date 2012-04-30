package com.kadwa.scuseme;

/**
 *
 * Created by Neville Kadwa.
 */
public interface Interceptor<T> {

    public void setInterceptedClass(T interceptedInstance);

}
