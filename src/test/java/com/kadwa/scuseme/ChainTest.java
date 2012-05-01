package com.kadwa.scuseme;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public class ChainTest extends TestCase {

    final static Logger logger = LoggerFactory.getLogger(ChainTest.class);

    InterceptionFactory interceptionFactory;

    @Override
    protected void setUp() throws Exception {
        interceptionFactory = new InterceptionFactory();
    }

    public static class ChainInterceptor implements Interceptor<ITest> {
        public int aCount = 0;
        public int bCount = 0;
        public int bOverrideCount = 0;
        public int cCount = 0;
        public int cOverrideCount = 0;
        public int noCallCount = 0;

        public ITest interceptedInstance = null;
        public void setInterceptedClass( ITest interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception( type = Interception.CallType.CHAIN)
        public void mA() { aCount++; this.interceptedInstance.mA(); }
        @Interception(type = Interception.CallType.CHAIN)
        public void mB() { bCount++; this.interceptedInstance.mB(); }
        @Interception( type = Interception.CallType.CHAIN)
        public void mB( int p ) { bOverrideCount++; /* Not chained */ }
        @Interception( type = Interception.CallType.CHAIN)
        public String mC() { cCount++; return this.interceptedInstance.mC(); }
        @Interception( type = Interception.CallType.CHAIN)
        public long mC( String p ) {
            cOverrideCount++;
            this.interceptedInstance.mC(); /* Cross call */
            return this.interceptedInstance.mC( p ) - 1;
        }
        public void noCall() {
            noCallCount++;
        }
        @Interception( type = Interception.CallType.CHAIN)
        public Object noMethod() {
            return this;
        }
    }

    private void runChainTests(ITest intercepted, Test test, ChainInterceptor interceptor) {
        intercepted.mA();intercepted.mA();
        assertSame( test, interceptor.interceptedInstance );
        assertEquals( 2, test.aCount );
        assertEquals( 2, interceptor.aCount );
        intercepted.mB();intercepted.mB();intercepted.mB();
        assertEquals( 3, test.bCount );
        assertEquals( 3, interceptor.bCount );
        intercepted.mB( 20 );
        assertEquals( 0, test.bOverrideCount );
        assertEquals( 1, interceptor.bOverrideCount );
        assertEquals( "Parent", intercepted.mC() );
        assertEquals( 1, test.cCount );
        assertEquals( 1, interceptor.cCount );
        assertEquals( 68, intercepted.mC( "abc" ) );
        assertEquals( 1, test.cOverrideCount );
        assertEquals( 1, interceptor.cOverrideCount );
        assertEquals( 2, test.cCount );
        assertEquals( 1, interceptor.cCount );

        try {
            intercepted.mFail();
            fail( "No Exception Raised" );
        }
        catch (ClassCastException ccex) {
        }
        catch (Throwable ex) {
            fail( "Incorrect Exception: " + ex.getClass() );
        }

        assertEquals( interceptor.noCallCount, 0 );
    }

    public void testChainOverride() {
        Test test = new Test();

        ChainInterceptor interceptor = new ChainInterceptor();

        ITest intercepted = interceptionFactory.createInterceptor(ITest.class, interceptor, test);

        runChainTests( intercepted, test, interceptor );
    }

    public void testChainOverride2() {
        Test test = new Test();

        ChainInterceptor interceptor = new ChainInterceptor();

        Test intercepted = interceptionFactory.createInterceptor(interceptor, test);

        runChainTests( intercepted, test, interceptor );
    }

    public void testMultipleChained() {
        Test test = new Test();

        ChainInterceptor chain1 = new ChainInterceptor();
        ChainInterceptor chain2 = new ChainInterceptor();
        ChainInterceptor chain3 = new ChainInterceptor();

        Test intercepted1, intercepted2, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(chain1, test);
        intercepted2 = interceptionFactory.createInterceptor(chain2, intercepted1);
        intercepted = interceptionFactory.createInterceptor(chain3, intercepted2);

        assertSame( intercepted1, intercepted );
        assertSame( intercepted1, intercepted2 );

        intercepted.mB();
        assertEquals( 1, test.bCount );
        assertEquals( 1, chain1.bCount );
        assertEquals( 1, chain2.bCount );
        assertEquals( 1, chain3.bCount );
        assertEquals( 66, intercepted.mC( "test" ) );
    }

    public void testMultipleChained2() {
        Test test = new Test();

        ChainInterceptor chain1 = new ChainInterceptor();
        ChainInterceptor chain2 = new ChainInterceptor();
        ChainInterceptor chain3 = new ChainInterceptor();

        ITest intercepted1, intercepted2, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(ITest.class, chain1, test);
        intercepted2 = interceptionFactory.createInterceptor(ITest.class, chain2, intercepted1);
        intercepted = interceptionFactory.createInterceptor(ITest.class, chain3, intercepted2);

        assertNotSame( intercepted1, intercepted );
        assertNotSame( intercepted1, intercepted2 );

        intercepted.mB();
        assertEquals( 1, test.bCount );
        assertEquals( 1, chain1.bCount );
        assertEquals( 1, chain2.bCount );
        assertEquals( 1, chain3.bCount );
        assertEquals( 66, intercepted.mC( "test" ) );
    }

    public static class InvocationInterceptorChain implements Interceptor<Test>, InvocationHandler {
        public int aCount = 0;
        public int bCount = 0;
        public int bOverrideCount = 0;
        public int cCount = 0;
        public int cOverrideCount = 0;
        public int noCallCount = 0;

        Test interceptedInstance;
        public void setInterceptedClass( Test interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception( type = Interception.CallType.CHAIN)
        public Object invoke( Object o, Method method, Object[] args ) throws Throwable {
            if (method.getName().equals( "mA" ))
                aCount++;
            else if (method.getName().equals( "mB" ) && method.getParameterTypes().length == 0)
                bCount++;
            else if (method.getName().equals( "mB" ) && method.getParameterTypes().length == 1) {
                bOverrideCount++;
                return null;
            }
            else if (method.getName().equals( "mC" ) && method.getParameterTypes().length == 0)
                cCount++;
            else if (method.getName().equals( "mC" ) && method.getParameterTypes().length == 1) {
                cOverrideCount++;
                this.interceptedInstance.mC(); /* Cross call */
                return ((Long) method.invoke( o, args )) - 1;
            }
            else if (method.getName().equals( "noCall" ))
                noCallCount++;

            return method.invoke( o, args );
        }
    }

    private void runChainTests(ITest intercepted, Test test, InvocationInterceptorChain interceptor) {
        intercepted.mA();intercepted.mA();
        assertSame( test, interceptor.interceptedInstance );
        assertEquals( 2, test.aCount );
        assertEquals( 2, interceptor.aCount );
        intercepted.mB();intercepted.mB();intercepted.mB();
        assertEquals( 3, test.bCount );
        assertEquals( 3, interceptor.bCount );
        intercepted.mB( 20 );
        assertEquals( 0, test.bOverrideCount );
        assertEquals( 1, interceptor.bOverrideCount );
        assertEquals( "Parent", intercepted.mC() );
        assertEquals( 1, test.cCount );
        assertEquals( 1, interceptor.cCount );
        assertEquals( 68, intercepted.mC( "abc" ) );
        assertEquals( 1, test.cOverrideCount );
        assertEquals( 1, interceptor.cOverrideCount );
        assertEquals( 2, test.cCount );
        assertEquals( 1, interceptor.cCount );

        try {
            intercepted.mFail();
            fail( "No Exception Raised" );
        }
        catch (ClassCastException ccex) {
        }
        catch (Throwable ex) {
            fail( "Incorrect Exception: " + ex.getClass() );
        }

        assertEquals( interceptor.noCallCount, 0 );
    }


    public void testTargetInvocationHandlerChain() {
        Test test = new Test();

        InvocationInterceptorChain interceptor = new InvocationInterceptorChain();

        ITest intercepted = interceptionFactory.createInterceptor(ITest.class, interceptor, test);

        runChainTests( intercepted, test, interceptor );
    }

    public void testTargetInvocationHandlerChain2() {
        Test test = new Test();

        InvocationInterceptorChain interceptor = new InvocationInterceptorChain();

        Test intercepted = interceptionFactory.createInterceptor(interceptor, test);

        runChainTests( intercepted, test, interceptor );
    }

}
