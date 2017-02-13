/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.ui.widget.KeyStatusList.KeyDisplayStatus;
import org.sufficientlysecure.keychain.ui.widget.SubkeyStatusLoader.KeySubkeyStatus;
import org.sufficientlysecure.keychain.ui.widget.SubkeyStatusLoader.SubKeyItem;


public class KeyStatusPresenter implements LoaderCallbacks<KeySubkeyStatus> {
    private final Context context;
    private final KeyStatusMvpView view;
    private final int loaderId;

    private final long masterKeyId;


    public KeyStatusPresenter(Context context, KeyStatusMvpView view, int loaderId, long masterKeyId) {
        this.context = context;
        this.view = view;
        this.loaderId = loaderId;

        this.masterKeyId = masterKeyId;
    }

    public void startLoader(LoaderManager loaderManager) {
        loaderManager.restartLoader(loaderId, null, this);
    }

    @Override
    public Loader<KeySubkeyStatus> onCreateLoader(int id, Bundle args) {
        return new SubkeyStatusLoader(context, context.getContentResolver(), masterKeyId, KeyHealthPresenter.SUBKEY_COMPARATOR);
    }

    @Override
    public void onLoadFinished(Loader<KeySubkeyStatus> loader, KeySubkeyStatus subkeyStatus) {
        SubKeyItem bestCertify = subkeyStatus.keyCertify;
        KeyDisplayStatus certStatus = getKeyDisplayStatus(bestCertify);
        view.setCertifyStatus(certStatus);

        SubKeyItem bestSign = subkeyStatus.keysSign.isEmpty() ? null : subkeyStatus.keysSign.get(0);
        KeyDisplayStatus signStatus =
                bestSign == null ? KeyDisplayStatus.STRIPPED : getKeyDisplayStatus(bestSign);
        view.setSignStatus(signStatus);

        SubKeyItem bestEncrypt = subkeyStatus.keysEncrypt.isEmpty() ? null : subkeyStatus.keysEncrypt.get(0);
        KeyDisplayStatus encryptStatus =
                bestEncrypt == null ? KeyDisplayStatus.STRIPPED : getKeyDisplayStatus(bestEncrypt);
        view.setDecryptStatus(encryptStatus);
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private KeyDisplayStatus getKeyDisplayStatus(SubKeyItem subKeyItem) {
        if (subKeyItem.isValid()) {
            if (subKeyItem.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD) {
                return KeyDisplayStatus.DIVERT;
            }
            return KeyDisplayStatus.OK;
        } else {
            if (subKeyItem.mIsRevoked) {
                return KeyDisplayStatus.REVOKED;
            }
            if (subKeyItem.mIsExpired) {
                return KeyDisplayStatus.EXPIRED;
            }
            return KeyDisplayStatus.STRIPPED;
        }
    }

    interface KeyStatusMvpView {
        void setCertifyStatus(KeyDisplayStatus unavailable);
        void setSignStatus(KeyDisplayStatus signStatus);
        void setDecryptStatus(KeyDisplayStatus encryptStatus);
    }
}
