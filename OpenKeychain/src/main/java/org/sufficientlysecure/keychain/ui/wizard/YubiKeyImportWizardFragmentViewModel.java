/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
package org.sufficientlysecure.keychain.ui.wizard;


import android.app.Activity;
import android.os.Bundle;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class YubiKeyImportWizardFragmentViewModel implements BaseViewModel {
    private static final String ARG_FINGERPRINT = "fingerprint";
    public static final String ARG_AID = "aid";
    public static final String ARG_USER_ID = "user_ids";

    private byte[] mNfcFingerprints;
    private byte[] mNfcAid;
    private String mNfcUserId;
    private String mNfcFingerprint;
    private Activity mActivity;

    // for CryptoOperationFragment key import
    private String mKeyserver;
    private ArrayList<ParcelableKeyRing> mKeyList;
    private OnViewModelEventBind mOnViewModelEventBind;

    public interface OnViewModelEventBind {
        void updateUserID(CharSequence userID);

        void updateSerialNumber(CharSequence serialNumber);
    }

    public YubiKeyImportWizardFragmentViewModel(OnViewModelEventBind viewModelEventBind) {
        mOnViewModelEventBind = viewModelEventBind;
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity activity) {
        mActivity = activity;
        if (savedInstanceState == null) {
            updateNFCData(arguments.getByteArray(ARG_FINGERPRINT), arguments.getByteArray(ARG_AID),
                    arguments.getString(ARG_USER_ID), true);
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {
        outState.putByteArray(ARG_FINGERPRINT, mNfcFingerprints);
        outState.putByteArray(ARG_AID, mNfcAid);
        outState.putString(ARG_USER_ID, mNfcUserId);
    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            updateNFCData(savedInstanceState.getByteArray(ARG_FINGERPRINT),
                    savedInstanceState.getByteArray(ARG_AID),
                    savedInstanceState.getString(ARG_USER_ID), true);
        }
    }

    /**
     * Updates the nfc data.
     *
     * @param nfcFingerprints
     * @param nfcAid
     * @param nfcUserId
     */
    public void updateNFCData(byte[] nfcFingerprints, byte[] nfcAid, String nfcUserId, boolean setData) {
        mNfcFingerprints = nfcFingerprints;
        mNfcAid = nfcAid;
        mNfcUserId = nfcUserId;
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(mNfcFingerprints);

        byte[] fp = new byte[20];
        ByteBuffer.wrap(fp).put(mNfcFingerprints, 0, 20);
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);

        if (setData) {
            setData();
        }
    }

    /**
     * Updates Yubi Key display data,
     */
    public void setData() {
        String serialNumber = Hex.toHexString(mNfcAid, 10, 4);
        mOnViewModelEventBind.updateSerialNumber(mActivity.getString(R.string.yubikey_serno,
                serialNumber));

        if (!mNfcUserId.isEmpty()) {
            mOnViewModelEventBind.updateUserID(mActivity.getString(R.string.yubikey_key_holder,
                    mNfcUserId));
        } else {
            mOnViewModelEventBind.updateUserID(mActivity.getString(R.string.yubikey_key_holder_not_set));
        }
    }

    public boolean onBackClicked() {
        if (mActivity.getFragmentManager().getBackStackEntryCount() == 0) {
            mActivity.setResult(Activity.RESULT_CANCELED);
            mActivity.finish();
            return false;
        } else {
            return true;
        }
    }

    public ImportKeyringParcel createOperationInput() {
        return new ImportKeyringParcel(mKeyList, mKeyserver);
    }

    public String getNfcFingerprint() {
        return mNfcFingerprint;
    }

    public String getKeyserver() {
        return mKeyserver;
    }

    public void setKeyserver(String keyserver) {
        mKeyserver = keyserver;
    }

    public void setKeyList(ArrayList<ParcelableKeyRing> keyList) {
        mKeyList = keyList;
    }

    public byte[] getNfcAid() {
        return mNfcAid;
    }

    public String getNfcUserId() {
        return mNfcUserId;
    }
}
