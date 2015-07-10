package org.sufficientlysecure.keychain.ui.wizard;


import android.content.Context;
import android.os.Bundle;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.base.QueueingCryptoOperationFragment;
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
	private Context mContext;

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
	public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
		mContext = context;
		if (savedInstanceState != null) {
			restoreViewModelState(savedInstanceState);
		} else {
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
		updateNFCData(savedInstanceState.getByteArray(ARG_FINGERPRINT),
				savedInstanceState.getByteArray(ARG_AID),
				savedInstanceState.getString(ARG_USER_ID), true);
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

		if(setData) {
			setData();
		}
	}

	/**
	 * Updates Yubi Key display data,
	 */
	public void setData() {
		String serialNumber = Hex.toHexString(mNfcAid, 10, 4);
		mOnViewModelEventBind.updateSerialNumber(mContext.getString(R.string.yubikey_serno,
				serialNumber));

		if (!mNfcUserId.isEmpty()) {
			mOnViewModelEventBind.updateUserID(mContext.getString(R.string.yubikey_key_holder,
					mNfcUserId));
		} else {
			mOnViewModelEventBind.updateUserID(mContext.getString(R.string.yubikey_key_holder_not_set));
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
