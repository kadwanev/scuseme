package com.kadwa.scuseme.internal;

import com.kadwa.scuseme.*;
import com.kadwa.scuseme.Interception;

import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
class InterceptionPoint<T extends Interceptor> {
    public final Interception config;
    public final T interceptor;
    public final Method method;

    InterceptionPoint(Interception config, T interceptor, Method method) {
        this.config = config;
        this.interceptor = interceptor;
        this.method = method;
    }
}
