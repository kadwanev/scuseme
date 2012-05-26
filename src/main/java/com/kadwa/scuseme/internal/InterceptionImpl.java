package com.kadwa.scuseme.internal;

import com.kadwa.scuseme.Interception;
import com.kadwa.scuseme.Interceptor;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

/**
 * Created by Neville Kadwa
 */
public class InterceptionImpl<I, T extends Interceptor> implements InvocationHandler, MethodInterceptor {

    final static Logger logger = LoggerFactory.getLogger(InterceptionImpl.class);

    private final InterceptionFactoryImpl factory;
    final I baseIntercepted;
    final Class baseInterceptedClass;
    // Interceptor Method, Intercepted Method - no updates after initialization
    HashMap<Method, InterceptionSet<I,T>> interceptMethodMap = null;
    InterceptionSet<I,T> invocationHandlers = null;
    boolean hasChaining = false;


    InterceptionImpl(InterceptionFactoryImpl factory, Class<I> interfaceClass, I intercepted, T interceptor) {
        this.factory = factory;
        this.baseIntercepted = intercepted;
        this.baseInterceptedClass = interfaceClass;
        initializeInterceptMethodMap(interfaceClass, interceptor.getClass(), intercepted, interceptor);
    }

    InterceptionImpl(InterceptionFactoryImpl factory, I intercepted, T interceptor) {
        this.factory = factory;
        this.baseIntercepted = intercepted;
        this.baseInterceptedClass = intercepted.getClass();
        initializeInterceptMethodMap( intercepted.getClass(), interceptor.getClass(), intercepted, interceptor );
    }

    InterceptionImpl(InterceptionFactoryImpl factory, I intercepted, InvocationHandler invocationHandler) {
        this.factory = factory;
        this.baseIntercepted = intercepted;
        this.baseInterceptedClass = intercepted.getClass();
        initializeInvocationHandler( invocationHandler, intercepted );
    }

    void initializeInterceptMethodMap(Class sourceClass, Class targetClass, I intercepted, T interceptor) {
        if (interceptMethodMap == null)
            interceptMethodMap = new HashMap<Method, InterceptionSet<I,T>>();
        for (Method m : targetClass.getMethods()) {
            Interception a = m.getAnnotation( Interception.class );
            if (a != null) {
                if (a.type() == Interception.CallType.CHAIN)
                    this.hasChaining = true;
                try {
                    String methodName = a.mName();
                    if ( methodName == null || methodName.length() == 0 )
                        methodName = m.getName();
                    Class[] paramTypes = calcMethodTypes( a, m );
                    Method im = sourceClass.getMethod( methodName, paramTypes );
                    m.setAccessible( true );
                    InterceptionSet overrides = interceptMethodMap.get( im );
                    if ( overrides == null )
                        interceptMethodMap.put( im, InterceptionSet.addOverride(overrides, intercepted, new InterceptionPoint(a, interceptor, m)) );
                    else
                        InterceptionSet.addOverride(overrides, intercepted, new InterceptionPoint(a, interceptor, m));
                }
                catch (NoSuchMethodException nsmex) {
                    logger.error("Expected overriding method not found: " + m);
                }
            }
        }
    }

    void initializeInvocationHandler(InvocationHandler invocationHandler, I intercepted) {
        try {
            Method m = invocationHandler.getClass().getMethod( "invoke", Object.class, Method.class, Object[].class );
            Interception a = m.getAnnotation( Interception.class );
            if (a == null) {
                logger.error("Expected InterceptionOverride annotation not found");
                throw new IllegalArgumentException( "Expected InterceptionOverride annotation not found" );
            }

            if (a.type() == Interception.CallType.CHAIN)
                this.hasChaining = true;
            invocationHandlers = InterceptionSet.addOverride(invocationHandlers, intercepted,
                    new InterceptionPoint(a, (Interceptor) invocationHandler, null));
        }
        catch (NoSuchMethodException nsmex) {
            logger.error("Expected invocation method not found: invoke");
        }
    }

