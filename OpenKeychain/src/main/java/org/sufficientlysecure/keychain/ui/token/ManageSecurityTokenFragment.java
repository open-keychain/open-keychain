/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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

package org.sufficientlysecure.keychain.ui.token;


import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.PromoteKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.service.input.SecurityTokenChangePinParcel;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayFragment;
import org.sufficientlysecure.keychain.ui.SecurityTokenOperationActivity;
import org.sufficientlysecure.keychain.ui.SecurityTokenChangePinOperationActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper.AbstractCallback;
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenContract.ManageSecurityTokenMvpPresenter;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenContract.ManageSecurityTokenMvpView;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.widget.StatusIndicator;
import org.sufficientlysecure.keychain.ui.widget.StatusIndicator.Status;
import org.sufficientlysecure.keychain.ui.widget.ToolableViewAnimator;
import org.sufficientlysecure.keychain.util.FileHelper;


public class ManageSecurityTokenFragment extends Fragment implements ManageSecurityTokenMvpView, OnClickListener {
    private static final String ARG_TOKEN_INFO = "token_info";
    public static final int REQUEST_CODE_OPEN_FILE = 0;
    public static final int REQUEST_CODE_RESET = 1;
    public static final int REQUEST_CODE_CHANGE_PIN = 2;
    public static final int PERMISSION_READ_STORAGE = 0;

    ManageSecurityTokenMvpPresenter presenter;
    private ViewGroup statusLayoutGroup;
    private ToolableViewAnimator actionAnimator;
    private TextView unlockSubtitle;

    ImportKeyringParcel currentImportKeyringParcel;
    PromoteKeyringParcel currentPromoteKeyringParcel;
    private LayoutInflater layoutInflater;
    private StatusIndicator latestStatusIndicator;

    public static Fragment newInstance(SecurityTokenInfo tokenInfo) {
        ManageSecurityTokenFragment frag = new ManageSecurityTokenFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_TOKEN_INFO, tokenInfo);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        SecurityTokenInfo tokenInfo = args.getParcelable(ARG_TOKEN_INFO);

        presenter = new ManageSecurityTokenPresenter(getContext(), getLoaderManager(), tokenInfo);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.layoutInflater = inflater;
        View view = inflater.inflate(R.layout.create_security_token_import_fragment, container, false);

        statusLayoutGroup = (ViewGroup) view.findViewById(R.id.status_indicator_layout);
        actionAnimator = (ToolableViewAnimator) view.findViewById(R.id.action_animator);
        unlockSubtitle = (TextView) view.findViewById(R.id.button_unlock_subtitle);

        view.findViewById(R.id.button_import).setOnClickListener(this);
        view.findViewById(R.id.button_view_key).setOnClickListener(this);
        view.findViewById(R.id.button_retry).setOnClickListener(this);
        view.findViewById(R.id.button_reset_token_1).setOnClickListener(this);
        view.findViewById(R.id.button_reset_token_2).setOnClickListener(this);
        view.findViewById(R.id.button_reset_token_3).setOnClickListener(this);
        view.findViewById(R.id.button_reset_token_4).setOnClickListener(this);
        view.findViewById(R.id.button_reset_token_5).setOnClickListener(this);
        view.findViewById(R.id.button_unlock).setOnClickListener(this);
        view.findViewById(R.id.button_unlock_impossible).setOnClickListener(this);
        view.findViewById(R.id.button_load_file).setOnClickListener(this);
        view.findViewById(R.id.button_setup).setOnClickListener(this);

        setHasOptionsMenu(true);

