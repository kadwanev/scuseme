package com.kadwa.scuseme.ext;

import com.kadwa.scuseme.Interception;
import com.kadwa.scuseme.InterceptionCapability;

/**
 * Created by Neville Kadwa.
 */
public class RetryCapability implements InterceptionCapability<Retry> {

    @Override
    public Class<Retry> getAnnotation() {
        return Retry.class;
    }

    @Override
    public Interception.CallType getCallType(Retry retry) {
        return (retry.attempts() > 0) ? Interception.CallType.CHAIN : null;
    }



}
