package com.kadwa.scuseme;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public class AfterTest extends TestCase {

    final static Logger logger = LoggerFactory.getLogger(AfterTest.class);

    InterceptionFactory interceptionFactory;

    @Override
    protected void setUp() throws Exception {
        interceptionFactory = new InterceptionFactory();
    }

    public static class AfterInterceptor implements Interceptor<IExample> {
        public int aCount = 0;
        public int testACount;
        public int bOverrideCount = 0;
        public int bOverrideParam = 0;
        public int cCount = 0;
        public String cParam = null;

        public Example example;
        public AfterInterceptor(Example example) {
            this.example = example;
        }

        public IExample interceptedInstance = null;
        public void setInterceptedClass( IExample interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.AFTER)
        public void mA() {
            aCount++;
            this.testACount = example.aCount;
        }

        @Interception(type = Interception.CallType.AFTER)
        public void mB(int p) {
            bOverrideParam = p;
            bOverrideCount++;
        }

        @Interception(type = Interception.CallType.AFTER)
        public String mC(String returned) {
            cCount++;
            cParam = returned;
            return null;
        }
    }

    public static class AsyncInterceptor implements Interceptor<IExample> {
        public int aCount = 0;
        public int testACount;

        public Example example;
        public AsyncInterceptor(Example example) {
            this.example = example;
        }

        public IExample interceptedInstance = null;
        public void setInterceptedClass( IExample interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.ASYNC, delay = 1000)
        public void mA() {
            aCount++;
            this.testACount = example.aCount;
        }
    }

    public void testBeforeAfterOverride() {
        Example example = new Example();

        AfterInterceptor interceptor = new AfterInterceptor(example);

        IExample intercepted = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        assertEquals( 0, example.aCount );
        assertEquals( 0, interceptor.aCount );
        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.aCount );
        assertEquals( 1, interceptor.testACount );
        intercepted.mA();
        assertEquals( 2, example.aCount );
        assertEquals( 2, interceptor.aCount );
        assertEquals( 2, interceptor.testACount );
        assertEquals( "Parent", intercepted.mC() );
        assertEquals( "Parent", interceptor.cParam );
        assertEquals( 1, example.cCount );
        assertEquals( 1, interceptor.cCount );
        intercepted.mB( 20 );
        assertEquals( 1, example.bOverrideCount );
        assertEquals( 1, interceptor.bOverrideCount );
        assertEquals( 20, interceptor.bOverrideParam );
    }

    public void testAsync() {
        Example example = new Example();

        AsyncInterceptor interceptor = new AsyncInterceptor(example);

        Example intercepted = interceptionFactory.createInterceptor(interceptor, example);

        assertEquals( 0, example.aCount );
        assertEquals( 0, interceptor.aCount );
        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 0, interceptor.aCount );
        assertEquals( 0, interceptor.testACount );
        try { Thread.sleep( 2000 ); } catch ( Exception ex ) { }

        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.aCount );
        assertEquals( 1, interceptor.testACount );

    }

    public static class InvocationInterceptorAfter implements Interceptor<Example>, InvocationHandler {
        public int count = 0;
        public int instanceACount = 0;

        Example instance;
        public void setInterceptedClass( Example interceptedInstance ) {
            this.instance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.AFTER)
        public Object invoke( Object o, Method method, Object[] objects ) throws Throwable {
            count++;
            instanceACount = this.instance.aCount;
            return null;
        }
    }

    public void testTargetInvocationHandlerAfter() {
        Example example = new Example();

        InvocationInterceptorAfter interceptor = new InvocationInterceptorAfter();

        IExample intercepted = interceptionFactory.createInterceptor(interceptor, example);

        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        intercepted.mB();
        assertEquals( 1, example.aCount );
        assertEquals( 2, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        intercepted.mA();
        assertEquals( 2, example.aCount );
        assertEquals( 3, interceptor.count );
        assertEquals( 2, interceptor.instanceACount );
    }

    public void testTargetInvocationHandlerAfter2() {
        Example example = new Example();

        InvocationInterceptorAfter interceptor = new InvocationInterceptorAfter();

        IExample interceped = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        interceped.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        interceped.mB();
        assertEquals( 1, example.aCount );
        assertEquals( 2, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        interceped.mA();
        assertEquals( 2, example.aCount );
        assertEquals( 3, interceptor.count );
        assertEquals( 2, interceptor.instanceACount );
    }

    public static class InvocationInterceptorAsync implements Interceptor<Example>, InvocationHandler {
        public int count = 0;
        public int instanceACount = 0;

        Example instance;
        public void setInterceptedClass( Example interceptedInstance ) {
            this.instance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.ASYNC, delay = 1000)
        public Object invoke( Object o, Method method, Object[] objects ) throws Throwable {
            count++;
            instanceACount = this.instance.aCount;
            return null;
        }
    }

    public void testTargetInvocationHandlerAsync() {
        Example example = new Example();

        InvocationInterceptorAsync interceptor = new InvocationInterceptorAsync();

        IExample intercepted = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 0, interceptor.count );
        assertEquals( 0, interceptor.instanceACount );
        try { Thread.sleep( 2000 ); } catch ( Exception ex ) { }

        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
    }

    public static class AfterTypeInterceptor implements Interceptor<IExample> {
        public int cCount = 0;
        public int failCount = 0;
        public Throwable failException;
        public int cOverrideCount = 0;
        public Throwable cOverrideException;

        public Example example;
        public AfterTypeInterceptor(Example example) {
            this.example = example;
        }

        public IExample interceptedInstance = null;
        public void setInterceptedClass( IExample interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.AFTER, after = Interception.AfterMode.NONE)
        public String mC() {
            cCount++;
            return null;
        }

        @Interception(type = Interception.CallType.AFTER, after = Interception.AfterMode.RETURN_EXCEPTION)
        public void mFail(Throwable throwable) {
            failCount++;
            failException = throwable;
        }

        @Interception(type = Interception.CallType.AFTER, after = Interception.AfterMode.RETURN_EXCEPTION)
        public long mC( long returned, Throwable throwable, String p ) {
            cOverrideCount++;
            cOverrideException = throwable;
            return returned;
        }
    }

    public void testAfterType() throws Exception {
        Example example = new Example();

        AfterTypeInterceptor interceptor = new AfterTypeInterceptor(example);

        IExample intercepted = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        intercepted.mC();
        assertEquals( 1, example.cCount );
        assertEquals( 1, interceptor.cCount );
        assertEquals( 69, intercepted.mC("loof"));
        assertEquals( 1, example.cOverrideCount );
        assertEquals( 1, interceptor.cOverrideCount );
        assertNull(interceptor.cOverrideException);
        assertNull(interceptor.failException);
        try {
            intercepted.mFail();
            fail("call should fail");
        }
        catch (Throwable ex) {
        }
        assertNotNull(interceptor.failException);
        assertEquals(1, interceptor.failCount);

    }

    public void testAfterType2() throws Exception {
        Example example = new Example();

        AfterTypeInterceptor interceptor = new AfterTypeInterceptor(example);

        Example intercepted = interceptionFactory.createInterceptor(interceptor, example);

        intercepted.mC();
        assertEquals( 1, example.cCount );
        assertEquals( 1, interceptor.cCount );
        assertEquals( 69, intercepted.mC("loof"));
        assertEquals( 1, example.cOverrideCount );
        assertEquals( 1, interceptor.cOverrideCount );
        assertNull(interceptor.cOverrideException);
        assertNull(interceptor.failException);
        try {
            intercepted.mFail();
            fail("call should fail");
        }
        catch (Throwable ex) {
        }
        assertNotNull(interceptor.failException);
        assertEquals(1, interceptor.failCount);

    }

}
