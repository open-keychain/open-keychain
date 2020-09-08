package org.sufficientlysecure.keychain.ui.keyview;


import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import android.content.Context;

import org.sufficientlysecure.keychain.daos.KeyMetadataDao;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.KeyMetadata;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao.KeySubkeyStatus;


public class KeyFragmentViewModel extends ViewModel {
    private LiveData<List<IdentityInfo>> identityInfo;
    private LiveData<KeySubkeyStatus> subkeyStatus;
    private LiveData<KeyMetadata> keyserverStatus;

    LiveData<List<IdentityInfo>> getIdentityInfo(Context context, LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData) {
        if (identityInfo == null) {
            IdentityDao identityDao = IdentityDao.getInstance(context);
            identityInfo = Transformations.switchMap(unifiedKeyInfoLiveData,
                    (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                            () -> identityDao.getIdentityInfos(unifiedKeyInfo.master_key_id())));
        }
        return identityInfo;
    }

    LiveData<KeySubkeyStatus> getSubkeyStatus(Context context, LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData) {
        if (subkeyStatus == null) {
            SubkeyStatusDao subkeyStatusDao = SubkeyStatusDao.getInstance(context);
            subkeyStatus = Transformations.switchMap(unifiedKeyInfoLiveData,
                    (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                            () -> subkeyStatusDao.getSubkeyStatus(unifiedKeyInfo.master_key_id())));
        }
        return subkeyStatus;
    }

    LiveData<KeyMetadata> getKeyserverStatus(Context context, LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData) {
        if (keyserverStatus == null) {
            KeyMetadataDao keyMetadataDao = KeyMetadataDao.create(context);
            keyserverStatus = Transformations.switchMap(unifiedKeyInfoLiveData,
                    (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                            () -> keyMetadataDao.getKeyMetadata(unifiedKeyInfo.master_key_id())));
        }
        return keyserverStatus;
    }
}
