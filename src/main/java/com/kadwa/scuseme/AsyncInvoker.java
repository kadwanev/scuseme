package com.kadwa.scuseme;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by Neville Kadwa.
 */
public interface AsyncInvoker extends Invoker<ScheduledFuture> {

    @Override
    <T extends Interceptor, I>
        ScheduledFuture execute(Interception config, T interceptor, I intercepted, Method method, Object[] calcArgs);

    @Override
    <T extends Interceptor, I>
        ScheduledFuture execute(Interception config, InvocationHandler handler, T interceptor, I intercepted, Method method, Object[] calcArgs);
}
