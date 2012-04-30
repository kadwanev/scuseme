package com.kadwa.scuseme;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public interface AsyncInvoker extends Invoker {

    @Override
    <T extends Interceptor, I>
        void execute(Interception config, T interceptor, I intercepted, Method method, Object[] calcArgs);

    @Override
    <T extends Interceptor, I>
        void execute(Interception config, InvocationHandler handler, T interceptor, I intercepted, Method method, Object[] calcArgs);
}
