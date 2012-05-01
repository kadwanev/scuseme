package com.kadwa.scuseme;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public class BeforeTest extends TestCase {

    final static Logger logger = LoggerFactory.getLogger(BeforeTest.class);

    InterceptionFactory interceptionFactory;

    @Override
    protected void setUp() throws Exception {
        interceptionFactory = new InterceptionFactory();
    }

    public static class BeforeInterceptor implements Interceptor<ITest> {
        public int aCount = 0;
        public int testACount;
        public int cCount = 0;

        public Test test;
        public BeforeInterceptor(Test test) {
            this.test = test;
        }

        public ITest interceptedInstance = null;
        public void setInterceptedClass( ITest interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.BEFORE)
        public void mA() {
            aCount++;
            this.testACount = test.aCount;
        }

        @Interception(type = Interception.CallType.BEFORE)
        public String mC() {
            cCount++;
            return "Befored";
        }
    }

    public void testBeforeOverride() {
        Test test = new Test();

        BeforeInterceptor interceptor = new BeforeInterceptor(test);

        ITest intercepted = interceptionFactory.createInterceptor(ITest.class, interceptor, test);

        assertEquals( 0, test.aCount );
        assertEquals( 0, interceptor.aCount );
        intercepted.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.aCount );
        assertEquals( 0, interceptor.testACount );
        intercepted.mA();
        assertEquals( 2, test.aCount );
        assertEquals( 2, interceptor.aCount );
        assertEquals( 1, interceptor.testACount );
        assertEquals( "Parent", intercepted.mC() );
        assertEquals( 1, test.cCount );
        assertEquals( 1, interceptor.cCount );
    }

    public static class RenameInterceptor implements Interceptor<Test> {
        public int aCount = 0;
        public int argsCount = 0;

        public Test test;
        public RenameInterceptor(Test test) {
            this.test = test;
        }

        public Test interceptedInstance = null;
        public void setInterceptedClass( Test interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.BEFORE, mName = "mA")
        public void mADifferent() {
            aCount++;
        }

    }

    public void testMethodRename() {
        Test test = new Test();

        RenameInterceptor interceptor = new RenameInterceptor(test);

        ITest interceped = interceptionFactory.createInterceptor(interceptor, test);

        interceped.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.aCount );
    }

    public static class InvocationInterceptorBefore implements Interceptor<Test>, InvocationHandler {
        public int count = 0;
        public int instanceACount = 0;

        Test instance;
        public void setInterceptedClass( Test interceptedInstance ) {
            this.instance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.BEFORE)
        public Object invoke( Object o, Method method, Object[] objects ) throws Throwable {
            count++;
            instanceACount = this.instance.aCount;
            return null;
        }
    }

    public void testTargetInvocationHandlerBefore() {
        Test test = new Test();

        InvocationInterceptorBefore interceptor = new InvocationInterceptorBefore();

        ITest interceped = interceptionFactory.createInterceptor(interceptor, test);

        interceped.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 0, interceptor.instanceACount );
        interceped.mB();
        assertEquals( 1, test.aCount );
        assertEquals( 2, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        interceped.mA();
        assertEquals( 2, test.aCount );
        assertEquals( 3, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
    }

    public void testTargetInvocationHandlerBefore2() {
        Test test = new Test();

        InvocationInterceptorBefore interceptor = new InvocationInterceptorBefore();

        ITest interceped = interceptionFactory.createInterceptor(ITest.class, interceptor, test);

        interceped.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 0, interceptor.instanceACount );
        interceped.mB();
        assertEquals( 1, test.aCount );
        assertEquals( 2, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        interceped.mA();
        assertEquals( 2, test.aCount );
        assertEquals( 3, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
    }

}

