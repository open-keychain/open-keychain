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

package org.sufficientlysecure.keychain.ui.keyview.presenter;


import java.util.Comparator;
import java.util.Date;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyStatusList.KeyDisplayStatus;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusLoader;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusLoader.KeySubkeyStatus;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusLoader.SubKeyItem;


public class KeyHealthPresenter implements LoaderCallbacks<KeySubkeyStatus> {
    private static final Comparator<SubKeyItem> SUBKEY_COMPARATOR = new Comparator<SubKeyItem>() {
        @Override
        public int compare(SubKeyItem one, SubKeyItem two) {
            // if one is valid and the other isn't, the valid one always comes first
            if (one.isValid() ^ two.isValid()) {
                return one.isValid() ? -1 : 1;
            }
            // compare usability, if one is "more usable" than the other, that one comes first
            int usability = one.mSecretKeyType.compareUsability(two.mSecretKeyType);
            if (usability != 0) {
                return usability;
            }
            if ((one.mSecurityProblem == null) ^ (two.mSecurityProblem == null)) {
                return one.mSecurityProblem == null ? -1 : 1;
            }
            // otherwise, the newer one comes first
            return one.newerThan(two) ? -1 : 1;
        }
    };

    private final Context context;
    private final KeyHealthMvpView view;
    private final int loaderId;

    private final long masterKeyId;
    private final boolean isSecret;

    private KeySubkeyStatus subkeyStatus;
    private boolean showingExpandedInfo;


    public KeyHealthPresenter(Context context, KeyHealthMvpView view, int loaderId, long masterKeyId, boolean isSecret) {
        this.context = context;
        this.view = view;
        this.loaderId = loaderId;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;

        view.setOnHealthClickListener(new KeyHealthClickListener() {
            @Override
            public void onKeyHealthClick() {
                KeyHealthPresenter.this.onKeyHealthClick();
            }
        });
    }

    public void startLoader(LoaderManager loaderManager) {
        loaderManager.restartLoader(loaderId, null, this);
    }

    @Override
    public Loader<KeySubkeyStatus> onCreateLoader(int id, Bundle args) {
        return new SubkeyStatusLoader(context, context.getContentResolver(), masterKeyId, SUBKEY_COMPARATOR);
    }

    @Override
    public void onLoadFinished(Loader<KeySubkeyStatus> loader, KeySubkeyStatus subkeyStatus) {
        this.subkeyStatus = subkeyStatus;

        KeyHealthStatus keyHealthStatus = determineKeyHealthStatus(subkeyStatus);

        boolean isInsecure = keyHealthStatus == KeyHealthStatus.INSECURE;
        boolean isExpired = keyHealthStatus == KeyHealthStatus.EXPIRED;
        if (isInsecure) {
            boolean primaryKeySecurityProblem = subkeyStatus.keyCertify.mSecurityProblem != null;
            if (primaryKeySecurityProblem) {
                view.setKeyStatus(keyHealthStatus);
                view.setPrimarySecurityProblem(subkeyStatus.keyCertify.mSecurityProblem);
                view.setShowExpander(false);
            } else {
                view.setKeyStatus(keyHealthStatus);
                view.setShowExpander(false);
                displayExpandedInfo(false);
            }
        } else if (isExpired) {
            view.setKeyStatus(keyHealthStatus);
            view.setPrimaryExpiryDate(subkeyStatus.keyCertify.mExpiry);
            view.setShowExpander(false);
        } else {
            view.setKeyStatus(keyHealthStatus);
            view.setShowExpander(keyHealthStatus != KeyHealthStatus.REVOKED);
        }
    }

