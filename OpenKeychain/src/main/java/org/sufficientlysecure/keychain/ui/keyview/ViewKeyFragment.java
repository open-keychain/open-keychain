/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.ui.keyview;


import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.EditIdentitiesActivity;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.base.LoaderFragment;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyHealthCardView;
import org.sufficientlysecure.keychain.ui.keyview.presenter.KeyHealthPresenter;
import org.sufficientlysecure.keychain.ui.keyview.view.LinkedIdentitiesCardView;
import org.sufficientlysecure.keychain.ui.keyview.presenter.LinkedIdentitiesPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.LinkedIdentitiesPresenter.LinkedIdsFragMvpView;
import org.sufficientlysecure.keychain.ui.keyview.view.SystemContactCardView;
import org.sufficientlysecure.keychain.ui.keyview.presenter.SystemContactPresenter;
import org.sufficientlysecure.keychain.util.Preferences;


public class ViewKeyFragment extends LoaderFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        LinkedIdsFragMvpView {

    public static final String ARG_MASTER_KEY_ID = "master_key_id";
    public static final String ARG_IS_SECRET = "is_secret";

    private ListView mUserIds;

    boolean mIsSecret = false;

    private static final int LOADER_ID_USER_IDS = 1;
    private static final int LOADER_ID_LINKED_IDS = 2;
    private static final int LOADER_ID_LINKED_CONTACT = 3;
    private static final int LOADER_ID_SUBKEY_STATUS = 4;

    private UserIdsAdapter mUserIdsAdapter;

    private Uri mDataUri;

    LinkedIdentitiesCardView mLinkedIdsCard;
    LinkedIdentitiesPresenter mLinkedIdentitiesPresenter;

    SystemContactCardView mSystemContactCard;
    SystemContactPresenter mSystemContactPresenter;

    KeyHealthCardView mKeyHealthCard;
    KeyHealthPresenter mKeyHealthPresenter;

    private long mMasterKeyId;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyFragment newInstance(long masterKeyId, boolean isSecret) {
        ViewKeyFragment frag = new ViewKeyFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_MASTER_KEY_ID, masterKeyId);
        args.putBoolean(ARG_IS_SECRET, isSecret);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_fragment, getContainer());

        mUserIds = (ListView) view.findViewById(R.id.view_key_user_ids);
        Button userIdsEditButton = (Button) view.findViewById(R.id.view_key_card_user_ids_edit);
        mLinkedIdsCard = (LinkedIdentitiesCardView) view.findViewById(R.id.card_linked_ids);

        userIdsEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editIdentities(mDataUri);
            }
        });

        mUserIds.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showUserIdInfo(position);
            }
        });

        mSystemContactCard = (SystemContactCardView) view.findViewById(R.id.linked_system_contact_card);
        mKeyHealthCard = (KeyHealthCardView) view.findViewById(R.id.subkey_status_card);

        return root;
    }

    private void editIdentities(Uri dataUri) {
        Intent editIntent = new Intent(getActivity(), EditIdentitiesActivity.class);
        editIntent.setData(KeychainContract.KeyRingData.buildSecretKeyRingUri(dataUri));
        startActivityForResult(editIntent, 0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMasterKeyId = getArguments().getLong(ARG_MASTER_KEY_ID);
        mDataUri = KeyRings.buildGenericKeyRingUri(mMasterKeyId);
        mIsSecret = getArguments().getBoolean(ARG_IS_SECRET);

        // load user ids after we know if it's a secret key
        mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0, !mIsSecret);
        mUserIds.setAdapter(mUserIdsAdapter);

        // initialize loaders, which will take care of auto-refresh on change
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
        initCardButtonsVisibility(mIsSecret);

        if (Preferences.getPreferences(getActivity()).getExperimentalEnableLinkedIdentities()) {
            mLinkedIdentitiesPresenter = new LinkedIdentitiesPresenter(
                    getContext(), mLinkedIdsCard, this, LOADER_ID_LINKED_IDS, mMasterKeyId, mIsSecret);
            mLinkedIdentitiesPresenter.startLoader(getLoaderManager());
        }

        mSystemContactPresenter = new SystemContactPresenter(
                getContext(), mSystemContactCard, LOADER_ID_LINKED_CONTACT, mMasterKeyId, mIsSecret);
        mSystemContactPresenter.startLoader(getLoaderManager());

        mKeyHealthPresenter = new KeyHealthPresenter(
                getContext(), mKeyHealthCard, LOADER_ID_SUBKEY_STATUS, mMasterKeyId, mIsSecret);
        mKeyHealthPresenter.startLoader(getLoaderManager());
    }

    @Override
    public void switchToFragment(final Fragment frag, final String backStackName) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.view_key_fragment, frag)
                        .addToBackStack(backStackName)
                        .commit();
            }
        });
    }

    private void showUserIdInfo(final int position) {
        if (!mIsSecret) {
            final boolean isRevoked = mUserIdsAdapter.getIsRevoked(position);
            final int isVerified = mUserIdsAdapter.getIsVerified(position);

            DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
                public void run() {
                    UserIdInfoDialogFragment dialogFragment =
                            UserIdInfoDialogFragment.newInstance(isRevoked, isVerified);

                    dialogFragment.show(getActivity().getSupportFragmentManager(), "userIdInfoDialog");
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(getActivity()).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        switch (id) {
            case LOADER_ID_USER_IDS: {
                return UserIdsAdapter.createLoader(getActivity(), mDataUri);
            }

            case LOADER_ID_LINKED_IDS:
            case LOADER_ID_LINKED_CONTACT:
            case LOADER_ID_SUBKEY_STATUS: {
                throw new IllegalStateException("This callback should never end up here!");
            }

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* TODO better error handling? May cause problems when a key is deleted,
         * because the notification triggers faster than the activity closes.
         */
        if (data == null) {
            return;
        }
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS: {
                setContentShown(true, false);
                mUserIdsAdapter.swapCursor(data);

                break;
            }

            case LOADER_ID_LINKED_IDS:
            case LOADER_ID_LINKED_CONTACT:
            case LOADER_ID_SUBKEY_STATUS: {
                throw new IllegalStateException("This callback should never end up here!");
            }
        }
    }

    private void initCardButtonsVisibility(boolean isSecret) {
        LinearLayout buttonsUserIdsLayout =
                (LinearLayout) getActivity().findViewById(R.id.view_key_card_user_ids_buttons);
        LinearLayout buttonsLinkedIdsLayout =
                (LinearLayout) getActivity().findViewById(R.id.view_key_card_linked_ids_buttons);
        if (isSecret) {
            buttonsUserIdsLayout.setVisibility(View.VISIBLE);
            buttonsLinkedIdsLayout.setVisibility(View.VISIBLE);
        } else {
            buttonsUserIdsLayout.setVisibility(View.GONE);
            buttonsLinkedIdsLayout.setVisibility(View.GONE);
        }
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS: {
                mUserIdsAdapter.swapCursor(null);
                break;
            }
        }
    }

    public boolean isValidForData(boolean isSecret) {
        return isSecret == mIsSecret;
    }

}
