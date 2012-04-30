package com.kadwa.scuseme.impl;

import com.kadwa.scuseme.Interceptor;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public class ChainCallbackHandler<I, T extends Interceptor> implements InvocationHandler, MethodInterceptor {
    private InterceptionPoint[] calls;
    private I intercepted;
    private Method interceptedMethod;
    private int offset;

    ChainCallbackHandler(InterceptionPoint[] calls, I intercepted, Method interceptedMethod) {
        this.calls = calls;
        this.intercepted = intercepted;
        this.interceptedMethod = interceptedMethod;
        this.offset = 0;
    }

    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        if ( method == interceptedMethod ) {
            offset++;
            if (offset < calls.length) {
                calls[offset].interceptor.setInterceptedClass(proxy);
                return calls[offset].method.invoke( calls[offset].interceptor, args );
            } else {
                return interceptedMethod.invoke( intercepted, args );
            }
        } else {
            return method.invoke( intercepted, args );
        }
    }

    public Object intercept( Object proxy, Method method, Object[] args, MethodProxy methodProxy ) throws Throwable {
        return invoke( proxy, method, args );
    }
}
