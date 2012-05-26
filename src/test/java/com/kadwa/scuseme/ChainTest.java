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

    public static class ChainInterceptor implements Interceptor<IExample> {
        public int aCount = 0;
        public int bCount = 0;
        public int bOverrideCount = 0;
        public int cCount = 0;
        public int cOverrideCount = 0;
        public int noCallCount = 0;

        public IExample interceptedInstance = null;
        public void setInterceptedClass( IExample interceptedInstance ) {
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

    private void runChainTests(IExample intercepted, Example example, ChainInterceptor interceptor) {
        intercepted.mA();intercepted.mA();
        assertSame(example, interceptor.interceptedInstance );
        assertEquals( 2, example.aCount );
        assertEquals( 2, interceptor.aCount );
        intercepted.mB();intercepted.mB();intercepted.mB();
        assertEquals( 3, example.bCount );
        assertEquals( 3, interceptor.bCount );
        intercepted.mB( 20 );
        assertEquals( 0, example.bOverrideCount );
        assertEquals( 1, interceptor.bOverrideCount );
        assertEquals( "Parent", intercepted.mC() );
        assertEquals( 1, example.cCount );
        assertEquals( 1, interceptor.cCount );
        assertEquals( 68, intercepted.mC( "abc" ) );
        assertEquals( 1, example.cOverrideCount );
        assertEquals( 1, interceptor.cOverrideCount );
        assertEquals( 2, example.cCount );
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
        Example example = new Example();

        ChainInterceptor interceptor = new ChainInterceptor();

        IExample intercepted = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        runChainTests( intercepted, example, interceptor );
    }

    public void testChainOverride2() {
        Example example = new Example();

        ChainInterceptor interceptor = new ChainInterceptor();

        Example intercepted = interceptionFactory.createInterceptor(interceptor, example);

        runChainTests( intercepted, example, interceptor );
    }

    public void testMultipleChained() {
        Example example = new Example();

        ChainInterceptor chain1 = new ChainInterceptor();
        ChainInterceptor chain2 = new ChainInterceptor();
        ChainInterceptor chain3 = new ChainInterceptor();

        Example intercepted1, intercepted2, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(chain1, example);
        intercepted2 = interceptionFactory.createInterceptor(chain2, intercepted1);
        intercepted = interceptionFactory.createInterceptor(chain3, intercepted2);

        assertSame( intercepted1, intercepted );
        assertSame( intercepted1, intercepted2 );

        intercepted.mB();
        assertEquals( 1, example.bCount );
        assertEquals( 1, chain1.bCount );
        assertEquals( 1, chain2.bCount );
        assertEquals( 1, chain3.bCount );
        assertEquals( 66, intercepted.mC( "example" ) );
    }

    public void testMultipleChained2() {
        Example example = new Example();

        ChainInterceptor chain1 = new ChainInterceptor();
        ChainInterceptor chain2 = new ChainInterceptor();
        ChainInterceptor chain3 = new ChainInterceptor();

        IExample intercepted1, intercepted2, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(IExample.class, chain1, example);
        intercepted2 = interceptionFactory.createInterceptor(IExample.class, chain2, intercepted1);
        intercepted = interceptionFactory.createInterceptor(IExample.class, chain3, intercepted2);

        assertNotSame( intercepted1, intercepted );
        assertNotSame( intercepted1, intercepted2 );

        intercepted.mB();
        assertEquals( 1, example.bCount );
        assertEquals( 1, chain1.bCount );
        assertEquals( 1, chain2.bCount );
        assertEquals( 1, chain3.bCount );
        assertEquals( 66, intercepted.mC( "example" ) );
    }

    public static class InvocationInterceptorChain implements Interceptor<Example>, InvocationHandler {
        public int aCount = 0;
        public int bCount = 0;
        public int bOverrideCount = 0;
        public int cCount = 0;
        public int cOverrideCount = 0;
        public int noCallCount = 0;

        Example interceptedInstance;
        public void setInterceptedClass( Example interceptedInstance ) {
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

    private void runChainTests(IExample intercepted, Example example, InvocationInterceptorChain interceptor) {
        intercepted.mA();intercepted.mA();
        assertSame(example, interceptor.interceptedInstance );
        assertEquals( 2, example.aCount );
        assertEquals( 2, interceptor.aCount );
        intercepted.mB();intercepted.mB();intercepted.mB();
        assertEquals( 3, example.bCount );
        assertEquals( 3, interceptor.bCount );
        intercepted.mB( 20 );
        assertEquals( 0, example.bOverrideCount );
        assertEquals( 1, interceptor.bOverrideCount );
        assertEquals( "Parent", intercepted.mC() );
        assertEquals( 1, example.cCount );
        assertEquals( 1, interceptor.cCount );
        assertEquals( 68, intercepted.mC( "abc" ) );
        assertEquals( 1, example.cOverrideCount );
        assertEquals( 1, interceptor.cOverrideCount );
        assertEquals( 2, example.cCount );
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
        Example example = new Example();

        InvocationInterceptorChain interceptor = new InvocationInterceptorChain();

        IExample intercepted = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        runChainTests( intercepted, example, interceptor );
    }

    public void testTargetInvocationHandlerChain2() {
        Example example = new Example();

        InvocationInterceptorChain interceptor = new InvocationInterceptorChain();

        Example intercepted = interceptionFactory.createInterceptor(interceptor, example);

        runChainTests( intercepted, example, interceptor );
    }

}
