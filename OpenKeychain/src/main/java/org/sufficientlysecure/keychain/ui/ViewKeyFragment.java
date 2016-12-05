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
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.base.LoaderFragment;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdViewFragment;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdViewFragment.OnIdentityLoadedListener;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdWizard;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

public class ViewKeyFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";
    public static final String ARG_POSTPONE_TYPE = "postpone_type";

    private ListView mUserIds;

    enum PostponeType {
        NONE, LINKED
    }

    boolean mIsSecret = false;

    private static final int LOADER_ID_UNIFIED = 0;
    private static final int LOADER_ID_USER_IDS = 1;
    private static final int LOADER_ID_LINKED_IDS = 2;
    private static final int LOADER_ID_LINKED_CONTACT = 3;

    private static final String LOADER_EXTRA_LINKED_CONTACT_MASTER_KEY_ID
            = "loader_linked_contact_master_key_id";
    private static final String LOADER_EXTRA_LINKED_CONTACT_IS_SECRET
            = "loader_linked_contact_is_secret";

    private UserIdsAdapter mUserIdsAdapter;
    private LinkedIdsAdapter mLinkedIdsAdapter;

    private Uri mDataUri;
    private PostponeType mPostponeType;

    private CardView mSystemContactCard;
    private LinearLayout mSystemContactLayout;
    private ImageView mSystemContactPicture;
    private TextView mSystemContactName;

    private ListView mLinkedIds;
    private CardView mLinkedIdsCard;
    private TextView mLinkedIdsEmpty;
    private byte[] mFingerprint;
    private TextView mLinkedIdsExpander;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyFragment newInstance(Uri dataUri, PostponeType postponeType) {
        ViewKeyFragment frag = new ViewKeyFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);
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
        mSystemContactCard = (CardView) view.findViewById(R.id.linked_system_contact_card);
        mSystemContactLayout = (LinearLayout) view.findViewById(R.id.system_contact_layout);
        mSystemContactName = (TextView) view.findViewById(R.id.system_contact_name);
        mSystemContactPicture = (ImageView) view.findViewById(R.id.system_contact_picture);

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
            frag = mLinkedIdsAdapter.getLinkedIdFragment(mDataUri, position, mFingerprint);
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

    /**
     * Hides card if no linked system contact exists. Sets name, picture
     * and onClickListener for the linked system contact's layout.
     * In the case of a secret key, "me" (own profile) contact details are loaded.
     */
    private void loadLinkedSystemContact(final long contactId) {
        // contact doesn't exist, stop
        if (contactId == -1) return;

        final Context context = mSystemContactName.getContext();
        ContactHelper contactHelper = new ContactHelper(context);

        String contactName = null;

        if (mIsSecret) {//all secret keys are linked to "me" profile in contacts
            List<String> mainProfileNames = contactHelper.getMainProfileContactName();
            if (mainProfileNames != null && mainProfileNames.size() > 0) {
                contactName = mainProfileNames.get(0);
            }
        } else {
            contactName = contactHelper.getContactName(contactId);
        }

        if (contactName != null) {//contact name exists for given master key
            showLinkedSystemContact();

            mSystemContactName.setText(contactName);

            Bitmap picture;
            if (mIsSecret) {
                picture = contactHelper.loadMainProfilePhoto(false);
            } else {
                picture = contactHelper.loadPhotoByContactId(contactId, false);
            }
            if (picture != null) mSystemContactPicture.setImageBitmap(picture);

            mSystemContactLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchContactActivity(contactId, context);
                }
            });
        } else {
            hideLinkedSystemContact();
        }
    }

    private void hideLinkedSystemContact() {
        mSystemContactCard.setVisibility(View.GONE);
    }

    private void showLinkedSystemContact() {
        mSystemContactCard.setVisibility(View.VISIBLE);
    }

    /**
     * launches the default android Contacts app to view a contact with the passed
     * contactId (CONTACT_ID column from ContactsContract.RawContact table which is _ID column in
     * ContactsContract.Contact table)
     *
     * @param contactId _ID for row in ContactsContract.Contacts table
     */
    private void launchContactActivity(final long contactId, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactId));
        intent.setData(uri);
        context.startActivity(intent);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        mPostponeType = PostponeType.valueOf(getArguments().getString(ARG_POSTPONE_TYPE));
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }

        loadData(dataUri);
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

    static final String[] UNIFIED_PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.IS_EXPIRED,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
            KeychainContract.KeyRings.FINGERPRINT,
            KeychainContract.KeyRings.HAS_ENCRYPT
    };

    static final int INDEX_MASTER_KEY_ID = 1;
    @SuppressWarnings("unused")
    static final int INDEX_USER_ID = 2;
    @SuppressWarnings("unused")
    static final int INDEX_IS_REVOKED = 3;
    @SuppressWarnings("unused")
    static final int INDEX_IS_EXPIRED = 4;
    @SuppressWarnings("unused")
    static final int INDEX_VERIFIED = 5;
    static final int INDEX_HAS_ANY_SECRET = 6;
    static final int INDEX_FINGERPRINT = 7;
    @SuppressWarnings("unused")
    static final int INDEX_HAS_ENCRYPT = 8;

    private static final String[] RAW_CONTACT_PROJECTION = {
            ContactsContract.RawContacts.CONTACT_ID
    };

    private static final int INDEX_CONTACT_ID = 0;

    private void loadData(Uri dataUri) {
        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri);

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        switch (id) {
            case LOADER_ID_UNIFIED: {
                setContentShown(false, false);
                Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri, UNIFIED_PROJECTION, null, null, null);
            }

            case LOADER_ID_USER_IDS: {
                return UserIdsAdapter.createLoader(getActivity(), mDataUri);
            }

            case LOADER_ID_LINKED_IDS: {
                return LinkedIdsAdapter.createLoader(getActivity(), mDataUri);
            }

            case LOADER_ID_LINKED_CONTACT: {
                // we need a separate loader for linked contact
                // to ensure refreshing on verification

                // passed in args to explicitly specify their need
                long masterKeyId = args.getLong(LOADER_EXTRA_LINKED_CONTACT_MASTER_KEY_ID);
                boolean isSecret = args.getBoolean(LOADER_EXTRA_LINKED_CONTACT_IS_SECRET);

                Uri baseUri = isSecret ? ContactsContract.Profile.CONTENT_RAW_CONTACTS_URI :
                        ContactsContract.RawContacts.CONTENT_URI;

                return new CursorLoader(
                        getActivity(),
                        baseUri,
                        RAW_CONTACT_PROJECTION,
                        ContactsContract.RawContacts.ACCOUNT_TYPE + "=? AND " +
                                ContactsContract.RawContacts.SOURCE_ID + "=? AND " +
                                ContactsContract.RawContacts.DELETED + "=?",
                        new String[]{
                                Constants.ACCOUNT_TYPE,
                                Long.toString(masterKeyId),
                                "0" // "0" for "not deleted"
                        },
                        null);
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
            case LOADER_ID_UNIFIED: {
                if (data.getCount() == 1 && data.moveToFirst()) {

                    mIsSecret = data.getInt(INDEX_HAS_ANY_SECRET) != 0;
                    mFingerprint = data.getBlob(INDEX_FINGERPRINT);
                    long masterKeyId = data.getLong(INDEX_MASTER_KEY_ID);

                    // init other things after we know if it's a secret key
                    initUserIds(mIsSecret);
                    initLinkedIds(mIsSecret);
                    initLinkedContactLoader(masterKeyId, mIsSecret);
                    initCardButtonsVisibility(mIsSecret);
                }
                break;
            }

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

            case LOADER_ID_LINKED_CONTACT: {
                if (data.moveToFirst()) { // if we have a linked contact
                    long contactId = data.getLong(INDEX_CONTACT_ID);
                    loadLinkedSystemContact(contactId);
                }
                break;
            }
        }
    }

    private void initUserIds(boolean isSecret) {
        mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0, !isSecret, null);
        mUserIds.setAdapter(mUserIdsAdapter);
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
    }

    private void initLinkedIds(boolean isSecret) {
        if (Preferences.getPreferences(getActivity()).getExperimentalEnableLinkedIdentities()) {
            mLinkedIdsAdapter =
                    new LinkedIdsAdapter(getActivity(), null, 0, isSecret, mLinkedIdsExpander);
            mLinkedIds.setAdapter(mLinkedIdsAdapter);
            getLoaderManager().initLoader(LOADER_ID_LINKED_IDS, null, this);
        }
    }

    private void initLinkedContactLoader(long masterKeyId, boolean isSecret) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_DENIED) {
            Log.w(Constants.TAG, "loading linked system contact not possible READ_CONTACTS permission denied!");
            hideLinkedSystemContact();
            return;
        }

        Bundle linkedContactData = new Bundle();
        linkedContactData.putLong(LOADER_EXTRA_LINKED_CONTACT_MASTER_KEY_ID, masterKeyId);
        linkedContactData.putBoolean(LOADER_EXTRA_LINKED_CONTACT_IS_SECRET, isSecret);

        // initialises loader for contact query so we can listen to any updates
        getLoaderManager().initLoader(LOADER_ID_LINKED_CONTACT, linkedContactData, this);
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

}