        presenter.setView(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        presenter.detach();
        currentImportKeyringParcel = null;
        currentPromoteKeyringParcel = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.token_setup, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_log: {
                presenter.onMenuClickViewLog();
                return true;
            }
            case R.id.change_pin: {
                presenter.onMenuClickChangePin();
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        presenter.onActivityCreated();
    }

    @Override
    public void finishAndShowKey(long masterKeyId) {
        Activity activity = getActivity();

        Intent viewKeyIntent = new Intent(activity, ViewKeyActivity.class);
        // use the imported masterKeyId, not the one from the token, because
        // that one might* just have been a subkey of the imported key
        viewKeyIntent.setData(KeyRings.buildGenericKeyRingUri(masterKeyId));

        if (activity instanceof CreateKeyActivity) {
            ((CreateKeyActivity) activity).finishWithFirstTimeHandling(viewKeyIntent);
        } else {
            activity.startActivity(viewKeyIntent);
            activity.finish();
        }
    }

    @Override
    public void statusLineAdd(StatusLine statusLine) {
        if (latestStatusIndicator != null) {
            throw new IllegalStateException("Cannot set next status line before completing previous!");
        }

        View line = layoutInflater.inflate(R.layout.status_indicator_line, statusLayoutGroup, false);

        latestStatusIndicator = (StatusIndicator) line.findViewById(R.id.status_indicator);
        latestStatusIndicator.setDisplayedChild(Status.PROGRESS);
        TextView latestStatusText = (TextView) line.findViewById(R.id.status_text);
        latestStatusText.setText(statusLine.stringRes);

        statusLayoutGroup.addView(line);
    }

    @Override
    public void statusLineOk() {
        latestStatusIndicator.setDisplayedChild(Status.OK);
        latestStatusIndicator = null;
    }

    @Override
    public void statusLineError() {
        latestStatusIndicator.setDisplayedChild(Status.ERROR);
        latestStatusIndicator = null;
    }

    @Override
    public void resetStatusLines() {
        latestStatusIndicator = null;
        statusLayoutGroup.removeAllViews();
    }

    @Override
    public void showActionImport() {
        actionAnimator.setDisplayedChildId(R.id.token_layout_import);
    }

    @Override
    public void showActionViewKey() {
        actionAnimator.setDisplayedChildId(R.id.token_layout_ok);
    }

    @Override
    public void showActionRetryOrFromFile() {
        actionAnimator.setDisplayedChildId(R.id.token_layout_not_found);
    }

    @Override
    public void showActionLocked(int attemptsLeft) {
        if (attemptsLeft > 0) {
            actionAnimator.setDisplayedChildId(R.id.token_layout_locked);

            String unlockAttemptsText = getResources().getQuantityString(
                    R.plurals.token_unlock_attempts, attemptsLeft, attemptsLeft);
            unlockSubtitle.setText(unlockAttemptsText);
        } else {
            actionAnimator.setDisplayedChildId(R.id.token_layout_locked_hard);
        }
    }

    @Override
    public void showActionEmptyToken() {
        actionAnimator.setDisplayedChildId(R.id.token_layout_empty);
    }

    @Override
    public void showActionUnsupportedToken() {
        actionAnimator.setDisplayedChildId(R.id.token_layout_unsupported);
    }

    @Override
    public void hideAction() {
        actionAnimator.setDisplayedChild(0);
    }

    @Override
    public void operationImportKey(byte[] importKeyData) {
        if (currentImportKeyringParcel != null) {
            throw new IllegalStateException("Cannot trigger import operation twice!");
        }

        currentImportKeyringParcel =
                ImportKeyringParcel.createImportKeyringParcel(ParcelableKeyRing.createFromEncodedBytes(importKeyData));
        cryptoImportOperationHelper.setOperationMinimumDelay(1000L);
        cryptoImportOperationHelper.cryptoOperation();
    }

    @Override
    public void operationPromote(long masterKeyId, byte[] cardAid, List<byte[]> fingerprints) {
        if (currentImportKeyringParcel != null) {
            throw new IllegalStateException("Cannot trigger import operation twice!");
        }

        currentPromoteKeyringParcel = PromoteKeyringParcel.createPromoteKeyringParcel(
                masterKeyId, cardAid, fingerprints);
        cryptoPromoteOperationHelper.setOperationMinimumDelay(1000L);
        cryptoPromoteOperationHelper.cryptoOperation();
    }

    @Override
    public void operationResetSecurityToken() {
        Intent intent = new Intent(getActivity(), SecurityTokenOperationActivity.class);
        RequiredInputParcel resetP = RequiredInputParcel.createSecurityTokenReset();
        intent.putExtra(SecurityTokenOperationActivity.EXTRA_REQUIRED_INPUT, resetP);
        intent.putExtra(SecurityTokenOperationActivity.EXTRA_CRYPTO_INPUT, CryptoInputParcel.createCryptoInputParcel());
        startActivityForResult(intent, REQUEST_CODE_RESET);
    }

    @Override
    public void operationChangePinSecurityToken(String adminPin, String newPin) {
        Intent intent = new Intent(getActivity(), SecurityTokenChangePinOperationActivity.class);
        SecurityTokenChangePinParcel changePinParcel =
                SecurityTokenChangePinParcel.createSecurityTokenUnlock(adminPin, newPin);
        intent.putExtra(SecurityTokenChangePinOperationActivity.EXTRA_CHANGE_PIN_PARCEL, changePinParcel);
        startActivityForResult(intent, REQUEST_CODE_CHANGE_PIN);
    }

    @Override
    public void showFileSelectDialog() {
        FileHelper.openDocument(this, null, "*/*", false, REQUEST_CODE_OPEN_FILE);
    }

    @Override
    public void showConfirmResetDialog() {
        new Builder(ThemeChanger.getDialogThemeWrapper(getContext()))
                .setTitle(R.string.token_reset_confirm_title)
                .setMessage(R.string.token_reset_confirm_message)
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.token_reset_confirm_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.onClickConfirmReset();
                    }
                }).show();
    }

    @Override
    public void showAdminPinDialog() {
        AlertDialog adminPinDialog = ChangePinDialogHelper.createAdminPinDialog(getContext(), presenter);

        adminPinDialog.show();
    }

    @Override
    public void startCreateKeyForToken(SecurityTokenInfo tokenInfo) {
        CreateKeyActivity activity = (CreateKeyActivity) getActivity();
        activity.startCreateKeyForSecurityToken(tokenInfo);
    }

    @Override
    public void showErrorCannotUnlock() {
        Notify.create(getActivity(), R.string.token_error_locked_indefinitely, Style.ERROR).show();
    }

    @Override
    public void showErrorCannotReset(boolean isGnuk) {
        if (isGnuk) {
            Notify.create(getActivity(), R.string.token_error_cannot_reset_gnuk_old, Style.ERROR).show();
        } else {
            Notify.create(getActivity(), R.string.token_error_cannot_reset, Style.ERROR).show();
        }
    }

    @Override
    public void showDisplayLogActivity(OperationResult result) {
        Intent intent = new Intent(getActivity(), LogDisplayActivity.class);
        intent.putExtra(LogDisplayFragment.EXTRA_RESULT, result);
        startActivity(intent);
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    @Override
    public void requestStoragePermission() {
        requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, PERMISSION_READ_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode != PERMISSION_READ_STORAGE) {
            return;
        }

        boolean permissionWasGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (permissionWasGranted) {
            presenter.onStoragePermissionGranted();
        } else {
            presenter.onStoragePermissionDenied();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OPEN_FILE: {
                if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                    Uri fileUri = data.getData();
                    presenter.onFileSelected(fileUri);
                }
                break;
            }
            case REQUEST_CODE_RESET: {
                SecurityTokenInfo tokenInfo = data == null ? null :
                        data.<SecurityTokenInfo>getParcelableExtra(SecurityTokenOperationActivity.RESULT_TOKEN_INFO);
                if (resultCode == Activity.RESULT_OK) {
                    presenter.onSecurityTokenResetSuccess(tokenInfo);
                } else {
                    presenter.onSecurityTokenResetCanceled(tokenInfo);
                }
                break;
            }
            case REQUEST_CODE_CHANGE_PIN: {
                SecurityTokenInfo tokenInfo = data == null ? null :
                        data.<SecurityTokenInfo>getParcelableExtra(SecurityTokenOperationActivity.RESULT_TOKEN_INFO);
                if (resultCode == Activity.RESULT_OK) {
                    presenter.onSecurityTokenChangePinSuccess(tokenInfo);
                } else {
                    presenter.onSecurityTokenChangePinCanceled(tokenInfo);
                }
                break;
            }
            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_import: {
                presenter.onClickImport();
                break;
            }
            case R.id.button_retry: {
                presenter.onClickRetry();
                break;
            }
            case R.id.button_view_key: {
                presenter.onClickViewKey();
                break;
            }
            case R.id.button_load_file: {
                presenter.onClickLoadFile();
                break;
            }
            case R.id.button_reset_token_1:
            case R.id.button_reset_token_2:
            case R.id.button_reset_token_3:
            case R.id.button_reset_token_4:
            case R.id.button_reset_token_5: {
                presenter.onClickResetToken();
                break;
            }

            case R.id.button_unlock: {
                presenter.onClickUnlockToken();
                break;
            }
            case R.id.button_unlock_impossible: {
                presenter.onClickUnlockTokenImpossible();
                break;
            }

            case R.id.button_setup: {
                presenter.onClickSetupToken();
                break;
            }
        }
    }

    CryptoOperationHelper<ImportKeyringParcel, ImportKeyResult> cryptoImportOperationHelper =
            new CryptoOperationHelper<>(0, this, new AbstractCallback<ImportKeyringParcel, ImportKeyResult>() {
                @Override
                public ImportKeyringParcel createOperationInput() {
                    return currentImportKeyringParcel;
                }

                @Override
                public void onCryptoOperationSuccess(ImportKeyResult result) {
                    if (currentImportKeyringParcel == null) {
                        return;
                    }

                    currentImportKeyringParcel = null;
                    presenter.onImportSuccess(result);
                }

                @Override
                public void onCryptoOperationError(ImportKeyResult result) {
                    if (currentImportKeyringParcel == null) {
                        return;
                    }

                    currentImportKeyringParcel = null;
                    presenter.onImportError(result);
                }
            }, null);

    CryptoOperationHelper<PromoteKeyringParcel, PromoteKeyResult> cryptoPromoteOperationHelper =
            new CryptoOperationHelper<>(1, this, new AbstractCallback<PromoteKeyringParcel, PromoteKeyResult>() {
                @Override
                public PromoteKeyringParcel createOperationInput() {
                    return currentPromoteKeyringParcel;
                }

                @Override
                public void onCryptoOperationSuccess(PromoteKeyResult result) {
                    if (currentPromoteKeyringParcel == null) {
                        return;
                    }

                    currentPromoteKeyringParcel = null;
                    presenter.onPromoteSuccess(result);
                }

                @Override
                public void onCryptoOperationError(PromoteKeyResult result) {
                    if (currentPromoteKeyringParcel == null) {
                        return;
                    }

                    currentPromoteKeyringParcel = null;
                    presenter.onPromoteError(result);
                }
            }, null);

    enum StatusLine {
        CHECK_KEY (R.string.status_check_key),
        SEARCH_LOCAL (R.string.status_search_local),
        SEARCH_URI (R.string.status_search_uri),
        SEARCH_KEYSERVER (R.string.status_search_keyserver),
        IMPORT (R.string.status_import),
        TOKEN_PROMOTE(R.string.status_token_promote),
        TOKEN_CHECK (R.string.status_token_check),
        SEARCH_CONTENT_URI (R.string.status_content_uri);

        @StringRes
        private int stringRes;

        StatusLine(@StringRes int stringRes) {
            this.stringRes = stringRes;
        }
    }
}
