package com.kadwa.scuseme;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public class DefaultInvoker implements Invoker<Object> {

    @Override
    public <T extends Interceptor, I>
        Object execute(Interception config, T interceptor, I intercepted, Method method, Object[] calcArgs) throws Throwable
    {
        interceptor.setInterceptedClass(intercepted);
        return method.invoke(interceptor, calcArgs);
    }

    @Override
    public <T extends Interceptor, I>
        Object execute(Interception config, InvocationHandler handler, T interceptor, I intercepted, Method method, Object[] calcArgs) throws Throwable
    {
        interceptor.setInterceptedClass(intercepted);
        return handler.invoke(interceptor, method, calcArgs);
    }
}
