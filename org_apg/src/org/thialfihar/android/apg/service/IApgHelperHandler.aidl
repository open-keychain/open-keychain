package org.thialfihar.android.apg.service;

interface IApgHelperHandler {

    oneway void onSuccessGetDecryptionKey(in long secretKeyId, in boolean symmetric);
    
    
    oneway void onException(in int exceptionNumber, in String message);
}