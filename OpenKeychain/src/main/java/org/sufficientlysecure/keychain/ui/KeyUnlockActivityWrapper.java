package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;

import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.nfc.BaseNfcTagTechnology;
import org.sufficientlysecure.keychain.nfc.MifareUltralight;
import org.sufficientlysecure.keychain.nfc.NfcDispatcher;
import org.sufficientlysecure.keychain.ui.dialog.PinUnlockDialogViewModel;
import org.sufficientlysecure.keychain.ui.dialog.UnlockDialog;
import org.sufficientlysecure.keychain.ui.util.KeyboardUtils;

/**
 * Activity wrapper for the key unlock dialogs
 */
public class KeyUnlockActivityWrapper extends FragmentActivity
        implements KeyUnlockActivityWrapperViewModel.OnViewModelEventBind,
        NfcDispatcher.NfcDispatcherCallback {
    private KeyUnlockActivityWrapperViewModel mKeyUnlockActivityWrapperViewModel;
    public static final String RESULT_CRYPTO_INPUT = "result_data";
    public static final String EXTRA_REQUIRED_INPUT = "required_input";
    public static final String EXTRA_SUBKEY_ID = "secret_key_id";
    public static final String EXTRA_KEY_TYPE = "secret_key_type";

    // special extra for OpenPgpService
    public static final String EXTRA_SERVICE_INTENT = "data";

    private UnlockDialog mUnlockDialog;
    private NfcDispatcher mNfcDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        NfcDispatcher.RegisteredTechHandler registeredTechHandler = new NfcDispatcher.RegisteredTechHandler();
        registeredTechHandler.put(MifareUltralight.class);

        mNfcDispatcher = new NfcDispatcher(this, this, registeredTechHandler);
        mNfcDispatcher.initialize(savedInstanceState);

        mKeyUnlockActivityWrapperViewModel = new KeyUnlockActivityWrapperViewModel(this);
        mKeyUnlockActivityWrapperViewModel.prepareViewModel(savedInstanceState,
                getIntent().getExtras(), this);

        // do not allow screenshots of passphrase input
        // to prevent "too easy" passphrase theft by root apps
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }

        KeyboardUtils.hideKeyboard(this);
        mKeyUnlockActivityWrapperViewModel.startUnlockOperation();
    }

    /**
     * Shows an unlock dialog.
     *
     * @param unlockDialog
     */
    public void showUnlockDialog(UnlockDialog unlockDialog) {
        if (unlockDialog != null) {
            mUnlockDialog = unlockDialog;
            Intent serviceIntent = getIntent().getParcelableExtra(EXTRA_SERVICE_INTENT);

            Bundle bundle = new Bundle();
            bundle.putSerializable(UnlockDialog.EXTRA_PARAM_OPERATION_TYPE,
                    PinUnlockDialogViewModel.DialogUnlockOperation.DIALOG_UNLOCK_TYPE_UNLOCK_KEY);

            bundle.putSerializable(EXTRA_KEY_TYPE, mKeyUnlockActivityWrapperViewModel.getSecretKeyType());
            bundle.putLong(EXTRA_SUBKEY_ID, mKeyUnlockActivityWrapperViewModel.getKeyId());
            bundle.putParcelable(EXTRA_SERVICE_INTENT, serviceIntent);
            unlockDialog.setArguments(bundle);

            show(unlockDialog, this, bundle);
        }
    }

    /**
     * Shows passphrase dialog to cache a new passphrase the user enters for using it later for
     * encryption. Based on mSecretKeyId it asks for a passphrase to open a private key or it asks
     * for a symmetric passphrase
     */
    public static void show(final UnlockDialog unlockDialog, final FragmentActivity fragmentActivity,
                            final Bundle bundle) {
        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                // do NOT check if the key even needs a passphrase. that's not our job here.
                if (bundle != null) {
                    unlockDialog.setArguments(bundle);
                }
                unlockDialog.show(fragmentActivity.getSupportFragmentManager(), "passphraseDialog");
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        mNfcDispatcher.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcDispatcher.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNfcDispatcher.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mNfcDispatcher.onDestroy();
    }

    //NFC STUFF
    @Override
    public void doNfcInBackground() throws NfcDispatcher.CardException {
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcDispatcher.NfcDispatcherCallback) {
            ((NfcDispatcher.NfcDispatcherCallback) mUnlockDialog).doNfcInBackground();
        }
    }

    @Override
    public void onNfcPreExecute() throws NfcDispatcher.CardException {
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcDispatcher.NfcDispatcherCallback) {
            ((NfcDispatcher.NfcDispatcherCallback) mUnlockDialog).onNfcPreExecute();
        }
    }

    @Override
    public void onNfcPostExecute() throws NfcDispatcher.CardException {
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcDispatcher.NfcDispatcherCallback) {
            ((NfcDispatcher.NfcDispatcherCallback) mUnlockDialog).onNfcPostExecute();
        }
    }

    @Override
    public void onNfcError(NfcDispatcher.CardException exception) {
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcDispatcher.NfcDispatcherCallback) {
            ((NfcDispatcher.NfcDispatcherCallback) mUnlockDialog).onNfcError(exception);
        }
    }

    public void handleTagDiscoveredIntent(Intent intent) throws NfcDispatcher.CardException {
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcDispatcher.NfcDispatcherCallback) {
            ((NfcDispatcher.NfcDispatcherCallback) mUnlockDialog).handleTagDiscoveredIntent(intent);
        }
    }

    @Override
    public void onNfcTechnologyInitialized(BaseNfcTagTechnology baseNfcTagTechnology) {
        if (mUnlockDialog != null && mUnlockDialog instanceof NfcDispatcher.NfcDispatcherCallback) {
            ((NfcDispatcher.NfcDispatcherCallback) mUnlockDialog).onNfcTechnologyInitialized(baseNfcTagTechnology);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof UnlockDialog) {
            mUnlockDialog = (UnlockDialog) fragment;
        }
    }
}
