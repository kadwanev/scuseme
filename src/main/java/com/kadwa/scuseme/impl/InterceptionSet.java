package com.kadwa.scuseme.impl;

import com.kadwa.scuseme.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by Neville Kadwa.
 */
public class InterceptionSet<I, T extends Interceptor> {
    final static Logger logger = LoggerFactory.getLogger(InterceptionSet.class);

    public final I intercepted;
    public InterceptionPoint[] before;
    public InterceptionPoint[] chain;
    public InterceptionPoint[] after;
    public InterceptionPoint[] async;

    private InterceptionSet(I intercepted) {
        this.intercepted = intercepted;
    }

    private static <I, T extends Interceptor> InterceptionPoint[] add(InterceptionPoint[] cur, InterceptionPoint addition)
    {

        InterceptionPoint[] post;
        if (cur == null)
            post = new InterceptionPoint[1];
        else
            post = Arrays.copyOfRange(cur, 0, cur.length + 1);

        post[post.length-1] = addition;
        return post;
    }

    public static <I, T extends Interceptor> InterceptionSet<I,T> addOverride
            (InterceptionSet<I,T> overrides, I intercepted, InterceptionPoint override) {

        if (overrides == null)
            overrides = new InterceptionSet(intercepted);

        switch (override.config.type()) {
            case BEFORE:
                overrides.before = add( overrides.before, override );
                break;
            case AFTER:
                overrides.after = add( overrides.after, override );
                break;
            case CHAIN:
                overrides.chain = add( overrides.chain, override );
                break;
            case ASYNC:
                overrides.async = add( overrides.async, override );
                break;
            default:
                logger.error("Unimplemented CallType: " + override.config.type());
        }

        return overrides;
    }
}
