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

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;
import org.sufficientlysecure.keychain.ui.base.WizardFragmentListener;
import org.sufficientlysecure.keychain.ui.dialog.AddEmailDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.wizard.EmailWizardFragment;
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
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Activity for creating keys with different security options.
 */
public class CreateKeyWizardActivity extends BaseNfcActivity implements WizardFragmentListener,
        AddEmailDialogFragment.onAddEmailDialogListener,
        CreateKeyWizardViewModel.OnViewModelEventBind {

    public static final String FRAGMENT_TAG = "CurrentWizardFragment";
    public static final String EXTRA_FIRST_TIME = "EXTRA_FIRST_TIME";
    public static final String EXTRA_NFC_FINGERPRINTS = "nfc_fingerprints";
    public static final String EXTRA_NFC_USER_ID = "nfc_user_id";
    public static final String EXTRA_NFC_AID = "nfc_aid";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_EMAIL = "email";
    public static final int WIZARD_BUTTON_ANIMATION_DURATION = 400;

    private CreateKeyWizardViewModel mCreateKeyWizardViewModel;
    private Button mNextButton;
    private Button mBackButton;
    private WizardFragment mCurrentVisibleFragment;

    public interface NfcListenerFragment {
        void doNfcInBackground() throws IOException;

        void onNfcPostExecute() throws IOException;
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

        mCreateKeyWizardViewModel = new CreateKeyWizardViewModel(this);

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
            mCurrentVisibleFragment = (WizardFragment) getSupportFragmentManager().
                    findFragmentByTag(FRAGMENT_TAG);
        } else {
            if (getIntent().hasExtra(EXTRA_NFC_FINGERPRINTS)) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.title_import_keys);
                }
            }
        }

        mCreateKeyWizardViewModel.prepareViewModel(savedInstanceState, getIntent().getExtras(),
                this);
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
            mCreateKeyWizardViewModel.updateWizardStateOnBack();
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
            mCreateKeyWizardViewModel.updateWizardStateOnNext();
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
        mCreateKeyWizardViewModel.updateWizardStateOnNext();
    }

    /**
     * Updates the model with the current selected unlock type.
     *
     * @param secretKeyType
     */
    @Override
    public void setUnlockMethod(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        mCreateKeyWizardViewModel.setUnlockMethod(secretKeyType);
    }

    @Override
    public void setPassphrase(Passphrase passphrase) {
        mCreateKeyWizardViewModel.setPassphrase(passphrase);
    }

    @Override
    public void setUserName(CharSequence userName) {
        mCreateKeyWizardViewModel.setUserName(userName.toString());
    }

    @Override
    public void setAdditionalEmails(ArrayList<String> additionalEmails) {
        mCreateKeyWizardViewModel.setAdditionalEmails(additionalEmails);
    }

    @Override
    public void setEmail(CharSequence email) {
        mCreateKeyWizardViewModel.setEmail(email.toString());
    }

    @Override
    public CharSequence getName() {
        return mCreateKeyWizardViewModel.getName();
    }

    @Override
    public CharSequence getEmail() {
        return mCreateKeyWizardViewModel.getEmail();
    }

    @Override
    public ArrayList<String> getAdditionalEmails() {
        return mCreateKeyWizardViewModel.getAdditionalEmails();
    }

    @Override
    public Passphrase getPassphrase() {
        return mCreateKeyWizardViewModel.getPassphrase();
    }

    @Override
    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return mCreateKeyWizardViewModel.getSecretKeyType();
    }

    @Override
    public void onWizardFragmentVisible(WizardFragment fragment) {
        mCurrentVisibleFragment = fragment;
    }

    @Override
    public void cancelRequest() {
        if (mCreateKeyWizardViewModel.isFirstTime()) {
            Preferences prefs = Preferences.getPreferences(this);
            prefs.setFirstTime(false);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            finish();
        }
    }

    @Override
    public boolean isFirstTime() {
        return mCreateKeyWizardViewModel.isFirstTime();
    }

    @Override
    public void setUseYubiKey() {
        mCreateKeyWizardViewModel.setCreateYubiKey(true);
    }

    @Override
    public boolean createYubiKey() {
        return mCreateKeyWizardViewModel.isCreateYubiKey();
    }

    @Override
    public Passphrase getYubiKeyAdminPin() {
        return mCreateKeyWizardViewModel.getYubiKeyAdminPin();
    }

    @Override
    public Passphrase getYubiKeyPin() {
        return mCreateKeyWizardViewModel.getYubiKeyPin();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCreateKeyWizardViewModel.saveViewModelState(outState);
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
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.setCustomAnimations(R.anim.frag_slide_in_from_right,
                R.anim.frag_slide_out_to_left,
                R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
        transaction.replace(R.id.unlockWizardFragmentContainer, fragment,
                FRAGMENT_TAG);
        transaction.commit();
    }

    @Override
    public void onInstantiatePinUnlockMethod() {
        mCurrentVisibleFragment = new PinUnlockWizardFragment();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    @Override
    public void onInstantiatePatternUnlockMethod() {
        mCurrentVisibleFragment = new PatternUnlockWizardFragment();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    @Override
    public void onWelcomeState() {
        mCurrentVisibleFragment = WelcomeWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, false);
    }

    @Override
    public void onUnlockChoiceState() {
        mCurrentVisibleFragment = UnlockChoiceWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    @Override
    public void onNameState() {
        mCurrentVisibleFragment = NameWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    @Override
    public void onEmailState() {
        mCurrentVisibleFragment = EmailWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    @Override
    public void onFinalizeState() {
        //finalize the creation of the key
        mCurrentVisibleFragment = WizardConfirmationFragment.
                newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
        mNextButton.setText(R.string.btn_create_key);
        mNextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_key_plus_grey600_24dp, 0);
    }

    @Override
    public void onFirstTime() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
        mToolbar.setNavigationIcon(null);
        mToolbar.setNavigationOnClickListener(null);
    }

    @Override
    public void onNotFirstTime() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_manage_my_keys);
        }
    }

    @Override
    public void onYubiWaitState() {
        mCurrentVisibleFragment = YubiKeyWaitWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    @Override
    public void onYubiBlankState() {
        mCurrentVisibleFragment = YubiKeyBlankWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
        mNextButton.setText(getString(R.string.first_time_blank_yubikey_yes));
    }

    @Override
    public void onYubiPinState() {
        mCurrentVisibleFragment = YubiKeyPinWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    @Override
    public void onYubiPinRepeatState() {
        mCurrentVisibleFragment = YubiKeyPinRepeatWizardFragment.newInstance();
        beginWizardTransaction(mCurrentVisibleFragment, true);
    }

    @Override
    public void onYubiImportState(byte[] nfcFingerprints, String nfcUserId, byte[] nfcAid) {
        mCurrentVisibleFragment = YubiKeyImportWizardFragment.newInstance(nfcFingerprints, nfcAid,
                nfcUserId);
        beginWizardTransaction(mCurrentVisibleFragment, true);
        mNextButton.setText(R.string.btn_import);
        mNextButton.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                R.drawable.ic_key_plus_grey600_24dp, 0);
    }

    @Override
    public void onShowNotification(CharSequence message) {
        Notify.create(this, message.toString(), Notify.Style.ERROR).show();
    }

    @Override
    public void setActivityTitle(CharSequence title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
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
        if (mCurrentVisibleFragment instanceof NfcListenerFragment) {
            ((NfcListenerFragment) mCurrentVisibleFragment).onNfcPostExecute();
            return;
        }
        mCreateKeyWizardViewModel.updateNFCData(mNfcFingerprints, mNfcAid, mNfcUserId);
        mCreateKeyWizardViewModel.onNfcPostExecute();
    }

    @Override
    public void onBackPressed() {
        onBackClicked(null);
    }
}
