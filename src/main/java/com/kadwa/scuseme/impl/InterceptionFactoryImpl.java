package com.kadwa.scuseme.impl;

import com.kadwa.scuseme.Interception;
import com.kadwa.scuseme.InterceptionConfig;
import com.kadwa.scuseme.InterceptionFactory;
import com.kadwa.scuseme.Interceptor;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Created by Neville Kadwa.
 */
public class InterceptionFactoryImpl {

    final static Logger logger = LoggerFactory.getLogger(InterceptionFactory.class);
    final InterceptionConfig config;

    public InterceptionFactoryImpl(InterceptionConfig config) {
        this.config = config;
    }

    public <T extends Interceptor<? extends I>, I> I createInterceptor(Class<I> interfaceClass, T interceptorInstance, I interceptedInstance) {

        if ( !appendPrevious( interfaceClass, interceptorInstance, interceptedInstance ) ) {

            InterceptionImpl interception;
            if ( InvocationHandler.class.isAssignableFrom( interceptorInstance.getClass() ) ) {
                interception = new InterceptionImpl(this, interceptedInstance, (InvocationHandler) interceptorInstance);
            } else {
                interception = new InterceptionImpl(this, interfaceClass, interceptedInstance, interceptorInstance);
            }

            return (I) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, interception);
        }
        return interceptedInstance;
    }

    public <T extends Interceptor<? super I>, I> I createInterceptor(T interceptorInstance, I interceptedInstance) {
        Class interceptedClass = interceptedInstance.getClass();

        if ( !appendPrevious( interceptedClass, interceptorInstance, interceptedInstance ) ) {

            InterceptionImpl interception;

            if ( InvocationHandler.class.isAssignableFrom( interceptorInstance.getClass() ) ) {
                interception = new InterceptionImpl(this, interceptedInstance, (InvocationHandler) interceptorInstance );
            } else {
                interception = new InterceptionImpl(this, interceptedInstance, interceptorInstance);
            }

            return (I) Enhancer.create(interceptedClass, interception);
        }
        return interceptedInstance;
    }

    private <T extends Interceptor, I> boolean appendPrevious(Class<I> interceptedClass, T interceptorInstance, I interceptedInstance) {
        boolean currentChaining = isChaining( interceptorInstance.getClass() );

        if ( Proxy.isProxyClass( interceptedInstance.getClass() ) ) {
            InvocationHandler ih = Proxy.getInvocationHandler( interceptedInstance );
            if (ih instanceof InterceptionImpl) {
                InterceptionImpl interception = ((InterceptionImpl) ih);
                if ( !currentChaining || !interception.hasChaining ) {
                    if ( InvocationHandler.class.isAssignableFrom( interceptorInstance.getClass() ) ) {
                        interception.initializeInvocationHandler( (InvocationHandler) interceptorInstance, interception.baseIntercepted );
                    } else {
                        interception.initializeInterceptMethodMap( interceptedClass, interceptorInstance.getClass(), interception.baseIntercepted, interceptorInstance );
                    }
                    return true;
                }
            }
        }
        if ( Enhancer.isEnhanced( interceptedInstance.getClass() ) ) {
            InterceptionImpl interception = (InterceptionImpl) ( (Factory) interceptedInstance ).getCallback( 0 );
            if ( currentChaining && interception.hasChaining )
                logger.warn("It is not performant to have multiple interceptions of chaining using enhanced classes");
            if ( InvocationHandler.class.isAssignableFrom( interceptorInstance.getClass() ) ) {
                interception.initializeInvocationHandler((InvocationHandler) interceptorInstance, interception.baseIntercepted);
            } else {
                interception.initializeInterceptMethodMap(interception.baseIntercepted.getClass(), interceptorInstance.getClass(), interception.baseIntercepted, interceptorInstance);
            }
            return true;
        }
        return false;
    }

    public <T extends Interceptor, I> I drillToBase(I interceptedInstance) {
        InterceptionImpl<I,T> interceptor = unwrapProxy( interceptedInstance );
        if (interceptor != null)
            return drillToBase( interceptor.baseIntercepted );
        return interceptedInstance;
    }

    public <I> InterceptionImpl unwrapProxy(I interceptedInstance) {
        if ( Proxy.isProxyClass( interceptedInstance.getClass() ) ) {
            InvocationHandler ih = Proxy.getInvocationHandler( interceptedInstance );
            if (ih instanceof InterceptionImpl) {
                return (InterceptionImpl) ih;
            }
        }
        if ( Enhancer.isEnhanced( interceptedInstance.getClass() ) ) {
            return (InterceptionImpl) ( (Factory) interceptedInstance ).getCallback( 0 );
        }
        return null;
    }

    public <T extends Interceptor, I> String printInterceptionStack(I interceptedInstance) {
        StringBuilder sb = new StringBuilder();
        printInterceptionStack( interceptedInstance, "", sb );
        return sb.toString();
    }

    private <T extends Interceptor, I> void printInterceptionStack(I interceptedInstance, String pre, StringBuilder sb) {
        InterceptionImpl<I,T> interceptor = unwrapProxy( interceptedInstance );
        if ( interceptor == null ) {
            sb.append(pre).append(interceptedInstance.getClass().getName()).append( "\n" );
            return;
        }

        if (interceptor.invocationHandlers != null) {
            sb.append(pre).append( "InvocationHandler: " ).append( printMethodOverrides( interceptor.invocationHandlers, true ) ).append( "\n" );
        }
        if (interceptor.interceptMethodMap != null) {
            for ( Map.Entry<Method, InterceptionSet> entry : interceptor.interceptMethodMap.entrySet() ) {
                sb.append(pre).append( entry.getKey().getName() ).append( ":" ).append( printMethodOverrides( entry.getValue(), false ) ).append( "\n" );
            }
        }

        printInterceptionStack( interceptor.baseIntercepted, pre + "  ", sb );
    }

    private <T extends Interceptor> String printMethodOverrides(InterceptionSet overrides, boolean isInvocationHandler) {
        StringBuilder sb = new StringBuilder();
        InterceptionPoint order[][] = {overrides.before, overrides.chain, overrides.after, overrides.async};
        for (InterceptionPoint[] t : order) {
            if (t != null && t.length > 0) {
                sb.append( " " ).append( t[ 0 ].config.type().name() + ": " );
                for (int i = 0; i < t.length; i++) {
                    if (isInvocationHandler)
                        sb.append( t[i].interceptor.getClass().getName() );
                    else
                        sb.append(t[i].interceptor.getClass().getName() + "." + t[i].method.getName());
                }
            }
        }
        return sb.toString();
    }

    private static boolean isChaining(Class targetClass) {
        for (Method m : targetClass.getMethods()) {
            Interception a = m.getAnnotation( Interception.class );
            if (a != null) {
                if (a.type() == Interception.CallType.CHAIN)
                    return true;
            }
        }
        return false;
    }



}
