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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.*;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdViewFragment;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdViewFragment.OnIdentityLoadedListener;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.Log;

public class ViewKeyFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_DATA_URI = "uri";
    private static final String ARG_FINGERPRINT = "fingerprint";
    private static final String ARG_IS_SECRET = "is_secret";

    private ListView mUserIds;
    //private ListView mLinkedSystemContact;

    boolean mIsSecret = false;
    private String mName;

    LinearLayout mSystemContactLayout;
    ImageView mSystemContactPicture;
    TextView mSystemContactName;

    private static final int LOADER_ID_USER_IDS = 0;
    private static final int LOADER_ID_LINKED_IDS = 1;

    private UserIdsAdapter mUserIdsAdapter;
    private LinkedIdsAdapter mLinkedIdsAdapter;

    private Uri mDataUri;
    private ListView mLinkedIds;
    private CardView mLinkedIdsCard;
    private byte[] mFingerprint;
    private TextView mLinkedIdsExpander;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyFragment newInstance(Uri dataUri, boolean isSecret, byte[] fingerprint) {
        ViewKeyFragment frag = new ViewKeyFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);
        args.putBoolean(ARG_IS_SECRET, isSecret);
        args.putByteArray(ARG_FINGERPRINT, fingerprint);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        Uri dataUri = args.getParcelable(ARG_DATA_URI);
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }
        boolean isSecret = args.getBoolean(ARG_IS_SECRET);
        byte[] fingerprint = args.getByteArray(ARG_FINGERPRINT);

        loadData(dataUri, isSecret, fingerprint);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_fragment, getContainer());

        mUserIds = (ListView) view.findViewById(R.id.view_key_user_ids);
        mLinkedIdsCard = (CardView) view.findViewById(R.id.card_linked_ids);

        mLinkedIds = (ListView) view.findViewById(R.id.view_key_linked_ids);

        mLinkedIdsExpander = (TextView) view.findViewById(R.id.view_key_linked_ids_expander);

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

        mSystemContactLayout = (LinearLayout) view.findViewById(R.id.system_contact_layout);
        mSystemContactName = (TextView) view.findViewById(R.id.system_contact_name);
        mSystemContactPicture = (ImageView) view.findViewById(R.id.system_contact_picture);

        return root;
    }

    private void showLinkedId(final int position) {
        final LinkedIdViewFragment frag;
        try {
            frag = mLinkedIdsAdapter.getLinkedIdFragment(mDataUri, position, mFingerprint);
        } catch (IOException e) {
            e.printStackTrace();
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
     * Checks if a system contact exists for given masterKeyId, and if it does, sets name, picture
     * and onClickListener for the linked system contact's layout
     *
     * @param name
     * @param masterKeyId
     */
    private void loadLinkedSystemContact(String name, final long masterKeyId) {
        final Context context = mSystemContactName.getContext();
        final ContentResolver resolver = context.getContentResolver();

        final long contactId = ContactHelper.findContactId(resolver, masterKeyId);

        if (contactId != -1) {//contact exists for given master key
            mSystemContactName.setText(name);

            Bitmap picture = ContactHelper.loadPhotoByMasterKeyId(resolver, masterKeyId, true);
            if (picture != null) mSystemContactPicture.setImageBitmap(picture);

            mSystemContactLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchContactActivity(contactId, context);
                }
            });
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

    private void loadData(Uri dataUri, boolean isSecret, byte[] fingerprint) {
        mDataUri = dataUri;
        mIsSecret = isSecret;
        mFingerprint = fingerprint;

        Log.i(Constants.TAG, "mDataUri: " + mDataUri);

        // load user ids after we know if it's a secret key
        mUserIdsAdapter = new UserIdsAdapter(getActivity(), null, 0, !mIsSecret, null);
        mUserIds.setAdapter(mUserIdsAdapter);
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);

        mLinkedIdsAdapter = new LinkedIdsAdapter(getActivity(), null, 0,
                !mIsSecret, mLinkedIdsExpander);
        mLinkedIds.setAdapter(mLinkedIdsAdapter);
        getLoaderManager().initLoader(LOADER_ID_LINKED_IDS, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        setContentShown(false);

        switch (id) {
            case LOADER_ID_USER_IDS:
                return UserIdsAdapter.createLoader(getActivity(), mDataUri);

            case LOADER_ID_LINKED_IDS:
                return LinkedIdsAdapter.createLoader(getActivity(), mDataUri);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS: {
                mUserIdsAdapter.swapCursor(cursor);

                String guessedName = mUserIdsAdapter.getGuessedName();
                loadLinkedSystemContact(guessedName,
                        KeyFormattingUtils.convertFingerprintToKeyId(mFingerprint));
                break;
            }

            case LOADER_ID_LINKED_IDS: {
                mLinkedIdsCard.setVisibility(cursor.getCount() > 0 ? View.VISIBLE : View.GONE);
                mLinkedIdsAdapter.swapCursor(cursor);
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
            case LOADER_ID_LINKED_IDS: {
                mLinkedIdsCard.setVisibility(View.GONE);
                mLinkedIdsAdapter.swapCursor(null);
                break;
            }
        }
    }

}
