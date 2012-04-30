package com.kadwa.scuseme;

import com.kadwa.scuseme.impl.InterceptionImpl;
import com.kadwa.scuseme.impl.InterceptionFactoryImpl;

/**
 * Created by Neville Kadwa.
 */
public class InterceptionFactory {

    private InterceptionFactoryImpl impl;

    public InterceptionFactory() {
        this(InterceptionConfig.create().build());
    }

    public InterceptionFactory(InterceptionConfig config) {
        impl = new InterceptionFactoryImpl(config);
    }

    public <T extends Interceptor<? extends I>, I> I createInterceptor(Class<I> interfaceClass, T interceptorInstance, I interceptedInstance) {
        return impl.createInterceptor(interfaceClass, interceptorInstance, interceptedInstance);
    }

    public <T extends Interceptor<? super I>, I> I createInterceptor(T interceptorInstance, I interceptedInstance) {
        return impl.createInterceptor(interceptorInstance, interceptedInstance);
    }

    /**
     * Return base instance without any interception
     */
    public <I> I drillToBase(I interceptedInstance) {
        return impl.drillToBase(interceptedInstance);
    }

    /**
     * Return singly unwrapped instance
     */
    public <T extends Interceptor, I> InterceptionImpl<I,T> unwrapProxy(I interceptedInstance) {
        return impl.unwrapProxy(interceptedInstance);
    }

    public <T extends Interceptor, I> String printInterceptionStack(I interceptedInstance) {
        return impl.printInterceptionStack(interceptedInstance);
    }
}
