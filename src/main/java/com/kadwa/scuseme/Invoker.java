package com.kadwa.scuseme;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public interface Invoker<R> {

    public <T extends Interceptor, I>
        R execute(Interception config, T interceptor, I intercepted, Method method, Object[] calcArgs) throws Throwable;

    public <T extends Interceptor, I>
        R execute(Interception config, InvocationHandler handler, T interceptor, I intercepted, Method method, Object[] calcArgs) throws Throwable;

}
