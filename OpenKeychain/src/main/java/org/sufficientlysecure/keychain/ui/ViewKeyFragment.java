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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.util.List;

public class ViewKeyFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";

    private ListView mUserIds;
    //private ListView mLinkedSystemContact;

    boolean mIsSecret = false;
    boolean mSystemContactLoaded = false;

    LinearLayout mSystemContactLayout;
    ImageView mSystemContactPicture;
    TextView mSystemContactName;

    private static final int LOADER_ID_UNIFIED = 0;
    private static final int LOADER_ID_USER_IDS = 1;

    private UserIdsAdapter mUserIdsAdapter;

    private Uri mDataUri;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyFragment newInstance(Uri dataUri) {
        ViewKeyFragment frag = new ViewKeyFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_fragment, getContainer());

        mUserIds = (ListView) view.findViewById(R.id.view_key_user_ids);

        mUserIds.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showUserIdInfo(position);
            }
        });

        mSystemContactLayout = (LinearLayout) view.findViewById(R.id.system_contact_layout);
        mSystemContactName = (TextView) view.findViewById(R.id.system_contact_name);
        mSystemContactPicture = (ImageView) view.findViewById(R.id.system_contact_picture);

        return root;
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
     * Checks if a system contact exists for given masterKeyId, and if it does, sets name, picture
     * and onClickListener for the linked system contact's layout
     * In the case of a secret key, "me" contact details are loaded
     *
     * @param masterKeyId
     */
    private void loadLinkedSystemContact(final long masterKeyId) {
        final Context context = mSystemContactName.getContext();
        final ContentResolver resolver = context.getContentResolver();

        long contactId;
        String contactName = null;

        if (mIsSecret) {//all secret keys are linked to "me" profile in contacts
            contactId = ContactHelper.getMainProfileContactId(resolver);
            List<String> mainProfileNames = ContactHelper.getMainProfileContactName(context);
            if (mainProfileNames != null) contactName = mainProfileNames.get(0);

        } else {
            contactId = ContactHelper.findContactId(resolver, masterKeyId);
            contactName = ContactHelper.getContactName(resolver, contactId);
        }

        if (contactName != null) {//contact name exists for given master key
            mSystemContactName.setText(contactName);

            Bitmap picture;
            if (mIsSecret) {
                picture = ContactHelper.loadMainProfilePhoto(resolver, false);
            } else {
                picture = ContactHelper.loadPhotoByMasterKeyId(resolver, masterKeyId, false);
            }
            if (picture != null) mSystemContactPicture.setImageBitmap(picture);

            final long finalContactId = contactId;
            mSystemContactLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchContactActivity(finalContactId, context);
                }
            });
            mSystemContactLoaded = true;
        }
    }

    /**
     * launches the default android Contacts app to view a contact with the passed
     * contactId (CONTACT_ID column from ContactsContract.RawContact table which is _ID column in
     * ContactsContract.Contact table)
     *
     * @param contactId _ID for row in ContactsContract.Contacts table
     * @param context
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
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }

        loadData(dataUri);
    }


    // These are the rows that we will retrieve.
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
    static final int INDEX_USER_ID = 2;
    static final int INDEX_IS_REVOKED = 3;
    static final int INDEX_IS_EXPIRED = 4;
    static final int INDEX_VERIFIED = 5;
    static final int INDEX_HAS_ANY_SECRET = 6;
    static final int INDEX_FINGERPRINT = 7;
    static final int INDEX_HAS_ENCRYPT = 8;

    private void loadData(Uri dataUri) {
        mDataUri = dataUri;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri);

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        // TODO Is this loader the same as the one in the activity?
        getLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        setContentShown(false);

        switch (id) {
            case LOADER_ID_UNIFIED: {
                Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri, UNIFIED_PROJECTION, null, null, null);
            }
            case LOADER_ID_USER_IDS:
                return UserIdsAdapter.createLoader(getActivity(), mDataUri);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* TODO better error handling? May cause problems when a key is deleted,
         * because the notification triggers faster than the activity closes.
         */
        // Avoid NullPointerExceptions...
        if (data.getCount() == 0) {
            return;
        }
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_UNIFIED: {
                if (data.moveToFirst()) {

                    mIsSecret = data.getInt(INDEX_HAS_ANY_SECRET) != 0;

                    //TODO system to allow immediate refreshing of system contact on verification
                    if (!mSystemContactLoaded) {//ensure we load linked system contact only once
                        long masterKeyId = data.getLong(INDEX_MASTER_KEY_ID);
                        loadLinkedSystemContact(masterKeyId);
                    }
                    // load user ids after we know if it's a secret key
                    mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0, !mIsSecret, null);
                    mUserIds.setAdapter(mUserIdsAdapter);
                    getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);

                    break;
                }
            }

            case LOADER_ID_USER_IDS: {
                mUserIdsAdapter.swapCursor(data);
                break;
            }

        }
        setContentShown(true);
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

}
