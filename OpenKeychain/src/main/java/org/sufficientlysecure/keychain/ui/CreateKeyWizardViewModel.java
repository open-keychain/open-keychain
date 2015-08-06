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
package org.sufficientlysecure.keychain.ui;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.model.WizardModel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * View Model for CreateKeyWizardActivity
 */
public class CreateKeyWizardViewModel implements BaseViewModel {
    public static final String STATE_SAVE_WIZARD_STEP = "STATE_SAVE_WIZARD_STEP";
    public static final String STATE_SAVE_WIZARD_MODEL = "STATE_SAVE_WIZARD_MODEL";
    public static final String EXTRA_FIRST_TIME = "EXTRA_FIRST_TIME";
    public static final String EXTRA_NFC_USER_ID = "nfc_user_id";
    public static final String EXTRA_NFC_AID = "nfc_aid";
    public static final String EXTRA_NFC_FINGERPRINTS = "nfc_fingerprints";
    public static final String EXTRA_CREATE_YUBI_KEY = "create_yubi_key";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_YUBI_KEY_PIN = "yubi_key_pin";
    public static final String EXTRA_YUBI_KEY_ADMIN_PIN = "yubi_key_admin_pin";

    private WizardStep mWizardStep = WizardStep.WIZARD_STEP_BEGIN;
    private Context mContext;
    private WizardModel mWizardModel;
    private OnViewModelEventBind mOnViewModelEventBind;

    //NFC stuff
    private byte[] mNfcFingerprints;
    private byte[] mNfcAid;
    private String mNfcUserId;
    private String mNfcFingerprint;

    /**
     * View Model communication
     */
    public interface OnViewModelEventBind {
        void onInstantiatePinUnlockMethod();

        void onInstantiatePatternUnlockMethod();

        void onWelcomeState();

        void onUnlockChoiceState();

        void onNameState();

        void onEmailState();

        void onFinalizeState();

        void onFirstTime();

        void onNotFirstTime();

        void onYubiWaitState();

        void onYubiBlankState();

        void onYubiPinState();

        void onYubiPinRepeatState();

        void onYubiImportState(byte[] nfcFingerprints, String nfcUserId, byte[] nfcAid);

        void onShowNotification(CharSequence message);

        void setActivityTitle(CharSequence title);

        void onStartViewKeyActivity(Intent intent);
    }

    /**
     * Wizard screen steps
     */
    public enum WizardStep {
        WIZARD_STEP_BEGIN,
        WIZARD_STEP_CHOOSE_UNLOCK_METHOD,
        WIZARD_STEP_KEYWORD_INPUT_VERIFICATION,
        WIZARD_STEP_CONTACT_NAME,
        WIZARD_STEP_CONTACT_EMAILS,
        WIZARD_STEP_FINALIZE,
        WIZARD_STEP_YUBI_KEY_WAIT,
        WIZARD_STEP_YUBI_KEY_IMPORT,
        WIZARD_STEP_YUBI_KEY_BLANK,
        WIZARD_STEP_YUBI_KEY_PIN,
        WIZARD_STEP_YUBI_KEY_PIN_REPEAT
    }

    /**
     * Binds the View to the viewModel for callback communication.
     *
     * @param viewModelEventBind
     */
    public CreateKeyWizardViewModel(OnViewModelEventBind viewModelEventBind)
            throws UnsupportedOperationException {
        mOnViewModelEventBind = viewModelEventBind;

        if (mOnViewModelEventBind == null) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Activity context) {
        mContext = context;
        mWizardModel = new WizardModel();
        if (savedInstanceState != null) {
            restoreViewModelState(savedInstanceState);
        } else {
            if (arguments != null) {
                mWizardModel.setCreateYubiKey(arguments.getBoolean(EXTRA_CREATE_YUBI_KEY, false));
                mWizardModel.setFirstTime(arguments.getBoolean(EXTRA_FIRST_TIME, false));
                mWizardModel.setName(arguments.getString(EXTRA_NAME));
                mWizardModel.setEmail(arguments.getString(EXTRA_EMAIL));
                mWizardModel.setYubiKeyAdminPin((Passphrase) arguments.getParcelable(EXTRA_YUBI_KEY_ADMIN_PIN));
                mWizardModel.setYubiKeyPin((Passphrase) arguments.getParcelable(EXTRA_YUBI_KEY_PIN));

                if (arguments.getBoolean(EXTRA_NFC_FINGERPRINTS)) {
                    mNfcFingerprints = arguments.getByteArray(EXTRA_NFC_FINGERPRINTS);
                    mNfcUserId = arguments.getString(EXTRA_NFC_USER_ID);
                    mNfcAid = arguments.getByteArray(EXTRA_NFC_AID);

                    if (containsKeys(mNfcFingerprints)) {
                        mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_IMPORT;
                        mOnViewModelEventBind.onYubiImportState(mNfcFingerprints, mNfcUserId, mNfcAid);
                        mOnViewModelEventBind.setActivityTitle(mContext.getString(R.string.title_import_keys));
                    } else {
                        mOnViewModelEventBind.onShowNotification("YubiKey key creation is currently not supported. Please follow our FAQ.");
                    }

                    return;
                }
            }
        }

        if (mWizardModel.isFirstTime()) {
            mOnViewModelEventBind.onFirstTime();
        } else {
            mOnViewModelEventBind.onNotFirstTime();
        }

        if (mWizardStep == WizardStep.WIZARD_STEP_BEGIN) {
            mOnViewModelEventBind.onWelcomeState();
        }
    }

