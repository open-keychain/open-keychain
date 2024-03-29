package org.sufficientlysecure.keychain.ui.keyview;


import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import org.sufficientlysecure.keychain.daos.DatabaseNotifyManager;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.UnifiedKeyInfo;


public class UnifiedKeyInfoViewModel extends ViewModel {
    private Long masterKeyId;
    private LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData;

    public void setMasterKeyId(long masterKeyId) {
        if (this.masterKeyId != null && this.masterKeyId != masterKeyId) {
            throw new IllegalStateException("cannot change masterKeyId once set!");
        }
        this.masterKeyId = masterKeyId;
    }

    public long getMasterKeyId() {
        return masterKeyId;
    }

    public LiveData<UnifiedKeyInfo> getUnifiedKeyInfoLiveData(Context context) {
        if (masterKeyId == null) {
            throw new IllegalStateException("masterKeyId must be set to retrieve this!");
        }
        if (unifiedKeyInfoLiveData == null) {
            KeyRepository keyRepository = KeyRepository.create(context);
            Uri notifyUri = DatabaseNotifyManager.getNotifyUriMasterKeyId(masterKeyId);
            unifiedKeyInfoLiveData = new GenericLiveData<>(context, notifyUri,
                    () -> keyRepository.getUnifiedKeyInfo(masterKeyId));
        }
        return unifiedKeyInfoLiveData;
    }
}