    private Class[] calcMethodTypes(Interception a, Method m) {
        Class[] paramTypes = m.getParameterTypes();
        if (a.type() == Interception.CallType.AFTER || a.type() == Interception.CallType.ASYNC) {
            switch (a.after()) {
                case RETURN:
                    if (m.getReturnType() != void.class) {
                        Class[] c = new Class[paramTypes.length-1];
                        if (m.getReturnType() != paramTypes[0]) {
                            logger.error("Illegal method matching. Return type of interceptor must match return type of intercepted: " + m.toString());
                            return paramTypes;
                        }
                        System.arraycopy( paramTypes, 1, c, 0, paramTypes.length-1 );
                        paramTypes = c;
                    }
                    break;
                case RETURN_EXCEPTION:
                    int returnOffset = 0;
                    if (m.getReturnType() != void.class) {
                        returnOffset = 1;
                        if (m.getReturnType() != paramTypes[0]) {
                            logger.error("Illegal method matching. Return type of interceptor must match return type of intercepted: " + m.toString());
                            return paramTypes;
                        }
                    }
                    if (paramTypes[returnOffset] != Throwable.class) {
                        logger.error("Illegal method matching. Exception parameter of interceptor must be Throwable: " + m.toString());
                        return paramTypes;
                    }
                    Class[] c = new Class[paramTypes.length-returnOffset-1];
                    System.arraycopy( paramTypes, returnOffset+1, c, 0, paramTypes.length-returnOffset-1 );
                    paramTypes = c;
                    break;
                default:
                    break;
            }
        }
        return paramTypes;
    }

    private Object[] calcMethodParams(Interception a, Method m, Object[] args, Object callReturn, Throwable callException) {
        if (a.type() == Interception.CallType.AFTER || a.type() == Interception.CallType.ASYNC) {
            if (a.after() == Interception.AfterMode.RETURN) {
                if (m == null || m.getReturnType() != void.class) {
                    Object[] callArgs = new Object[(args == null ? 0 : args.length)+1];
                    callArgs[0] = callReturn;
                    if (args != null)
                        System.arraycopy( args, 0, callArgs, 1, args.length );
                    return callArgs;
                }
            } else
            if (a.after() == Interception.AfterMode.RETURN_EXCEPTION) {
                Object[] callArgs;
                int argOffset;
                if (m == null || m.getReturnType() != void.class) {
                    callArgs = new Object[(args == null ? 0 : args.length)+2];
                    callArgs[0] = callReturn;
                    callArgs[1] = callException;
                    argOffset = 2;
                } else {
                    callArgs = new Object[(args == null ? 0 : args.length)+1];
                    callArgs[0] = callException;
                    argOffset = 1;
                }
                if (args != null)
                    System.arraycopy( args, 0, callArgs, argOffset, args.length );
                return callArgs;
            }
        }
        return args;
    }

    private Object doCallback(final InterceptionPoint[] calls,
                            final I intercepted,
                            final Object[] args,
                            final Method interceptedMethod,
                            final InterceptionPoint[] handlers,
                            final Object callReturn,
                            final Throwable callException) throws Throwable
    {
        Object firstReturn = null;
        if (calls != null) {
            for ( final InterceptionPoint call : calls ) {
                // If entered with exception, only apply to after/RETURN_EXCEPTION configs
                if (callException != null && call.config.after() != Interception.AfterMode.RETURN_EXCEPTION)
                    continue;
                final Object[] calcArgs = calcMethodParams( call.config, call.method, args, callReturn, callException );
                if ( call.config.type() != Interception.CallType.ASYNC ) {
                    Object r = factory.config.getInvoker().execute(call.config, call.interceptor, intercepted, call.method, calcArgs);
                    if (firstReturn == null && call.config.before() == Interception.BeforeMode.RETURNABLE)
                        firstReturn = r;
                } else {
                    factory.config.getAsyncInvoker().execute(call.config, call.interceptor, intercepted, call.method, calcArgs);
                }
            }
        }
        if (handlers != null) {
            for ( final InterceptionPoint call : handlers ) {
                if (callException != null && call.config.after() != Interception.AfterMode.RETURN_EXCEPTION)
                    continue;
                final Object[] calcArgs = calcMethodParams( call.config, null, args, callReturn, callException );
                if ( call.config.type() != Interception.CallType.ASYNC ) {
                    Object r = factory.config.getInvoker().execute(call.config, (InvocationHandler) call.interceptor, call.interceptor, intercepted, interceptedMethod, calcArgs);
                    if (firstReturn == null && call.config.before() == Interception.BeforeMode.RETURNABLE)
                        firstReturn = r;
                } else {
                    factory.config.getAsyncInvoker().execute(call.config, (InvocationHandler) call.interceptor, call.interceptor, intercepted, interceptedMethod, calcArgs);
                }
            }
        }
        return firstReturn;
    }

