package org.thialfihar.android.apg.service;

interface IApgSignVerifyHandler {
    oneway void onSuccessSign(in byte[] outputBytes, in String outputUri);
    
    oneway void onSuccessVerify(in boolean signature, in long signatureKeyId,
            in String signatureUserId, in boolean signatureSuccess, in boolean signatureUnknown);
    
    
    oneway void onException(in int exceptionNumber, in String message);
}