package com.kadwa.scuseme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by Neville Kadwa.
 */
public class DefaultAsyncInvoker implements AsyncInvoker {

    final static Logger logger = LoggerFactory.getLogger(DefaultAsyncInvoker.class);
    protected ScheduledExecutorService executorService;

    public DefaultAsyncInvoker(InterceptionConfig configuration) {
        this.executorService = Executors.newScheduledThreadPool(configuration.getAsyncExecutorPoolSize());
    }

    protected void handleException(Throwable t) {
        logger.error("Unhandled Async Exception: " + t.getMessage(), t);
    }

    public <T extends Interceptor, I>
        void execute(final Interception config, final T interceptor, final I intercepted, final Method method, final Object[] calcArgs)
    {
        Runnable w = new Runnable() {
            @Override
            public void run() {
                try {
                    interceptor.setInterceptedClass(intercepted);
                    method.invoke(interceptor, calcArgs);
                }
                catch (Throwable t) {
                    handleException(t);
                }
            }
        };
        executorService.schedule(w, config.delay(), TimeUnit.MILLISECONDS);
    }

    @Override
    public <T extends Interceptor, I>
        void execute(final com.kadwa.scuseme.Interception config, final InvocationHandler handler, final T interceptor, final I intercepted, final Method method, final Object[] calcArgs)
    {
        Runnable w = new Runnable() {
            @Override
            public void run() {
                try {
                    interceptor.setInterceptedClass(intercepted);
                    handler.invoke(interceptor, method, calcArgs);
                }
                catch (Throwable t) {
                    handleException(t);
                }
            }
        };
        executorService.schedule(w, config.delay(), TimeUnit.MILLISECONDS);
    }
}
