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

    public static class BeforeInterceptor implements Interceptor<IExample> {
        public int aCount = 0;
        public int testACount;
        public int cCount = 0;

        public Example example;
        public BeforeInterceptor(Example example) {
            this.example = example;
        }

        public IExample interceptedInstance = null;
        public void setInterceptedClass( IExample interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.BEFORE)
        public void mA() {
            aCount++;
            this.testACount = example.aCount;
        }

        @Interception(type = Interception.CallType.BEFORE)
        public String mC() {
            cCount++;
            return "Befored";
        }
    }

    public void testBeforeOverride() {
        Example example = new Example();

        BeforeInterceptor interceptor = new BeforeInterceptor(example);

        IExample intercepted = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        assertEquals( 0, example.aCount );
        assertEquals( 0, interceptor.aCount );
        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.aCount );
        assertEquals( 0, interceptor.testACount );
        intercepted.mA();
        assertEquals( 2, example.aCount );
        assertEquals( 2, interceptor.aCount );
        assertEquals( 1, interceptor.testACount );
        assertEquals( "Parent", intercepted.mC() );
        assertEquals( 1, example.cCount );
        assertEquals( 1, interceptor.cCount );
    }

    public static class RenameInterceptor implements Interceptor<Example> {
        public int aCount = 0;
        public int argsCount = 0;

        public Example example;
        public RenameInterceptor(Example example) {
            this.example = example;
        }

        public Example interceptedInstance = null;
        public void setInterceptedClass( Example interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.BEFORE, mName = "mA")
        public void mADifferent() {
            aCount++;
        }

    }

    public void testMethodRename() {
        Example example = new Example();

        RenameInterceptor interceptor = new RenameInterceptor(example);

        IExample interceped = interceptionFactory.createInterceptor(interceptor, example);

        interceped.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.aCount );
    }

    public static class InvocationInterceptorBefore implements Interceptor<Example>, InvocationHandler {
        public int count = 0;
        public int instanceACount = 0;

        Example instance;
        public void setInterceptedClass( Example interceptedInstance ) {
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
        Example example = new Example();

        InvocationInterceptorBefore interceptor = new InvocationInterceptorBefore();

        IExample intercepted = interceptionFactory.createInterceptor(interceptor, example);

        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 0, interceptor.instanceACount );
        intercepted.mB();
        assertEquals( 1, example.aCount );
        assertEquals( 2, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        intercepted.mA();
        assertEquals( 2, example.aCount );
        assertEquals( 3, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
    }

    public void testTargetInvocationHandlerBefore2() {
        Example example = new Example();

        InvocationInterceptorBefore interceptor = new InvocationInterceptorBefore();

        IExample intercepted = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        intercepted.mA();
        assertEquals( 1, example.aCount );
        assertEquals( 1, interceptor.count );
        assertEquals( 0, interceptor.instanceACount );
        intercepted.mB();
        assertEquals( 1, example.aCount );
        assertEquals( 2, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
        intercepted.mA();
        assertEquals( 2, example.aCount );
        assertEquals( 3, interceptor.count );
        assertEquals( 1, interceptor.instanceACount );
    }

    public static class BeforeReturnableInterceptor implements Interceptor<Example> {
        public Example example;
        public BeforeReturnableInterceptor(Example example) {
            this.example = example;
        }

        public Example interceptedInstance = null;
        public void setInterceptedClass( Example interceptedInstance ) {
            this.interceptedInstance = interceptedInstance;
        }

        @Interception(type = Interception.CallType.BEFORE, before = Interception.BeforeMode.RETURNABLE)
        public String mC() {
            return "Cached";
        }

    }

    public void testBeforeReturnable() throws Exception {
        Example example = new Example();

        BeforeReturnableInterceptor interceptor = new BeforeReturnableInterceptor(example);

        IExample intercepted = interceptionFactory.createInterceptor(IExample.class, interceptor, example);

        assertEquals("Cached", intercepted.mC());
    }
}

