package org.thialfihar.android.apg.service;

interface IApgEncryptDecryptHandler {
    /**
     * Either output or streamUri is given. One of them is null
     *
     */
    oneway void onSuccessEncrypt(in byte[] outputBytes, in String outputUri);

    oneway void onSuccessDecrypt(in byte[] outputBytes, in String outputUri, in boolean signature,
            in long signatureKeyId, in String signatureUserId, in boolean signatureSuccess,
            in boolean signatureUnknown);
    
    
    oneway void onException(in int exceptionNumber, in String message);
}