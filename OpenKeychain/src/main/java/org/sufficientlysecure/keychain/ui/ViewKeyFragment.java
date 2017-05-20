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

package org.sufficientlysecure.keychain.ui;


import java.io.IOException;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.base.LoaderFragment;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdViewFragment;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdViewFragment.OnIdentityLoadedListener;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdWizard;
import org.sufficientlysecure.keychain.ui.widget.KeyHealthCardView;
import org.sufficientlysecure.keychain.ui.widget.KeyHealthPresenter;
import org.sufficientlysecure.keychain.ui.widget.SystemContactCardView;
import org.sufficientlysecure.keychain.ui.widget.SystemContactPresenter;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;


public class ViewKeyFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_MASTER_KEY_ID = "master_key_id";
    public static final String ARG_IS_SECRET = "is_secret";
    public static final String ARG_POSTPONE_TYPE = "postpone_type";

    private ListView mUserIds;

    enum PostponeType {
        NONE, LINKED
    }

    boolean mIsSecret = false;

    private static final int LOADER_ID_USER_IDS = 1;
    private static final int LOADER_ID_LINKED_IDS = 2;
    private static final int LOADER_ID_LINKED_CONTACT = 3;
    private static final int LOADER_ID_SUBKEY_STATUS = 4;

    private UserIdsAdapter mUserIdsAdapter;
    private LinkedIdsAdapter mLinkedIdsAdapter;

    private Uri mDataUri;

    private PostponeType mPostponeType;

    private ListView mLinkedIds;
    private CardView mLinkedIdsCard;
    private TextView mLinkedIdsEmpty;
    private TextView mLinkedIdsExpander;

    SystemContactCardView mSystemContactCard;
    SystemContactPresenter mSystemContactPresenter;

    KeyHealthCardView mKeyHealthCard;
    KeyHealthPresenter mKeyHealthPresenter;

    private long mMasterKeyId;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyFragment newInstance(long masterKeyId, boolean isSecret, PostponeType postponeType) {
        ViewKeyFragment frag = new ViewKeyFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_MASTER_KEY_ID, masterKeyId);
        args.putBoolean(ARG_IS_SECRET, isSecret);
        args.putString(ARG_POSTPONE_TYPE, postponeType.toString());

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_fragment, getContainer());

        mUserIds = (ListView) view.findViewById(R.id.view_key_user_ids);
        Button userIdsEditButton = (Button) view.findViewById(R.id.view_key_card_user_ids_edit);
        mLinkedIdsCard = (CardView) view.findViewById(R.id.card_linked_ids);
        mLinkedIds = (ListView) view.findViewById(R.id.view_key_linked_ids);
        mLinkedIdsExpander = (TextView) view.findViewById(R.id.view_key_linked_ids_expander);
        mLinkedIdsEmpty = (TextView)  view.findViewById(R.id.view_key_linked_ids_empty);
        Button linkedIdsAddButton = (Button) view.findViewById(R.id.view_key_card_linked_ids_add);

        userIdsEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editIdentities(mDataUri);
            }
        });

        linkedIdsAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLinkedIdentity(mDataUri);
            }
        });

        mUserIds.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showUserIdInfo(position);
            }
        });
        mLinkedIds.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showLinkedId(position);
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

    private void addLinkedIdentity(Uri dataUri) {
        Intent intent = new Intent(getActivity(), LinkedIdWizard.class);
        intent.setData(dataUri);
        startActivity(intent);
        getActivity().finish();
    }

    private void showLinkedId(final int position) {
        final LinkedIdViewFragment frag;
        try {
            frag = mLinkedIdsAdapter.getLinkedIdFragment(mDataUri, position, mMasterKeyId);
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException", e);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Transition trans = TransitionInflater.from(getActivity())
                    .inflateTransition(R.transition.linked_id_card_trans);
            // setSharedElementReturnTransition(trans);
            setExitTransition(new Fade());
            frag.setSharedElementEnterTransition(trans);
        }

        getFragmentManager().beginTransaction()
                .add(R.id.view_key_fragment, frag)
                .hide(frag)
                .commit();

        frag.setOnIdentityLoadedListener(new OnIdentityLoadedListener() {
            @Override
            public void onIdentityLoaded() {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        getFragmentManager().beginTransaction()
                                .show(frag)
                                .addSharedElement(mLinkedIdsCard, "card_linked_ids")
                                .remove(ViewKeyFragment.this)
                                .addToBackStack("linked_id")
                                .commit();
                    }
                });
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mMasterKeyId = getArguments().getLong(ARG_MASTER_KEY_ID);
        mDataUri = KeyRings.buildGenericKeyRingUri(mMasterKeyId);
        mIsSecret = getArguments().getBoolean(ARG_IS_SECRET);
        mPostponeType = PostponeType.valueOf(getArguments().getString(ARG_POSTPONE_TYPE));

        // load user ids after we know if it's a secret key
        mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0, !mIsSecret, null);
        mUserIds.setAdapter(mUserIdsAdapter);

        // initialize loaders, which will take care of auto-refresh on change
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
        initLinkedIds(mIsSecret);
        initCardButtonsVisibility(mIsSecret);

        mSystemContactPresenter = new SystemContactPresenter(
                getContext(), mSystemContactCard, LOADER_ID_LINKED_CONTACT, mMasterKeyId, mIsSecret);
        mSystemContactPresenter.startLoader(getLoaderManager());

        mKeyHealthPresenter = new KeyHealthPresenter(
                getContext(), mKeyHealthCard, LOADER_ID_SUBKEY_STATUS, mMasterKeyId, mIsSecret);
        mKeyHealthPresenter.startLoader(getLoaderManager());
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

            case LOADER_ID_LINKED_IDS: {
                return LinkedIdsAdapter.createLoader(getActivity(), mDataUri);
            }

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

            case LOADER_ID_LINKED_IDS: {
                mLinkedIdsAdapter.swapCursor(data);

                if (mIsSecret) {
                    mLinkedIdsCard.setVisibility(View.VISIBLE);
                    mLinkedIdsEmpty.setVisibility(mLinkedIdsAdapter.getCount() > 0 ? View.GONE : View.VISIBLE);
                } else {
                    mLinkedIdsCard.setVisibility(mLinkedIdsAdapter.getCount() > 0 ? View.VISIBLE : View.GONE);
                    mLinkedIdsEmpty.setVisibility(View.GONE);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mPostponeType == PostponeType.LINKED) {
                    mLinkedIdsCard.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
                        @TargetApi(VERSION_CODES.LOLLIPOP)
                        @Override
                        public boolean onPreDraw() {
                            mLinkedIdsCard.getViewTreeObserver().removeOnPreDrawListener(this);
                            getActivity().startPostponedEnterTransition();
                            return true;
                        }
                    });
                }
                break;
            }

            case LOADER_ID_LINKED_CONTACT:
            case LOADER_ID_SUBKEY_STATUS: {
                throw new IllegalStateException("This callback should never end up here!");
            }
        }
    }

    private void initLinkedIds(boolean isSecret) {
        if (!Preferences.getPreferences(getActivity()).getExperimentalEnableLinkedIdentities()) {
            return;
        }

        mLinkedIdsAdapter = new LinkedIdsAdapter(getActivity(), null, 0, isSecret, mLinkedIdsExpander);
        mLinkedIds.setAdapter(mLinkedIdsAdapter);
        getLoaderManager().initLoader(LOADER_ID_LINKED_IDS, null, this);
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
            case LOADER_ID_LINKED_IDS: {
                mLinkedIdsAdapter.swapCursor(null);
                break;
            }
        }
    }

    public boolean isValidForData(boolean isSecret) {
        return isSecret == mIsSecret;
    }

}
