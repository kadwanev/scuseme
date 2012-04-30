package com.kadwa.scuseme;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by Neville Kadwa.
 */
public class InterceptionFactoryTest extends TestCase {

    final static Logger logger = LoggerFactory.getLogger(InterceptionFactoryTest.class);

    InterceptionFactory interceptionFactory;

    @Override
    protected void setUp() throws Exception {
        interceptionFactory = new InterceptionFactory();
    }

    public static interface ITest {
        public void mA();
        public void mB();
        public void mB(int p);
        public String mC();
        public long mC(String p);
        public String mArgs(String a1, String a2, int a3, String a4);
        public void mFail();
    }

    public static class Test implements ITest {
        public int aCount = 0;
        public int bCount = 0;
        public int bOverrideCount = 0;
        public int cCount = 0;
        public int cOverrideCount = 0;
        public int argsCount = 0;

        public void mA() { aCount++; }
        public void mB() { bCount++; }
        public void mB( int p ) { bOverrideCount++; }
        public String mC() { cCount++; return "Parent"; }
        public long mC( String p ) { cOverrideCount++; return 69; }
        public String mArgs(String a1, String a2, int a3, String a4) { argsCount++; return "mArgs"; }
        public void mFail() { Integer i = (Integer) ((Object)"test"); /* Throw ClassCastException */ }
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

    public void testObject() {
        Test test = new Test();

        ChainInterceptor interceptor = new ChainInterceptor();

        Test intercepted = interceptionFactory.createInterceptor(interceptor, test);

        runChainTests(intercepted, test, interceptor);
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

    public void testBeforeAfterOverride() {
        {
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

        {
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

    public void testMultipleOverrideCombination() {
        Test test = new Test();
        BeforeInterceptor beforeInterceptor = new BeforeInterceptor(test);
        AfterInterceptor afterInterceptor = new AfterInterceptor(test);

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
        BeforeInterceptor beforeInterceptor = new BeforeInterceptor(test);
        AfterInterceptor afterInterceptor = new AfterInterceptor(test);

        ITest intercepted1, intercepted;

        intercepted1 = interceptionFactory.createInterceptor(ITest.class, afterInterceptor, test);
        intercepted = interceptionFactory.createInterceptor(ITest.class, beforeInterceptor, intercepted1);

        assertSame( intercepted, intercepted1 );

        intercepted.mA();
        assertEquals( 1, test.aCount );
        assertEquals( 1, afterInterceptor.aCount );
        assertEquals( 1, beforeInterceptor.aCount );
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

    public void testInterceptorCollapsing() throws Throwable {
        Test test = new Test();

        BeforeInterceptor beforeInterceptor = new BeforeInterceptor( test );
        AfterInterceptor afterInterceptor = new AfterInterceptor( test );
        AsyncInterceptor asyncInterceptor = new AsyncInterceptor( test );
        ChainInterceptor chainInterceptor = new ChainInterceptor();
        ChainInterceptor chainInterceptor1 = new ChainInterceptor();

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