    private KeyHealthStatus determineKeyHealthStatus(KeySubkeyStatus subkeyStatus) {
        SubKeyItem keyCertify = subkeyStatus.keyCertify;
        if (keyCertify.mIsRevoked) {
            return KeyHealthStatus.REVOKED;
        }

        if (keyCertify.mIsExpired) {
            return KeyHealthStatus.EXPIRED;
        }

        if (keyCertify.mSecurityProblem != null) {
            return KeyHealthStatus.INSECURE;
        }

        if (!subkeyStatus.keysSign.isEmpty() && subkeyStatus.keysEncrypt.isEmpty()) {
            SubKeyItem keySign = subkeyStatus.keysSign.get(0);
            if (!keySign.isValid()) {
                return KeyHealthStatus.BROKEN;
            }

            if (keySign.mSecurityProblem != null) {
                return KeyHealthStatus.INSECURE;
            }

            return KeyHealthStatus.SIGN_ONLY;
        }

        if (subkeyStatus.keysSign.isEmpty() || subkeyStatus.keysEncrypt.isEmpty()) {
            return KeyHealthStatus.BROKEN;
        }

        SubKeyItem keySign = subkeyStatus.keysSign.get(0);
        SubKeyItem keyEncrypt = subkeyStatus.keysEncrypt.get(0);

        if (keySign.mSecurityProblem != null && keySign.isValid()
                || keyEncrypt.mSecurityProblem != null && keyEncrypt.isValid()) {
            return KeyHealthStatus.INSECURE;
        }

        if (!keySign.isValid() || !keyEncrypt.isValid()) {
            return KeyHealthStatus.BROKEN;
        }

        if (keyCertify.mSecretKeyType == SecretKeyType.GNU_DUMMY
                && keySign.mSecretKeyType == SecretKeyType.GNU_DUMMY
                && keyEncrypt.mSecretKeyType == SecretKeyType.GNU_DUMMY) {
            return KeyHealthStatus.STRIPPED;
        }

        if (keyCertify.mSecretKeyType == SecretKeyType.GNU_DUMMY
                || keySign.mSecretKeyType == SecretKeyType.GNU_DUMMY
                || keyEncrypt.mSecretKeyType == SecretKeyType.GNU_DUMMY) {
            return KeyHealthStatus.PARTIAL_STRIPPED;
        }

        if (keyCertify.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD
                && keySign.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD
                && keyEncrypt.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD) {
            return KeyHealthStatus.DIVERT;
        }

        return KeyHealthStatus.OK;
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void onKeyHealthClick() {
        if (showingExpandedInfo) {
            showingExpandedInfo = false;
            view.hideExpandedInfo();
        } else {
            showingExpandedInfo = true;
            displayExpandedInfo(true);
        }
    }

    private void displayExpandedInfo(boolean displayAll) {
        SubKeyItem keyCertify = subkeyStatus.keyCertify;
        SubKeyItem keySign = subkeyStatus.keysSign.isEmpty() ? null : subkeyStatus.keysSign.get(0);
        SubKeyItem keyEncrypt = subkeyStatus.keysEncrypt.isEmpty() ? null : subkeyStatus.keysEncrypt.get(0);

        KeyDisplayStatus certDisplayStatus = getKeyDisplayStatus(keyCertify);
        KeyDisplayStatus signDisplayStatus = getKeyDisplayStatus(keySign);
        KeyDisplayStatus encryptDisplayStatus = getKeyDisplayStatus(keyEncrypt);

        if (!displayAll) {
            if (certDisplayStatus == KeyDisplayStatus.OK) {
                certDisplayStatus = null;
            }
            if (certDisplayStatus == KeyDisplayStatus.INSECURE) {
                signDisplayStatus = null;
                encryptDisplayStatus = null;
            }
            if (signDisplayStatus == KeyDisplayStatus.OK) {
                signDisplayStatus = null;
            }
            if (encryptDisplayStatus == KeyDisplayStatus.OK) {
                encryptDisplayStatus = null;
            }
        }

        view.showExpandedState(certDisplayStatus, signDisplayStatus, encryptDisplayStatus);
    }

    private KeyDisplayStatus getKeyDisplayStatus(SubKeyItem subKeyItem) {
        if (subKeyItem == null) {
            return KeyDisplayStatus.UNAVAILABLE;
        }

        if (subKeyItem.mIsRevoked) {
            return KeyDisplayStatus.REVOKED;
        }
        if (subKeyItem.mIsExpired) {
            return KeyDisplayStatus.EXPIRED;
        }
        if (subKeyItem.mSecurityProblem != null) {
            return KeyDisplayStatus.INSECURE;
        }
        if (subKeyItem.mSecretKeyType == SecretKeyType.GNU_DUMMY) {
            return KeyDisplayStatus.STRIPPED;
        }
        if (subKeyItem.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD) {
            return KeyDisplayStatus.DIVERT;
        }

        return KeyDisplayStatus.OK;
    }

    public enum KeyHealthStatus {
        OK, DIVERT, REVOKED, EXPIRED, INSECURE, SIGN_ONLY, STRIPPED, PARTIAL_STRIPPED, BROKEN
    }

    public interface KeyHealthMvpView {
        void setKeyStatus(KeyHealthStatus keyHealthStatus);
        void setPrimarySecurityProblem(KeySecurityProblem securityProblem);
        void setPrimaryExpiryDate(Date expiry);

        void setShowExpander(boolean showExpander);
        void showExpandedState(KeyDisplayStatus certifyStatus, KeyDisplayStatus signStatus,
                KeyDisplayStatus encryptStatus);
        void hideExpandedInfo();

        void setOnHealthClickListener(KeyHealthClickListener keyHealthClickListener);

    }

    public interface KeyStatusMvpView {
        void setCertifyStatus(KeyDisplayStatus unavailable);
        void setSignStatus(KeyDisplayStatus signStatus);
        void setDecryptStatus(KeyDisplayStatus encryptStatus);
    }

    public interface KeyHealthClickListener {
        void onKeyHealthClick();
    }
}
