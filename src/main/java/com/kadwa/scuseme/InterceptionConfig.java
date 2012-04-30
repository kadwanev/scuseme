package com.kadwa.scuseme;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Created by Neville Kadwa.
 */
public class InterceptionConfig {

    private boolean enforceInterceptedAnnotation;
    private int asyncExecutorPoolSize;
    private Invoker invoker;
    private AsyncInvoker asyncInvoker;
    private Map<Class<? extends Annotation>, InterceptionCapability> capabilities;

    private InterceptionConfig() {

    }

    public int getAsyncExecutorPoolSize() {
        return asyncExecutorPoolSize;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public Invoker getAsyncInvoker() {
        return asyncInvoker;
    }

    public boolean isEnforceInterceptedAnnotation() {
        return enforceInterceptedAnnotation;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private boolean enforceInterceptedAnnotation = false;
        private int asyncExecutorPoolSize = 100;
        private Invoker invoker;
        private AsyncInvoker asyncInvoker;
        private List<InterceptionCapability> capabilities = new ArrayList<InterceptionCapability>();

        public Builder setEnforceInterceptedAnnotation(boolean enforceInterceptedAnnotation) {
            this.enforceInterceptedAnnotation = enforceInterceptedAnnotation;
            return this;
        }

        public Builder setAsyncExecutorPoolSize(int asyncExecutorPoolSize) {
            this.asyncExecutorPoolSize = asyncExecutorPoolSize;
            return this;
        }

        public Builder setAsyncInvoker(AsyncInvoker asyncInvoker) {
            this.asyncInvoker = asyncInvoker;
            return this;
        }

        public Builder setInvoker(Invoker invoker) {
            this.invoker = invoker;
            return this;
        }

        public Builder addCapability(InterceptionCapability capability) {
            capabilities.add(capability);
            return this;
        }

        public InterceptionConfig build() {
            InterceptionConfig config = new InterceptionConfig();
            config.enforceInterceptedAnnotation = enforceInterceptedAnnotation;
            config.asyncExecutorPoolSize = asyncExecutorPoolSize;
            config.capabilities = new HashMap<Class<? extends Annotation>, InterceptionCapability>();
            config.invoker = (invoker != null ? invoker : new DefaultInvoker());
            config.asyncInvoker = (asyncInvoker != null ? asyncInvoker : new DefaultAsyncInvoker(config));
            for (InterceptionCapability capability : capabilities) {
                config.capabilities.put(capability.getAnnotation(), capability);
            }
            return config;
        }

    }

}
