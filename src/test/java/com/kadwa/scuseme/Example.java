package com.kadwa.scuseme;

/**
 * Created by Neville Kadwa.
 */
public class Example implements IExample {
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
    public void mFail() { Integer i = (Integer) ((Object)"example"); /* Throw ClassCastException */ }
}