    private Object doChainCallback(InterceptionPoint[] calls, Object[] args,
                                   Class interceptedClass, I intercepted, Method interceptedMethod,
                                   InterceptionPoint[] handlers) throws Throwable {

        if (calls == null && handlers == null)
            return interceptedMethod.invoke( intercepted, args );
        if (calls != null && calls.length == 1 && handlers == null) {
            calls[0].interceptor.setInterceptedClass(intercepted);
            return calls[0].method.invoke( calls[0].interceptor, args );
        } else
        if (calls == null && handlers != null && handlers.length == 1) {
            handlers[0].interceptor.setInterceptedClass(intercepted);
            return ((InvocationHandler) handlers[0].interceptor).invoke( intercepted, interceptedMethod, args );
        } else {

        }
        if (calls != null) {
            if (calls.length == 1) {
                calls[0].interceptor.setInterceptedClass(intercepted);
            } else {
                if (interceptedClass.isInterface())
                    calls[0].interceptor.setInterceptedClass( Proxy.newProxyInstance( interceptedClass.getClassLoader(),
                            new Class[] {interceptedClass},
                            new ChainCallbackHandler( calls, this.baseIntercepted, interceptedMethod ) ) );
                else
                    calls[0].interceptor.setInterceptedClass( Enhancer.create( interceptedClass,
                            new ChainCallbackHandler( calls, this.baseIntercepted, interceptedMethod ) ) );
            }
            return calls[0].method.invoke( calls[0].interceptor, args );
        }
        throw new Exception("Whaa?");
    }

    /**
     * InvocationHandler's invoke interface.
     */
    public Object invoke( Object proxy, Method method, final Object[] args ) throws Throwable {
        final InterceptionSet<I,T> overrides = (interceptMethodMap != null ? interceptMethodMap.get( method ) : null);
        Object toReturn = null;
        try {
            if (overrides == null && invocationHandlers == null )
                return method.invoke( this.baseIntercepted, args );

            toReturn = doCallback( (overrides != null ? overrides.before : null),
                    this.baseIntercepted, args, method,
                    (invocationHandlers != null ? invocationHandlers.before : null ),
                    null, null);

            if (toReturn == null) {
                // Before interruptor returned result, skipping invocation
                toReturn = doChainCallback( (overrides != null ? overrides.chain : null),
                        args, this.baseInterceptedClass,
                        (overrides != null ? overrides.intercepted : this.baseIntercepted),
                        method,
                        (invocationHandlers != null ? invocationHandlers.chain : null ) );
            }

            doCallback( (overrides != null ? overrides.after : null),
                    (overrides != null ? overrides.intercepted : this.baseIntercepted),
                    args, method,
                    (invocationHandlers != null ? invocationHandlers.after : null ),
                    toReturn, null);

            return toReturn;
        }
        catch (InvocationTargetException itex) {
            Throwable exception = itex.getTargetException();
            doCallback( (overrides != null ? overrides.after : null),
                    (overrides != null ? overrides.intercepted : this.baseIntercepted),
                    args, method,
                    (invocationHandlers != null ? invocationHandlers.after : null ),
                    toReturn, exception);

            throw exception;
        }
    }

    /**
     * cglib's MethodInterceptor intercept method. Forwarded to InvocationHandler's interface.
     */
    public Object intercept( Object proxy, Method method, Object[] args, MethodProxy methodProxy ) throws Throwable {
        return invoke( proxy, method, args );
    }

}
