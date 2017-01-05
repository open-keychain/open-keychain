/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ViewAnimator;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.SubkeysAddedAdapter;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.LoaderFragment;
import org.sufficientlysecure.keychain.ui.dialog.AddSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditSubkeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EditSubkeyExpiryDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;

public class ViewKeyAdvSubkeysFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "data_uri";

    private static final int LOADER_ID_UNIFIED = 0;
    private static final int LOADER_ID_SUBKEYS = 1;

    private ListView mSubkeysList;
    private ListView mSubkeysAddedList;
    private View mSubkeysAddedLayout;
    private ViewAnimator mSubkeyAddFabLayout;

    private SubkeysAdapter mSubkeysAdapter;
    private SubkeysAddedAdapter mSubkeysAddedAdapter;

    private CryptoOperationHelper<SaveKeyringParcel, EditKeyResult> mEditKeyHelper;

    private Uri mDataUri;

    private long mMasterKeyId;
    private byte[] mFingerprint;
    private boolean mHasSecret;
    private SaveKeyringParcel mEditModeSaveKeyringParcel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_adv_subkeys_fragment, getContainer());

        mSubkeysList = (ListView) view.findViewById(R.id.view_key_subkeys);
        mSubkeysAddedList = (ListView) view.findViewById(R.id.view_key_subkeys_added);
        mSubkeysAddedLayout = view.findViewById(R.id.view_key_subkeys_add_layout);

        mSubkeysList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editSubkey(position);
            }
        });

        View footer = new View(getActivity());
        int spacing = (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, 72, getResources().getDisplayMetrics()
        );
        android.widget.AbsListView.LayoutParams params = new android.widget.AbsListView.LayoutParams(
                android.widget.AbsListView.LayoutParams.MATCH_PARENT,
                spacing
        );
        footer.setLayoutParams(params);
        mSubkeysAddedList.addFooterView(footer, null, false);

        mSubkeyAddFabLayout = (ViewAnimator) view.findViewById(R.id.view_key_subkey_fab_layout);
        view.findViewById(R.id.view_key_subkey_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addSubkey();
            }
        });

        setHasOptionsMenu(true);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }

        loadData(dataUri);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mEditKeyHelper != null) {
            mEditKeyHelper.handleActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void loadData(Uri dataUri) {
        mDataUri = dataUri;

        // Create an empty adapter we will use to display the loaded data.
        mSubkeysAdapter = new SubkeysAdapter(getActivity(), null, 0);
        mSubkeysList.setAdapter(mSubkeysAdapter);

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);
        getLoaderManager().initLoader(LOADER_ID_SUBKEYS, null, this);
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
            KeychainContract.KeyRings.FINGERPRINT,
    };

    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_HAS_ANY_SECRET = 2;
    static final int INDEX_FINGERPRINT = 3;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_UNIFIED: {
                Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri,
                        PROJECTION, null, null, null);
            }

            case LOADER_ID_SUBKEYS: {
                setContentShown(false);

                Uri subkeysUri = KeychainContract.Keys.buildKeysUri(mDataUri);
                return new CursorLoader(getActivity(), subkeysUri,
                        SubkeysAdapter.SUBKEYS_PROJECTION, null, null, null);
            }

            default:
                return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Avoid NullPointerExceptions, if we get an empty result set.
        if (data.getCount() == 0) {
            return;
        }

        switch (loader.getId()) {
            case LOADER_ID_UNIFIED: {
                data.moveToFirst();

                mMasterKeyId = data.getLong(INDEX_MASTER_KEY_ID);
                mHasSecret = data.getInt(INDEX_HAS_ANY_SECRET) != 0;
                mFingerprint = data.getBlob(INDEX_FINGERPRINT);
                break;
            }
            case LOADER_ID_SUBKEYS: {
                // Swap the new cursor in. (The framework will take care of closing the
                // old cursor once we return.)
                mSubkeysAdapter.swapCursor(data);

                // TODO: maybe show not before both are loaded!
                setContentShown(true);
                break;
            }
        }

    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        mSubkeysAdapter.swapCursor(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_mode_edit:
                enterEditMode();
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
        activity.startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {

                mEditModeSaveKeyringParcel = new SaveKeyringParcel(mMasterKeyId, mFingerprint);

                mSubkeysAddedAdapter =
                        new SubkeysAddedAdapter(getActivity(), mEditModeSaveKeyringParcel.mAddSubKeys, false);
                mSubkeysAddedList.setAdapter(mSubkeysAddedAdapter);
                mSubkeysAddedLayout.setVisibility(View.VISIBLE);
                mSubkeyAddFabLayout.setDisplayedChild(1);

                mSubkeysAdapter.setEditMode(mEditModeSaveKeyringParcel);
                getLoaderManager().restartLoader(LOADER_ID_SUBKEYS, null, ViewKeyAdvSubkeysFragment.this);

                mode.setTitle(R.string.title_edit_subkeys);
                mode.getMenuInflater().inflate(R.menu.action_edit_uids, menu);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                editKey(mode);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mEditModeSaveKeyringParcel = null;
                mSubkeysAdapter.setEditMode(null);
                mSubkeysAddedLayout.setVisibility(View.GONE);
                mSubkeyAddFabLayout.setDisplayedChild(0);
                getLoaderManager().restartLoader(LOADER_ID_SUBKEYS, null, ViewKeyAdvSubkeysFragment.this);
            }
        });
    }

    private void addSubkey() {
        boolean willBeMasterKey;
        if (mSubkeysAdapter != null) {
            willBeMasterKey = mSubkeysAdapter.getCount() == 0 && mSubkeysAddedAdapter.getCount() == 0;
        } else {
            willBeMasterKey = mSubkeysAddedAdapter.getCount() == 0;
        }

        AddSubkeyDialogFragment addSubkeyDialogFragment =
                AddSubkeyDialogFragment.newInstance(willBeMasterKey);
        addSubkeyDialogFragment
                .setOnAlgorithmSelectedListener(
                        new AddSubkeyDialogFragment.OnAlgorithmSelectedListener() {
                            @Override
                            public void onAlgorithmSelected(SaveKeyringParcel.SubkeyAdd newSubkey) {
                                mSubkeysAddedAdapter.add(newSubkey);
                            }
                        }
                );
        addSubkeyDialogFragment.show(getActivity().getSupportFragmentManager(), "addSubkeyDialog");
    }

    private void editSubkey(final int position) {
        final long keyId = mSubkeysAdapter.getKeyId(position);

        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditSubkeyDialogFragment.MESSAGE_CHANGE_EXPIRY:
                        editSubkeyExpiry(position);
                        break;
                    case EditSubkeyDialogFragment.MESSAGE_REVOKE:
                        // toggle
                        if (mEditModeSaveKeyringParcel.mRevokeSubKeys.contains(keyId)) {
                            mEditModeSaveKeyringParcel.mRevokeSubKeys.remove(keyId);
                        } else {
                            mEditModeSaveKeyringParcel.mRevokeSubKeys.add(keyId);
                        }
                        break;
                    case EditSubkeyDialogFragment.MESSAGE_STRIP: {
                        SecretKeyType secretKeyType = mSubkeysAdapter.getSecretKeyType(position);
                        if (secretKeyType == SecretKeyType.GNU_DUMMY) {
                            // Key is already stripped; this is a no-op.
                            break;
                        }

                        SubkeyChange change = mEditModeSaveKeyringParcel.getSubkeyChange(keyId);
                        if (change == null) {
                            mEditModeSaveKeyringParcel.mChangeSubKeys.add(new SubkeyChange(keyId, true, false));
                            break;
                        }
                        // toggle
                        change.mDummyStrip = !change.mDummyStrip;
                        if (change.mDummyStrip && change.mMoveKeyToSecurityToken) {
                            // User had chosen to divert key, but now wants to strip it instead.
                            change.mMoveKeyToSecurityToken = false;
                        }
                        break;
                    }
                    case EditSubkeyDialogFragment.MESSAGE_MOVE_KEY_TO_SECURITY_TOKEN: {
                        SecretKeyType secretKeyType = mSubkeysAdapter.getSecretKeyType(position);
                        if (secretKeyType == SecretKeyType.DIVERT_TO_CARD ||
                                secretKeyType == SecretKeyType.GNU_DUMMY) {
                            Notify.create(getActivity(), R.string.edit_key_error_bad_security_token_stripped, Notify.Style.ERROR)
                                    .show();
                            break;
                        }

                        switch (mSubkeysAdapter.getAlgorithm(position)) {
                            case PublicKeyAlgorithmTags.RSA_GENERAL:
                            case PublicKeyAlgorithmTags.RSA_ENCRYPT:
                            case PublicKeyAlgorithmTags.RSA_SIGN:
                                if (mSubkeysAdapter.getKeySize(position) < 2048) {
                                    Notify.create(getActivity(), R.string.edit_key_error_bad_security_token_size, Notify.Style.ERROR)
                                            .show();
                                }
                                break;

                            case PublicKeyAlgorithmTags.ECDH:
                            case PublicKeyAlgorithmTags.ECDSA:
                                final ASN1ObjectIdentifier curve = NISTNamedCurves.getOID(mSubkeysAdapter.getCurveOid(position));
                                if (!curve.equals(NISTNamedCurves.getOID("P-256")) &&
                                        !curve.equals(NISTNamedCurves.getOID("P-384")) &&
                                        !curve.equals(NISTNamedCurves.getOID("P-521"))) {
                                    Notify.create(getActivity(), R.string.edit_key_error_bad_security_token_curve, Notify.Style.ERROR)
                                            .show();
                                }
                                break;

                            default:
                                Notify.create(getActivity(), R.string.edit_key_error_bad_security_token_algo, Notify.Style.ERROR)
                                        .show();
                                break;
                        }

                        SubkeyChange change;
                        change = mEditModeSaveKeyringParcel.getSubkeyChange(keyId);
                        if (change == null) {
                            mEditModeSaveKeyringParcel.mChangeSubKeys.add(
                                    new SubkeyChange(keyId, false, true)
                            );
                            break;
                        }
                        // toggle
                        change.mMoveKeyToSecurityToken = !change.mMoveKeyToSecurityToken;
                        if (change.mMoveKeyToSecurityToken && change.mDummyStrip) {
                            // User had chosen to strip key, but now wants to divert it.
                            change.mDummyStrip = false;
                        }
                        break;
                    }
                }
                getLoaderManager().getLoader(LOADER_ID_SUBKEYS).forceLoad();
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                EditSubkeyDialogFragment dialogFragment =
                        EditSubkeyDialogFragment.newInstance(messenger);

                dialogFragment.show(getActivity().getSupportFragmentManager(), "editSubkeyDialog");
            }
        });
    }

    private void editSubkeyExpiry(final int position) {
        final long keyId = mSubkeysAdapter.getKeyId(position);
        final Long creationDate = mSubkeysAdapter.getCreationDate(position);
        final Long expiryDate = mSubkeysAdapter.getExpiryDate(position);

        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case EditSubkeyExpiryDialogFragment.MESSAGE_NEW_EXPIRY:
                        mEditModeSaveKeyringParcel.getOrCreateSubkeyChange(keyId).mExpiry =
                                (Long) message.getData().getSerializable(
                                        EditSubkeyExpiryDialogFragment.MESSAGE_DATA_EXPIRY);
                        break;
                }
                getLoaderManager().getLoader(LOADER_ID_SUBKEYS).forceLoad();
            }
        };

        // Create a new Messenger for the communication back
        final Messenger messenger = new Messenger(returnHandler);

        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                EditSubkeyExpiryDialogFragment dialogFragment =
                        EditSubkeyExpiryDialogFragment.newInstance(messenger, creationDate, expiryDate);

                dialogFragment.show(getActivity().getSupportFragmentManager(), "editSubkeyExpiryDialog");
            }
        });
    }


    private void editKey(final ActionMode mode) {
        CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult> editKeyCallback
                = new CryptoOperationHelper.Callback<SaveKeyringParcel, EditKeyResult>() {

            @Override
            public SaveKeyringParcel createOperationInput() {
                return mEditModeSaveKeyringParcel;
            }

            @Override
            public void onCryptoOperationSuccess(EditKeyResult result) {
                mode.finish();
                result.createNotify(getActivity()).show();
            }

            @Override
            public void onCryptoOperationCancelled() {
                mode.finish();
            }

            @Override
            public void onCryptoOperationError(EditKeyResult result) {
                mode.finish();
                result.createNotify(getActivity()).show();
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };
        mEditKeyHelper = new CryptoOperationHelper<>(1, this, editKeyCallback, R.string.progress_saving);
        mEditKeyHelper.cryptoOperation();
    }

}
