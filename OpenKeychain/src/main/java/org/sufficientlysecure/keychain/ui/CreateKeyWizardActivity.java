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
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.ui.dialog.AddEmailDialogFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.wizard.EmailWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.NFCUnlockWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.NameWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.PatternUnlockWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.PinUnlockWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.UnlockChoiceWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.WelcomeWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.WizardConfirmationFragment;
import org.sufficientlysecure.keychain.ui.wizard.YubiKeyBlankWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.YubiKeyImportWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.YubiKeyPinRepeatWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.YubiKeyPinWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.YubiKeyWaitWizardFragment;
import org.sufficientlysecure.keychain.ui.wizard.model.WizardModel;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Activity for creating keys with different security options.
 */
public class CreateKeyWizardActivity extends BaseNfcActivity implements WizardFragmentListener,
        AddEmailDialogFragment.OnAddEmailDialogListener {

    public static final String FRAGMENT_TAG = "CurrentWizardFragment";
    public static final String STATE_SAVE_WIZARD_STEP = "STATE_SAVE_WIZARD_STEP";
    public static final String STATE_SAVE_WIZARD_MODEL = "STATE_SAVE_WIZARD_MODEL";
    public static final String EXTRA_FIRST_TIME = "EXTRA_FIRST_TIME";
    public static final String EXTRA_NFC_FINGERPRINTS = "nfc_fingerprints";
    public static final String EXTRA_NFC_USER_ID = "nfc_user_id";
    public static final String EXTRA_NFC_AID = "nfc_aid";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_EMAIL = "email";
    public static final String EXTRA_CREATE_YUBI_KEY = "create_yubi_key";
    public static final String EXTRA_YUBI_KEY_PIN = "yubi_key_pin";
    public static final String EXTRA_YUBI_KEY_ADMIN_PIN = "yubi_key_admin_pin";
    public static final int WIZARD_BUTTON_ANIMATION_DURATION = 400;

    private Button mNextButton;
    private Button mBackButton;
    private WizardFragment mCurrentVisibleFragment;
    private WizardStep mWizardStep = WizardStep.WIZARD_STEP_BEGIN;
    private WizardModel mWizardModel;

    public interface NfcListenerFragment {
        void onNfcError(Exception exception);

        void onNfcPreExecute() throws IOException;

        void doNfcInBackground() throws IOException;

        void onNfcPostExecute() throws IOException;

        void onNfcTagDiscovery(Intent intent);
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
     * Communication between the Activity and its fragments
     */
    public interface CreateKeyWizardListener {
        /**
         * Method that is triggered when the user clicks on the next button
         *
         * @return
         */
        boolean onNextClicked();

        /**
         * Method that is triggered when the user clicks on the back button
         */
        boolean onBackClicked();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWizardModel = new WizardModel();

        // React on NDEF_DISCOVERED from Manifest
        // NOTE: ACTION_NDEF_DISCOVERED and not ACTION_TAG_DISCOVERED like in BaseNfcActivity
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            handleIntentInBackground(getIntent());
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.title_manage_my_keys);
            }
            // done
            return;
        }

        if (savedInstanceState != null) {
            mWizardStep = (WizardStep) savedInstanceState.getSerializable(STATE_SAVE_WIZARD_STEP);
            mWizardModel = savedInstanceState.getParcelable(STATE_SAVE_WIZARD_MODEL);
            mCurrentVisibleFragment = (WizardFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        } else {
            Intent intent = getIntent();

            mWizardModel.setCreateYubiKey(intent.getBooleanExtra(EXTRA_CREATE_YUBI_KEY, false));
            mWizardModel.setFirstTime(intent.getBooleanExtra(EXTRA_FIRST_TIME, false));
            mWizardModel.setName(intent.getStringExtra(EXTRA_NAME));
            mWizardModel.setEmail(intent.getStringExtra(EXTRA_EMAIL));
            mWizardModel.setYubiKeyAdminPin((Passphrase) intent.getParcelableExtra(EXTRA_YUBI_KEY_ADMIN_PIN));
            mWizardModel.setYubiKeyPin((Passphrase) intent.getParcelableExtra(EXTRA_YUBI_KEY_PIN));

            if (getIntent().hasExtra(EXTRA_NFC_FINGERPRINTS)) {
                mNfcFingerprints = getIntent().getExtras().getByteArray(EXTRA_NFC_FINGERPRINTS);
                mNfcUserId = getIntent().getExtras().getString(EXTRA_NFC_USER_ID);
                mNfcAid = getIntent().getExtras().getByteArray(EXTRA_NFC_AID);

                if (containsKeys(mNfcFingerprints)) {
                    mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_IMPORT;
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(R.string.title_import_keys);
                    }
                    onYubiImportState(mNfcFingerprints, mNfcUserId, mNfcAid);
                } else {
                    onShowNotification("YubiKey key creation is currently not supported. Please follow our FAQ.");
                }
                return;
            }
        }

        if (mWizardModel.isFirstTime()) {
            onFirstTime();
        } else {
            onNotFirstTime();
        }

        if (mWizardStep == WizardStep.WIZARD_STEP_BEGIN) {
            onWelcomeState();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.create_key_wizard_activity);

        mNextButton = (Button) findViewById(R.id.nextButton);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNextClicked(v);
            }
        });

        mBackButton = (Button) findViewById(R.id.backButton);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackClicked(v);
            }
        });
    }

    /**
     * Updates the interface and the viewModel state when the user clicks on the back button.
     *
     * @param view
     */
    public void onBackClicked(View view) {
        if (mCurrentVisibleFragment != null && mCurrentVisibleFragment.onBackClicked()) {
            getSupportFragmentManager().popBackStack();
            updateWizardStateOnBack();
            restoreNavigationButtonText();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Restores the button text if it was altered previously
     * Note: might be a good idea to let the fragments manipulate the texts instead.
     */
    private void restoreNavigationButtonText() {
        mNextButton.setText(R.string.btn_next);
        mNextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_chevron_right_grey_24dp, 0);
    }

    /**
     * Updates the interface and the viewModel state when the user clicks on the next button.
     *
     * @param view
     */
    public void onNextClicked(View view) {
        if (mCurrentVisibleFragment != null && mCurrentVisibleFragment.onNextClicked()) {
            updateWizardStateOnNext();
        }
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
                    onYubiWaitState();
                } else {
                    mWizardStep = WizardStep.WIZARD_STEP_CHOOSE_UNLOCK_METHOD;
                    onUnlockChoiceState();
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
                onNameState();
            }
            break;
            case WIZARD_STEP_CONTACT_NAME: {
                mWizardStep = WizardStep.WIZARD_STEP_CONTACT_EMAILS;
                onEmailState();
            }
            break;
            case WIZARD_STEP_CONTACT_EMAILS: {
                mWizardStep = WizardStep.WIZARD_STEP_FINALIZE;
                onFinalizeState();
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
                onNameState();
                break;
            case WIZARD_STEP_YUBI_KEY_PIN:
                mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_PIN_REPEAT;
                onYubiPinRepeatState();
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
                onInstantiatePinUnlockMethod();
                break;
            case PATTERN:
                onInstantiatePatternUnlockMethod();
                break;
        }
    }

    @Override
    public void onHideNavigationButtons(boolean hideBack, boolean hideNext) {
        if (hideBack) {
            mBackButton.setVisibility(View.INVISIBLE);
            mBackButton.animate().setDuration(WIZARD_BUTTON_ANIMATION_DURATION);
            mBackButton.animate().alpha(0);
        } else {
            mBackButton.setVisibility(View.VISIBLE);
            mBackButton.animate().setDuration(WIZARD_BUTTON_ANIMATION_DURATION);
            mBackButton.animate().alpha(1);
        }

        if (hideNext) {
            mNextButton.setVisibility(View.INVISIBLE);
            mNextButton.animate().setDuration(WIZARD_BUTTON_ANIMATION_DURATION);
            mNextButton.animate().alpha(0);
        } else {
            mNextButton.setVisibility(View.VISIBLE);
            mNextButton.animate().setDuration(WIZARD_BUTTON_ANIMATION_DURATION);
            mNextButton.animate().alpha(1);
        }
    }

    @Override
    public void onAdvanceToNextWizardStep() {
        updateWizardStateOnNext();
    }

    /**
     * Updates the model with the current selected unlock type.
     *
     * @param secretKeyType
     */
    @Override
    public void setUnlockMethod(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        mWizardModel.setSecretKeyType(secretKeyType);
    }

    @Override
    public void setPassphrase(Passphrase passphrase) {
        mWizardModel.setPassphrase(passphrase);
    }

    @Override
    public void setUserName(CharSequence userName) {
        mWizardModel.setName(userName.toString());
    }

    @Override
    public void setAdditionalEmails(ArrayList<String> additionalEmails) {
        mWizardModel.setAdditionalEmails(additionalEmails);
    }

    @Override
    public void setEmail(CharSequence email) {
        mWizardModel.setEmail(email.toString());
    }

    @Override
    public CharSequence getName() {
        return mWizardModel.getName();
    }

    @Override
    public CharSequence getEmail() {
        return mWizardModel.getEmail();
    }

    @Override
    public ArrayList<String> getAdditionalEmails() {
        return mWizardModel.getAdditionalEmails();
    }

    @Override
    public Passphrase getPassphrase() {
        return mWizardModel.getPassphrase();
    }

    @Override
    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return mWizardModel.getSecretKeyType();
    }

    @Override
    public void onWizardFragmentVisible(WizardFragment fragment) {
        mCurrentVisibleFragment = fragment;
    }

    @Override
    public void cancelRequest() {
        if (isFirstTime()) {
            Preferences prefs = Preferences.getPreferences(this);
            prefs.setFirstTime(false);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            // just finish activity and return data
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public boolean isFirstTime() {
        return mWizardModel.isFirstTime();
    }

    @Override
    public void setUseYubiKey() {
        mWizardModel.setCreateYubiKey(true);
    }

    @Override
    public boolean createYubiKey() {
        return mWizardModel.isCreateYubiKey();
    }

    @Override
    public Passphrase getYubiKeyAdminPin() {
        return mWizardModel.getYubiKeyAdminPin();
    }

    @Override
    public Passphrase getYubiKeyPin() {
        return mWizardModel.getYubiKeyPin();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_SAVE_WIZARD_STEP, mWizardStep);
        outState.putParcelable(STATE_SAVE_WIZARD_MODEL, mWizardModel);
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

    @Override
    public void onAddAdditionalEmail(String email) {
        mCurrentVisibleFragment.onRequestAddEmail(email);
    }

    /**
     * Starts a new Wizard transaction.
     *
     * @param fragment
     * @param addToBackStack
     * @return
     */
    void beginWizardTransaction(WizardFragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.setCustomAnimations(R.anim.frag_slide_in_from_right,
                R.anim.frag_slide_out_to_left,
                R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.replace(R.id.unlockWizardFragmentContainer, fragment, FRAGMENT_TAG);
        transaction.commit();
    }

    public void onInstantiatePinUnlockMethod() {
        mCurrentVisibleFragment = new PinUnlockWizardFragment();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onInstantiatePatternUnlockMethod() {
        mCurrentVisibleFragment = new PatternUnlockWizardFragment();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onWelcomeState() {
        mCurrentVisibleFragment = WelcomeWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, false);
    }

    public void onUnlockChoiceState() {
        mCurrentVisibleFragment = UnlockChoiceWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onNameState() {
        mCurrentVisibleFragment = NameWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onEmailState() {
        mCurrentVisibleFragment = EmailWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onFinalizeState() {
        //finalize the creation of the key
        mCurrentVisibleFragment = WizardConfirmationFragment.
                newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
        mNextButton.setText(R.string.btn_create_key);
        mNextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_key_plus_grey600_24dp, 0);
    }

    public void onFirstTime() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
        mToolbar.setNavigationIcon(null);
        mToolbar.setNavigationOnClickListener(null);
    }

    public void onNotFirstTime() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_manage_my_keys);
        }
    }

    public void onYubiWaitState() {
        mCurrentVisibleFragment = YubiKeyWaitWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onYubiBlankState() {
        mCurrentVisibleFragment = YubiKeyBlankWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
        mNextButton.setText(getString(R.string.first_time_blank_yubikey_yes));
    }

    public void onYubiPinState() {
        mCurrentVisibleFragment = YubiKeyPinWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onYubiPinRepeatState() {
        mCurrentVisibleFragment = YubiKeyPinRepeatWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onYubiImportState(byte[] nfcFingerprints, String nfcUserId, byte[] nfcAid) {
        mCurrentVisibleFragment = YubiKeyImportWizardFragment.newInstance(nfcFingerprints, nfcAid,
                nfcUserId);
        beginWizardTransaction(mCurrentVisibleFragment, true);
        mNextButton.setText(R.string.btn_import);
        mNextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_key_plus_grey600_24dp, 0);
    }

    public void onInstantiateNFCUnlockMethod() {
        mCurrentVisibleFragment = new NFCUnlockWizardFragment();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    public void onShowNotification(CharSequence message) {
        Notify.create(this, message.toString(), Notify.Style.ERROR).show();
    }

    public void onStartViewKeyActivity(Intent intent) {
        startActivity(intent);
        finish();
    }

    @Override
    protected void doNfcInBackground() throws IOException {
        super.doNfcInBackground();
        if (mCurrentVisibleFragment instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mCurrentVisibleFragment).doNfcInBackground();
        }
    }

    @Override
    protected void onNfcPostExecute() throws IOException {
        updateNFCData();
        if (mCurrentVisibleFragment instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mCurrentVisibleFragment).onNfcPostExecute();
            return;
        }

        if (containsKeys(mNfcFingerprints)) {
            try {
                long masterKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(mNfcFingerprints);
                CachedPublicKeyRing ring = new ProviderHelper(this).getCachedPublicKeyRing(masterKeyId);
                ring.getMasterKeyId();

                Intent intent = new Intent(this, ViewKeyActivity.class);
                intent.setData(KeychainContract.KeyRings.buildGenericKeyRingUri(masterKeyId));
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_AID, mNfcAid);
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_USER_ID, mNfcUserId);
                intent.putExtra(ViewKeyActivity.EXTRA_NFC_FINGERPRINTS, mNfcFingerprints);
                onStartViewKeyActivity(intent);

            } catch (PgpKeyNotFoundException e) {
                onShowNotification("YubiKey key creation is currently not supported. Please follow our FAQ.");
            }
        } else {
            //mWizardStep = WizardStep.WIZARD_STEP_YUBI_KEY_BLANK;
            //mOnViewModelEventBind.onYubiBlankState();
            onShowNotification("YubiKey key creation is currently not supported. Please follow our FAQ.");
        }
    }

    /**
     * Updates the nfc data.
     */
    public void updateNFCData() {
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(mNfcFingerprints);

        byte[] fp = new byte[20];
        ByteBuffer.wrap(fp).put(mNfcFingerprints, 0, 20);
        mNfcFingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);
    }

    @Override
    protected void handleTagDiscoveredIntent(Intent intent) throws IOException {
        super.handleTagDiscoveredIntent(intent);
        if (mCurrentVisibleFragment instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mCurrentVisibleFragment).onNfcTagDiscovery(intent);
        }
    }

    @Override
    protected void handleNfcError(Exception e) {
        super.handleNfcError(e);
        if (mCurrentVisibleFragment instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mCurrentVisibleFragment).onNfcError(e);
        }
    }

    @Override
    public void onBackPressed() {
        onBackClicked(null);
    }
}