    @Override
    public void saveViewModelState(Bundle outState) {
        outState.putSerializable(STATE_SAVE_WIZARD_STEP, mWizardStep);
        outState.putParcelable(STATE_SAVE_WIZARD_MODEL, mWizardModel);
    }

    @Override
    public void restoreViewModelState(Bundle savedInstanceState) {
        mWizardStep = (CreateKeyWizardViewModel.WizardStep) savedInstanceState.
                getSerializable(STATE_SAVE_WIZARD_STEP);
        mWizardModel = savedInstanceState.getParcelable(STATE_SAVE_WIZARD_MODEL);
    }

    public boolean containsKeys(byte[] scannedFingerprints) {
        // If all fingerprint bytes are 0, the card contains no keys.
        boolean cardContainsKeys = false;
        for (byte b : scannedFingerprints) {
            if (b != 0) {
                cardContainsKeys = true;
                break;
            }
        }
        return cardContainsKeys;
    }

    /**
     * Updates the state of the wizard when the user presses the back button.
     */
    public void updateWizardStateOnBack() {
        switch (mWizardStep) {
            case WIZARD_STEP_BEGIN: {
            }
            break;
            case WIZARD_STEP_CHOOSE_UNLOCK_METHOD: {
                mWizardStep = WizardStep.WIZARD_STEP_BEGIN;
            }
            break;
            case WIZARD_STEP_KEYWORD_INPUT_VERIFICATION: {
                mWizardStep = WizardStep.WIZARD_STEP_CHOOSE_UNLOCK_METHOD;
            }
            break;
            case WIZARD_STEP_CONTACT_NAME: {
                mWizardStep = WizardStep.WIZARD_STEP_KEYWORD_INPUT_VERIFICATION;

            }
            break;
            case WIZARD_STEP_CONTACT_EMAILS: {
                mWizardStep = WizardStep.WIZARD_STEP_CONTACT_NAME;

            }
            break;
            case WIZARD_STEP_FINALIZE: {
                mWizardStep = WizardStep.WIZARD_STEP_CONTACT_EMAILS;
            }
            break;
            case WIZARD_STEP_YUBI_KEY_WAIT: {
                mWizardStep = WizardStep.WIZARD_STEP_BEGIN;
                mWizardModel.setCreateYubiKey(false);
            }
            break;
            case WIZARD_STEP_YUBI_KEY_IMPORT: {
                mWizardStep = WizardStep.WIZARD_STEP_BEGIN;
                mWizardModel.setCreateYubiKey(false);
            }
            break;
            case WIZARD_STEP_YUBI_KEY_BLANK:
                mWizardStep = WizardStep.WIZARD_STEP_BEGIN;
                mWizardModel.setCreateYubiKey(false);
                break;
            case WIZARD_STEP_YUBI_KEY_PIN:
                break;
            case WIZARD_STEP_YUBI_KEY_PIN_REPEAT:
                mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_PIN;
                mWizardModel.setCreateYubiKey(false);
                break;
            default:
                break;
        }
    }

