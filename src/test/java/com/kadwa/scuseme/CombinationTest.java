package com.kadwa.scuseme;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Neville Kadwa.
 */
public class CombinationTest extends TestCase {

    final static Logger logger = LoggerFactory.getLogger(CombinationTest.class);

    InterceptionFactory interceptionFactory;

    @Override
    protected void setUp() throws Exception {
        interceptionFactory = new InterceptionFactory();
    }

    public void testMultipleOverrideCombination() {
        Example example = new Example();
        BeforeTest.BeforeInterceptor beforeInterceptor = new BeforeTest.BeforeInterceptor(example);
        AfterTest.AfterInterceptor afterInterceptor = new AfterTest.AfterInterceptor(example);

        Example intercepted1, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(beforeInterceptor, example);
        intercepted = interceptionFactory.createInterceptor(afterInterceptor, intercepted1);

        assertSame( intercepted, intercepted1 );

        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, beforeInterceptor.aCount );
        assertEquals( 1, afterInterceptor.aCount );

    }

    public void testMultipleOverrideCombination2() {
        Example example = new Example();
        BeforeTest.BeforeInterceptor beforeInterceptor = new BeforeTest.BeforeInterceptor(example);
        AfterTest.AfterInterceptor afterInterceptor = new AfterTest.AfterInterceptor(example);

        IExample intercepted1, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(IExample.class, afterInterceptor, example);
        intercepted = interceptionFactory.createInterceptor(IExample.class, beforeInterceptor, intercepted1);

        assertSame( intercepted, intercepted1 );

        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, afterInterceptor.aCount );
        assertEquals( 1, beforeInterceptor.aCount );
    }


    public void testInterceptorCollapsing() throws Throwable {
        Example example = new Example();

        BeforeTest.BeforeInterceptor beforeInterceptor = new BeforeTest.BeforeInterceptor(example);
        AfterTest.AfterInterceptor afterInterceptor = new AfterTest.AfterInterceptor(example);
        AfterTest.AsyncInterceptor asyncInterceptor = new AfterTest.AsyncInterceptor(example);
        ChainTest.ChainInterceptor chainInterceptor = new ChainTest.ChainInterceptor();
        ChainTest.ChainInterceptor chainInterceptor1 = new ChainTest.ChainInterceptor();

        IExample intercepted1, intercepted2, intercepted3, intercepted4, intercepted5;

        intercepted1 = interceptionFactory.createInterceptor(IExample.class, beforeInterceptor, example);
        intercepted2 = interceptionFactory.createInterceptor(IExample.class, chainInterceptor, intercepted1);
        intercepted3 = interceptionFactory.createInterceptor(IExample.class, afterInterceptor, intercepted2);
        intercepted4 = interceptionFactory.createInterceptor(IExample.class, chainInterceptor1, intercepted3);
        intercepted5 = interceptionFactory.createInterceptor(IExample.class, asyncInterceptor, intercepted4);

        assertSame( intercepted1, intercepted2 );
        assertSame( intercepted2, intercepted3 );
        assertNotSame( intercepted3, intercepted4 );
        assertSame( intercepted4, intercepted5 );

        /* All enhanced classes must be combined, but one warning will be printed when multiple chains are used */
        intercepted1 = interceptionFactory.createInterceptor(beforeInterceptor, example);
        intercepted2 = interceptionFactory.createInterceptor(chainInterceptor, intercepted1);
        intercepted3 = interceptionFactory.createInterceptor(afterInterceptor, intercepted2);
        intercepted4 = interceptionFactory.createInterceptor(chainInterceptor1, intercepted3);
        intercepted5 = interceptionFactory.createInterceptor(asyncInterceptor, intercepted4);

        assertSame( intercepted1, intercepted2 );
        assertSame( intercepted2, intercepted3 );
        assertSame( intercepted3, intercepted4 );
        assertSame( intercepted4, intercepted5 );

        logger.warn(interceptionFactory.printInterceptionStack(intercepted5));

        intercepted5.mA();
        intercepted5.mB( 24 );
        intercepted5.mB();
        intercepted5.mC();
    }

}
