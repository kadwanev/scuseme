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
        Test test = new Test();
        BeforeTest.BeforeInterceptor beforeInterceptor = new BeforeTest.BeforeInterceptor(test);
        AfterTest.AfterInterceptor afterInterceptor = new AfterTest.AfterInterceptor(test);

        Test intercepted1, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(beforeInterceptor, test);
        intercepted = interceptionFactory.createInterceptor(afterInterceptor, intercepted1);

        assertSame( intercepted, intercepted1 );

        intercepted.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, beforeInterceptor.aCount );
        assertEquals( 1, afterInterceptor.aCount );

    }

    public void testMultipleOverrideCombination2() {
        Test test = new Test();
        BeforeTest.BeforeInterceptor beforeInterceptor = new BeforeTest.BeforeInterceptor(test);
        AfterTest.AfterInterceptor afterInterceptor = new AfterTest.AfterInterceptor(test);

        ITest intercepted1, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(ITest.class, afterInterceptor, test);
        intercepted = interceptionFactory.createInterceptor(ITest.class, beforeInterceptor, intercepted1);

        assertSame( intercepted, intercepted1 );

        intercepted.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, afterInterceptor.aCount );
        assertEquals( 1, beforeInterceptor.aCount );
    }


    public void testInterceptorCollapsing() throws Throwable {
        Test test = new Test();

        BeforeTest.BeforeInterceptor beforeInterceptor = new BeforeTest.BeforeInterceptor(test);
        AfterTest.AfterInterceptor afterInterceptor = new AfterTest.AfterInterceptor(test);
        AfterTest.AsyncInterceptor asyncInterceptor = new AfterTest.AsyncInterceptor(test);
        ChainTest.ChainInterceptor chainInterceptor = new ChainTest.ChainInterceptor();
        ChainTest.ChainInterceptor chainInterceptor1 = new ChainTest.ChainInterceptor();

        ITest intercepted1, intercepted2, intercepted3, intercepted4, intercepted5;

        intercepted1 = interceptionFactory.createInterceptor(ITest.class, beforeInterceptor, test);
        intercepted2 = interceptionFactory.createInterceptor(ITest.class, chainInterceptor, intercepted1);
        intercepted3 = interceptionFactory.createInterceptor(ITest.class, afterInterceptor, intercepted2);
        intercepted4 = interceptionFactory.createInterceptor(ITest.class, chainInterceptor1, intercepted3);
        intercepted5 = interceptionFactory.createInterceptor(ITest.class, asyncInterceptor, intercepted4);

        assertSame( intercepted1, intercepted2 );
        assertSame( intercepted2, intercepted3 );
        assertNotSame( intercepted3, intercepted4 );
        assertSame( intercepted4, intercepted5 );

        /* All enhanced classes must be combined, but one warning will be printed when multiple chains are used */
        intercepted1 = interceptionFactory.createInterceptor(beforeInterceptor, test);
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
