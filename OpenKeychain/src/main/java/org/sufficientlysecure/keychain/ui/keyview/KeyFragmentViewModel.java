package org.sufficientlysecure.keychain.ui.keyview;


import java.util.List;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.content.Context;

import org.sufficientlysecure.keychain.daos.KeyMetadataDao;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.KeyMetadata;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao.KeySubkeyStatus;
import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactDao;
import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactDao.SystemContactInfo;


public class KeyFragmentViewModel extends ViewModel {
    private LiveData<List<IdentityInfo>> identityInfo;
    private LiveData<KeySubkeyStatus> subkeyStatus;
    private LiveData<SystemContactInfo> systemContactInfo;
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

    LiveData<SystemContactInfo> getSystemContactInfo(Context context, LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData) {
        if (systemContactInfo == null) {
            SystemContactDao systemContactDao = SystemContactDao.getInstance(context);
            systemContactInfo = Transformations.switchMap(unifiedKeyInfoLiveData,
                    (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                            () -> systemContactDao.getSystemContactInfo(unifiedKeyInfo.master_key_id(),
                                    unifiedKeyInfo.has_any_secret())));
        }
        return systemContactInfo;
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
