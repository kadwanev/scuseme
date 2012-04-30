package com.kadwa.scuseme;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public class DefaultInvoker implements Invoker {

    @Override
    public <T extends Interceptor, I>
        void execute(Interception config, T interceptor, I intercepted, Method method, Object[] calcArgs) throws Throwable
    {
        interceptor.setInterceptedClass(intercepted);
        method.invoke(interceptor, calcArgs);
    }

    @Override
    public <T extends Interceptor, I>
        void execute(Interception config, InvocationHandler handler, T interceptor, I intercepted, Method method, Object[] calcArgs) throws Throwable
    {
        interceptor.setInterceptedClass(intercepted);
        handler.invoke(interceptor, method, calcArgs);
    }
}
