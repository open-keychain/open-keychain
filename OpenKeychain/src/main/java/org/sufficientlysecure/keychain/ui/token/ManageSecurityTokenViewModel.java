package org.sufficientlysecure.keychain.ui.token;


import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.net.Uri;

import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.ui.token.PublicKeyRetriever.KeyRetrievalResult;


public class ManageSecurityTokenViewModel extends ViewModel {
    private static final long MIN_OPERATION_TIME_MILLIS = 700;

    SecurityTokenInfo tokenInfo;

    private GenericLiveData<KeyRetrievalResult> keyRetrievalLocal;
    private GenericLiveData<KeyRetrievalResult> keyRetrievalUri;
    private GenericLiveData<KeyRetrievalResult> keyRetrievalKeyserver;
    private GenericLiveData<KeyRetrievalResult> keyRetrievalContentUri;

    private PublicKeyRetriever publicKeyRetriever;

    void setTokenInfo(Context context, SecurityTokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
        this.publicKeyRetriever = new PublicKeyRetriever(context, tokenInfo);
    }

    public LiveData<KeyRetrievalResult> getKeyRetrievalLocal(Context context) {
        if (keyRetrievalLocal == null) {
            keyRetrievalLocal = new GenericLiveData<>(context, publicKeyRetriever::retrieveLocal);
            keyRetrievalLocal.setMinLoadTime(MIN_OPERATION_TIME_MILLIS);
        }
        return keyRetrievalLocal;
    }

    public LiveData<KeyRetrievalResult> getKeyRetrievalUri(Context context) {
        if (keyRetrievalUri == null) {
            keyRetrievalUri = new GenericLiveData<>(context, publicKeyRetriever::retrieveUri);
            keyRetrievalUri.setMinLoadTime(MIN_OPERATION_TIME_MILLIS);
        }
        return keyRetrievalUri;
    }

    public LiveData<KeyRetrievalResult> getKeyRetrievalKeyserver(Context context) {
        if (keyRetrievalKeyserver == null) {
            keyRetrievalKeyserver = new GenericLiveData<>(context, publicKeyRetriever::retrieveKeyserver);
            keyRetrievalKeyserver.setMinLoadTime(MIN_OPERATION_TIME_MILLIS);
        }
        return keyRetrievalKeyserver;
    }

    public LiveData<KeyRetrievalResult> getKeyRetrievalContentUri(Context context, Uri uri) {
        if (keyRetrievalContentUri == null) {
            keyRetrievalContentUri = new GenericLiveData<>(context, () -> publicKeyRetriever.retrieveContentUri(uri));
            keyRetrievalContentUri.setMinLoadTime(MIN_OPERATION_TIME_MILLIS);
        }
        return keyRetrievalContentUri;
    }

    public void resetLiveData(LifecycleOwner lifecycleOwner) {
        if (keyRetrievalLocal != null) {
            keyRetrievalLocal.removeObservers(lifecycleOwner);
            keyRetrievalLocal = null;
        }
        if (keyRetrievalKeyserver != null) {
            keyRetrievalKeyserver.removeObservers(lifecycleOwner);
            keyRetrievalKeyserver = null;
        }
        if (keyRetrievalUri != null) {
            keyRetrievalUri.removeObservers(lifecycleOwner);
            keyRetrievalUri = null;
        }
        if (keyRetrievalContentUri != null) {
            keyRetrievalContentUri.removeObservers(lifecycleOwner);
            keyRetrievalContentUri = null;
        }
    }
}