    /**
     * Updates the state of the wizard when the user presses the next button.
     */
    public void updateWizardStateOnNext() {
        switch (mWizardStep) {
            case WIZARD_STEP_BEGIN: {
                if (mWizardModel.isCreateYubiKey()) {
                    mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_WAIT;
                    mOnViewModelEventBind.onYubiWaitState();
                } else {
                    mWizardStep = WizardStep.WIZARD_STEP_CHOOSE_UNLOCK_METHOD;
                    mOnViewModelEventBind.onUnlockChoiceState();
                }
            }
            break;
            case WIZARD_STEP_CHOOSE_UNLOCK_METHOD: {
                mWizardStep = WizardStep.WIZARD_STEP_KEYWORD_INPUT_VERIFICATION;
                onInstantiateUnlockMethod();
            }
            break;
            case WIZARD_STEP_KEYWORD_INPUT_VERIFICATION: {
                mWizardStep = WizardStep.WIZARD_STEP_CONTACT_NAME;
                mOnViewModelEventBind.onNameState();
            }
            break;
            case WIZARD_STEP_CONTACT_NAME: {
                mWizardStep = WizardStep.WIZARD_STEP_CONTACT_EMAILS;
                mOnViewModelEventBind.onEmailState();
            }
            break;
            case WIZARD_STEP_CONTACT_EMAILS: {
                mWizardStep = WizardStep.WIZARD_STEP_FINALIZE;
                mOnViewModelEventBind.onFinalizeState();
            }
            break;
            case WIZARD_STEP_FINALIZE: {
                //key creation time ?
            }
            break;
            case WIZARD_STEP_YUBI_KEY_WAIT: {
                mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_IMPORT;
            }
            break;
            case WIZARD_STEP_YUBI_KEY_IMPORT: {

            }
            break;
            case WIZARD_STEP_YUBI_KEY_BLANK:
                mWizardStep = WizardStep.WIZARD_STEP_CONTACT_NAME;
                mOnViewModelEventBind.onNameState();
                break;
            case WIZARD_STEP_YUBI_KEY_PIN:
                mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_PIN_REPEAT;
                mOnViewModelEventBind.onYubiPinRepeatState();
                break;
            case WIZARD_STEP_YUBI_KEY_PIN_REPEAT:
                break;
            default:
                break;
        }
    }

    /**
     * Notifies the view to load the unlock fragment.
     */
    private void onInstantiateUnlockMethod() {
        switch (mWizardModel.getSecretKeyType()) {
            case PIN:
                mOnViewModelEventBind.onInstantiatePinUnlockMethod();
                break;
            case PATTERN:
                mOnViewModelEventBind.onInstantiatePatternUnlockMethod();
                break;
        }
    }

    public boolean isFirstTime() {
        return mWizardModel.isFirstTime();
    }

    public void setUnlockMethod(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        mWizardModel.setSecretKeyType(secretKeyType);
    }

    public void setPassphrase(Passphrase passphrase) {
        mWizardModel.setPassphrase(passphrase);
    }

    public void setUserName(CharSequence userName) {
        mWizardModel.setName(userName.toString());
    }

    public void setAdditionalEmails(ArrayList<String> additionalEmails) {
        mWizardModel.setAdditionalEmails(additionalEmails);
    }

    public void setEmail(CharSequence email) {
        mWizardModel.setEmail(email.toString());
    }

    public CharSequence getName() {
        return mWizardModel.getName();
    }

    public CharSequence getEmail() {
        return mWizardModel.getEmail();
    }

    public ArrayList<String> getAdditionalEmails() {
        return mWizardModel.getAdditionalEmails();
    }

    public Passphrase getPassphrase() {
        return mWizardModel.getPassphrase();
    }

    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return mWizardModel.getSecretKeyType();
    }

    public void setCreateYubiKey(boolean createYubiKey) {
        mWizardModel.setCreateYubiKey(createYubiKey);
    }

    public boolean isCreateYubiKey() {
        return mWizardModel.isCreateYubiKey();
    }

    public Passphrase getYubiKeyAdminPin() {
        return mWizardModel.getYubiKeyAdminPin();
    }

    public Passphrase getYubiKeyPin() {
        return mWizardModel.getYubiKeyPin();
    }

    public void onNfcPostExecute() {
        if (containsKeys(mNfcFingerprints)) {
            try {
                long masterKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(mNfcFingerprints);
                CachedPublicKeyRing ring = new ProviderHelper(mContext).getCachedPublicKeyRing(masterKeyId);
                ring.getMasterKeyId();

                Intent intent = new Intent(mContext, ViewKeyActivity.class);
                intent.setData(KeychainContract.KeyRings.buildGenericKeyRingUri(masterKeyId));
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_AID, mNfcAid);
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_USER_ID, mNfcUserId);
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_FINGERPRINTS, mNfcFingerprints);
                mOnViewModelEventBind.onStartViewKeyActivity(intent);

            } catch (PgpKeyNotFoundException e) {
                mOnViewModelEventBind.onShowNotification("YubiKey key creation is currently not supported. Please follow our FAQ.");
            }
        } else {
            //mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_BLANK;
            //mOnViewModelEventBind.onYubiBlankState();
            mOnViewModelEventBind.onShowNotification("YubiKey key creation is currently not supported. Please follow our FAQ.");
        }
    }

    /**
     * Updates the nfc data.
     *
     * @param nfcFingerprints
     * @param nfcAid
     * @param nfcUserId
     */
    public void updateNFCData(byte[] nfcFingerprints, byte[] nfcAid, String nfcUserId) {
        mNfcFingerprints = nfcFingerprints;
        mNfcAid = nfcAid;
        mNfcUserId = nfcUserId;
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(mNfcFingerprints);

        byte[] fp = new byte[20];
        ByteBuffer.wrap(fp).put(mNfcFingerprints, 0, 20);
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);
    }
}
