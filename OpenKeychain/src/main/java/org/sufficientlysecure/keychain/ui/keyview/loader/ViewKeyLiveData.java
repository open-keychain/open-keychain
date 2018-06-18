package org.sufficientlysecure.keychain.ui.keyview.loader;


import java.util.Comparator;
import java.util.List;

import android.content.Context;

import org.sufficientlysecure.keychain.model.KeyMetadata;
import org.sufficientlysecure.keychain.provider.KeyMetadataDao;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao.KeySubkeyStatus;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao.SubKeyItem;
import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactDao.SystemContactInfo;


public class ViewKeyLiveData {
    public static class IdentityLiveData extends AsyncTaskLiveData<List<IdentityInfo>> {
        private final IdentityDao identityDao;

        private final long masterKeyId;
        private final boolean showLinkedIds;

        public IdentityLiveData(Context context, long masterKeyId, boolean showLinkedIds) {
            super(context, KeyRings.buildGenericKeyRingUri(masterKeyId));

            this.identityDao = IdentityDao.getInstance(context);

            this.masterKeyId = masterKeyId;
            this.showLinkedIds = showLinkedIds;
        }

        @Override
        public List<IdentityInfo> asyncLoadData() {
            return identityDao.getIdentityInfos(masterKeyId, showLinkedIds);
        }
    }

    public static class SubkeyStatusLiveData extends AsyncTaskLiveData<KeySubkeyStatus> {
        private final SubkeyStatusDao subkeyStatusDao;

        private final long masterKeyId;
        private final Comparator<SubKeyItem> comparator;

        public SubkeyStatusLiveData(Context context, long masterKeyId, Comparator<SubKeyItem> comparator) {
            super(context, KeyRings.buildGenericKeyRingUri(masterKeyId));

            this.subkeyStatusDao = SubkeyStatusDao.getInstance(context);

            this.masterKeyId = masterKeyId;
            this.comparator = comparator;
        }

        @Override
        public KeySubkeyStatus asyncLoadData() {
            return subkeyStatusDao.getSubkeyStatus(masterKeyId, comparator);
        }
    }

    public static class SystemContactInfoLiveData extends AsyncTaskLiveData<SystemContactInfo> {
        private final SystemContactDao systemContactDao;

        private final long masterKeyId;
        private final boolean isSecret;

        public SystemContactInfoLiveData(Context context, long masterKeyId, boolean isSecret) {
            super(context, null);

            this.systemContactDao = SystemContactDao.getInstance(context);

            this.masterKeyId = masterKeyId;
            this.isSecret = isSecret;
        }

        @Override
        public SystemContactInfo asyncLoadData() {
            return systemContactDao.getSystemContactInfo(masterKeyId, isSecret);
        }
    }

    public static class KeyserverStatusLiveData extends AsyncTaskLiveData<KeyMetadata> {
        private final KeyMetadataDao keyMetadataDao;

        private final long masterKeyId;

        public KeyserverStatusLiveData(Context context, long masterKeyId) {
            super(context, KeyRings.buildGenericKeyRingUri(masterKeyId));

            this.keyMetadataDao = KeyMetadataDao.create(context);
            this.masterKeyId = masterKeyId;
        }

        @Override
        public KeyMetadata asyncLoadData() {
            return keyMetadataDao.getKeyMetadata(masterKeyId);
        }
    }
}
