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

    public static class AfterInterceptor implements Interceptor<ITest> {
        public int aCount = 0;
        public int testACount;
        public int bOverrideCount = 0;
        public int bOverrideParam = 0;
        public int cCount = 0;
        public String cParam = null;

        public Test test;
        public AfterInterceptor(Test test) {
            this.test = test;
        }

        public ITest interceptedInstance = null;
        public void setInterceptedClass( ITest interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.AFTER)
        public void mA() {
            aCount++;
            this.testACount = test.aCount;
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

    public static class AsyncInterceptor implements Interceptor<ITest> {
        public int aCount = 0;
        public int testACount;

        public Test test;
        public AsyncInterceptor(Test test) {
            this.test = test;
        }

        public ITest interceptedInstance = null;
        public void setInterceptedClass( ITest interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.ASYNC, delay = 1000)
        public void mA() {
            aCount++;
            this.testACount = test.aCount;
        }
    }

    public void testBeforeAfterOverride() {
        Test test = new Test();

        AfterInterceptor interceptor = new AfterInterceptor(test);

        ITest intercepted = interceptionFactory.createInterceptor(ITest.class, interceptor, test);

        assertEquals( 0, test.aCount );
        assertEquals( 0, interceptor.aCount );
        intercepted.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.aCount );
        assertEquals( 1, interceptor.testACount );
        intercepted.mA();
        assertEquals( 2, test.aCount );
        assertEquals( 2, interceptor.aCount );
        assertEquals( 2, interceptor.testACount );
        assertEquals( "Parent", intercepted.mC() );
        assertEquals( "Parent", interceptor.cParam );
        assertEquals( 1, test.cCount );
        assertEquals( 1, interceptor.cCount );
        intercepted.mB( 20 );
        assertEquals( 1, test.bOverrideCount );
        assertEquals( 1, interceptor.bOverrideCount );
        assertEquals( 20, interceptor.bOverrideParam );
    }

    public void testAsync() {
        Test test = new Test();

        AsyncInterceptor interceptor = new AsyncInterceptor(test);

        Test intercepted = interceptionFactory.createInterceptor(interceptor, test);

        assertEquals( 0, test.aCount );
        assertEquals( 0, interceptor.aCount );
        intercepted.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 0, interceptor.aCount );
        assertEquals( 0, interceptor.testACount );
        try { Thread.sleep( 2000 ); } catch ( Exception ex ) { }

        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.aCount );
        assertEquals( 1, interceptor.testACount );

    }

    public static class InvocationInterceptorAfter implements Interceptor<Test>, InvocationHandler {
        public int count = 0;
        public int instanceACount = 0;

        Test instance;
        public void setInterceptedClass( Test interceptedInstance ) {
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
        Test test = new Test();

        InvocationInterceptorAfter interceptor = new InvocationInterceptorAfter();

        ITest intercepted = interceptionFactory.createInterceptor(interceptor, test);

        intercepted.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        intercepted.mB();
        assertEquals( 1, test.aCount );
        assertEquals( 2, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        intercepted.mA();
        assertEquals( 2, test.aCount );
        assertEquals( 3, interceptor.count );
        assertEquals( 2, interceptor.instanceACount );
    }

    public void testTargetInvocationHandlerAfter2() {
        Test test = new Test();

        InvocationInterceptorAfter interceptor = new InvocationInterceptorAfter();

        ITest interceped = interceptionFactory.createInterceptor(ITest.class, interceptor, test);

        interceped.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        interceped.mB();
        assertEquals( 1, test.aCount );
        assertEquals( 2, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        interceped.mA();
        assertEquals( 2, test.aCount );
        assertEquals( 3, interceptor.count );
        assertEquals( 2, interceptor.instanceACount );
    }

    public static class InvocationInterceptorAsync implements Interceptor<Test>, InvocationHandler {
        public int count = 0;
        public int instanceACount = 0;

        Test instance;
        public void setInterceptedClass( Test interceptedInstance ) {
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
        Test test = new Test();

        InvocationInterceptorAsync interceptor = new InvocationInterceptorAsync();

        ITest intercepted = interceptionFactory.createInterceptor(ITest.class, interceptor, test);

        intercepted.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 0, interceptor.count );
        assertEquals( 0, interceptor.instanceACount );
        try { Thread.sleep( 2000 ); } catch ( Exception ex ) { }

        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
    }

    public static class AfterTypeInterceptor implements Interceptor<ITest> {
        public int cCount = 0;
        public int failCount = 0;
        public Throwable failException;
        public int cOverrideCount = 0;
        public Throwable cOverrideException;

        public Test test;
        public AfterTypeInterceptor(Test test) {
            this.test = test;
        }

        public ITest interceptedInstance = null;
        public void setInterceptedClass( ITest interceptedInstance ) {
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
        Test test = new Test();

        AfterTypeInterceptor interceptor = new AfterTypeInterceptor(test);

        ITest intercepted = interceptionFactory.createInterceptor(ITest.class, interceptor, test);

        intercepted.mC();
        assertEquals( 1, test.cCount );
        assertEquals( 1, interceptor.cCount );
        assertEquals( 69, intercepted.mC("loof"));
        assertEquals( 1, test.cOverrideCount );
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
