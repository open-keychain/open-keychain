/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.LoaderFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.widget.ToolableViewAnimator;
import org.sufficientlysecure.keychain.util.Passphrase;
import timber.log.Timber;


public class ViewKeyAdvLockFragment extends LoaderFragment implements OnClickListener {
    public static final String ARG_DATA_URI = "uri";

    public static final int REQUEST_CODE_PASSPHRASE = 0;


    private ToolableViewAnimator viewAnimator;
    private KeyRepository keyRepository;
    private TextView lockStatusText;
    private View lockCurrentNone;
    private View lockCurrentPassword;
    private ActionMode actionMode;
    private long masterKeyId;
    private byte[] fingerprint;
    private CryptoInputParcel cachedCryptoInput;
    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult>
            operationHelper;
    private EditText lockPasswordField;
    private EditText lockPasswordRepeat;

    private LockType currentLockType;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_adv_lock_fragment, getContainer());

        keyRepository = KeyRepository.create(getContext());

        viewAnimator = view.findViewById(R.id.adv_lock_animator);
        lockStatusText = view.findViewById(R.id.lock_status_text);

        lockCurrentNone = view.findViewById(R.id.lock_method_current_none);
        lockCurrentPassword = view.findViewById(R.id.lock_method_current_password);

        lockPasswordField = view.findViewById(R.id.lock_password);
        lockPasswordRepeat = view.findViewById(R.id.lock_password_repeat);

        view.findViewById(R.id.lock_method_choice_none).setOnClickListener(this);
        view.findViewById(R.id.lock_method_choice_password).setOnClickListener(this);
        view.findViewById(R.id.lock_button_password_back).setOnClickListener(this);
        view.findViewById(R.id.lock_button_password_save).setOnClickListener(this);

        setHasOptionsMenu(true);

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        if (dataUri == null) {
            Timber.e("Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }

        loadData(dataUri);
    }

    private void loadData(Uri dataUri) {
        try {
            CachedPublicKeyRing cachedPublicKeyRing = keyRepository.getCachedPublicKeyRing(dataUri);
            masterKeyId = cachedPublicKeyRing.extractOrGetMasterKeyId();
            fingerprint = cachedPublicKeyRing.getFingerprint();
        } catch (PgpKeyNotFoundException e) {
            throw new IllegalStateException("Key is gone?");
        }

        refreshLockMethodDisplay();

        setContentShown(true);
    }

    private enum LockType {
        NONE, PASSWORD, SECURITY_TOKEN, MIXED, UNAVAILABLE;

        static LockType fromSecretKeyType(SecretKeyType secretKeyType) {
            switch (secretKeyType) {
                case PASSPHRASE:
                    return PASSWORD;
                case PASSPHRASE_EMPTY:
                    return NONE;
                case DIVERT_TO_CARD:
                    return SECURITY_TOKEN;
                case UNAVAILABLE:
                case GNU_DUMMY:
                    return UNAVAILABLE;
            }
            throw new IllegalArgumentException("all cases must be handled!");
        }

        /* This will combine two lock types, returning the  */
        LockType plus(LockType other) {
            if (this == UNAVAILABLE || other == UNAVAILABLE) {
                return other == UNAVAILABLE ? this : other;
            }

            if (this == MIXED || other == MIXED) {
                return MIXED;
            }

            if (this != other) {
                return MIXED;
            }

            return this;
        }
    }

    private LockType findCurrentLockType() {
        try {
            LockType lockType = LockType.UNAVAILABLE;

            CanonicalizedPublicKeyRing canonicalizedPublicKeyRing =
                    keyRepository.getCanonicalizedPublicKeyRing(masterKeyId);
            CachedPublicKeyRing cachedPublicKeyRing = keyRepository.getCachedPublicKeyRing(masterKeyId);
            for (CanonicalizedPublicKey subKey : canonicalizedPublicKeyRing.publicKeyIterator()) {
                SecretKeyType secretKeyType = cachedPublicKeyRing.getSecretKeyType(subKey.getKeyId());
                LockType subKeyLockType = LockType.fromSecretKeyType(secretKeyType);

                lockType = lockType.plus(subKeyLockType);
            }

            return lockType;
        } catch (NotFoundException e) {
            throw new IllegalStateException("Key is gone?");
        }
    }

    private void refreshLockMethodDisplay() {
        lockCurrentNone.setVisibility(View.GONE);
        lockCurrentPassword.setVisibility(View.GONE);

        currentLockType = findCurrentLockType();

        switch (currentLockType) {
            case NONE:
                lockStatusText.setText(R.string.lock_status_none);
                lockCurrentNone.setVisibility(View.VISIBLE);
                break;
            case PASSWORD:
                lockStatusText.setText(R.string.lock_status_password);
                lockCurrentPassword.setVisibility(View.VISIBLE);
                break;
            case SECURITY_TOKEN:
                lockStatusText.setText(R.string.lock_status_security_token);
                break;
            case MIXED:
                lockStatusText.setText(R.string.lock_status_mixed);
                break;
            case UNAVAILABLE:
                lockStatusText.setText(R.string.lock_status_unavailable);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_mode_edit:
                enterEditModeOrShowPasswordDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void enterEditMode() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        hideKeyboard();

        activity.startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                actionMode = mode;

                mode.setTitle(R.string.title_edit_key_lock);

                viewAnimator.setDisplayedChildId(R.id.lock_layout_select);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                cachedCryptoInput = null;

                viewAnimator.setDisplayedChildId(R.id.lock_layout_status);
                refreshLockMethodDisplay();
            }
        });
    }

    private void enterEditModeOrShowPasswordDialog() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        switch (currentLockType) {
            case NONE:
            case MIXED: // mixed might fail? not sure, but we try anyways! D:
            case PASSWORD: {
                showPasswordDialog();
                break;
            }

            case SECURITY_TOKEN: {
                Notify.create(getActivity(), R.string.error_key_is_on_token, Style.ERROR).show();
                break;
            }

            case UNAVAILABLE: {
                Notify.create(getActivity(), R.string.error_key_not_available, Style.ERROR).show();
                break;
            }
        }
    }

    private void showPasswordDialog() {
        Intent intent = new Intent(getContext(), PassphraseDialogActivity.class);
        RequiredInputParcel requiredInput =
                RequiredInputParcel.createRequiredDecryptPassphrase(masterKeyId, masterKeyId);
        requiredInput.mSkipCaching = true;
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (operationHelper != null && operationHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_PASSPHRASE:
                if (resultCode == Activity.RESULT_OK) {
                    cachedCryptoInput = data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                    enterEditMode();
                }
                hideKeyboard();
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.lock_method_choice_none:
                checkAndRemovePassword();
                break;
            case R.id.lock_method_choice_password:
                viewAnimator.setDisplayedChildId(R.id.lock_layout_password);
                break;
            case R.id.lock_button_password_save:
                checkAndSavePassword();
                break;
            case R.id.lock_button_password_back:
                showLockChoices();
                break;
        }
    }

    private void checkAndRemovePassword() {
        if (currentLockType == LockType.NONE) {
            endActionMode();
            return;
        }

        setNewUnlockMechanism(ChangeUnlockParcel.createUnLockParcelForNewKey(new Passphrase()));
    }

    private void checkAndSavePassword() {
        hideKeyboard();

        Passphrase first = new Passphrase(lockPasswordField);
        Passphrase second = new Passphrase(lockPasswordRepeat);
        if (!first.equals(second)) {
            Notify.create(getActivity(), "Passwords don't match!", Style.ERROR).show();
            return;
        }

        setNewUnlockMechanism(ChangeUnlockParcel.createUnLockParcelForNewKey(first));
    }

    private void showLockChoices() {
        hideKeyboard();
        viewAnimator.setDisplayedChildId(R.id.lock_layout_select);

        lockPasswordField.setText("");
        lockPasswordRepeat.setText("");
    }

    private void setNewUnlockMechanism(ChangeUnlockParcel changeUnlockParcel) {
        SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildChangeKeyringParcel(masterKeyId, fingerprint);
        builder.setNewUnlock(changeUnlockParcel);

        editKey(builder.build());
    }

    private void editKey(final SaveKeyringParcel saveKeyringParcel) {
        hideKeyboard();

        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> editKeyCallback =
                new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {

            @Override
            public SaveKeyringParcel createOperationInput() {
                return saveKeyringParcel;
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {
                endActionMode();
                result.createNotify(getActivity()).show();
            }

            @Override
            public void onCryptoOperationCancelled() {
                actionMode.finish();
            }

            @Override
            public void onCryptoOperationError(EditKeyResult result) {
                endActionMode();
                result.createNotify(getActivity()).show();
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };
        operationHelper = new CryptoOperationHelper<>(1, this, editKeyCallback, R.string.progress_saving);
        operationHelper.cryptoOperation(cachedCryptoInput);
    }

    private void endActionMode() {
        if (actionMode == null) {
            return;
        }

        hideKeyboard();
        actionMode.finish();
    }

    public void hideKeyboard() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View decorView = activity.getWindow().getDecorView();
        if (decorView != null) {
            imm.hideSoftInputFromWindow(decorView.getApplicationWindowToken(), 0);
        }
    }
}
