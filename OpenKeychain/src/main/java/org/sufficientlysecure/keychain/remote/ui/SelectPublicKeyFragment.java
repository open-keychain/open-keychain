/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.remote.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.remote.ui.adapter.SelectEncryptKeyAdapter;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;

public class SelectPublicKeyFragment extends RecyclerFragment<SelectEncryptKeyAdapter>
        implements TextWatcher, LoaderManager.LoaderCallbacks<Cursor> {
    public static final String ARG_PRESELECTED_KEY_IDS = "preselected_key_ids";

    private EditText mSearchView;
    private long mSelectedMasterKeyIds[];
    private String mQuery;

    /**
     * Creates new instance of this fragment
     */
    public static SelectPublicKeyFragment newInstance(long[] preselectedKeyIds) {
        SelectPublicKeyFragment frag = new SelectPublicKeyFragment();
        Bundle args = new Bundle();

        args.putLongArray(ARG_PRESELECTED_KEY_IDS, preselectedKeyIds);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedMasterKeyIds = getArguments().getLongArray(ARG_PRESELECTED_KEY_IDS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = getContext();
        FrameLayout root = new FrameLayout(context);

        LinearLayout progressContainer = new LinearLayout(context);
        progressContainer.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        progressContainer.setOrientation(LinearLayout.VERTICAL);
        progressContainer.setGravity(Gravity.CENTER);
        progressContainer.setVisibility(View.GONE);

        ProgressBar progressBar = new ProgressBar(context, null,
                android.R.attr.progressBarStyleLarge);

        progressContainer.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(progressContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout listContainer = new FrameLayout(context);
        listContainer.setId(INTERNAL_LIST_CONTAINER_ID);

        TextView textView = new TextView(context);
        textView.setId(INTERNAL_EMPTY_VIEW_ID);
        textView.setGravity(Gravity.CENTER);

        listContainer.addView(textView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout innerListContainer = new LinearLayout(context);
        innerListContainer.setOrientation(LinearLayout.VERTICAL);

        mSearchView = new EditText(context);
        mSearchView.setId(android.R.id.input);
        mSearchView.setHint(R.string.menu_search);
        mSearchView.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_search_grey_24dp
                ), null, null, null);

        innerListContainer.addView(mSearchView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        RecyclerView listView = new RecyclerView(context);
        listView.setId(INTERNAL_LIST_VIEW_ID);

        int padding = FormattingUtils.dpToPx(context, 8);
        listView.setPadding(padding, 0, padding, 0);
        listView.setClipToPadding(false);

        innerListContainer.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        listContainer.addView(innerListContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(listContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return root;
    }

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.list_empty));
        mSearchView.addTextChangedListener(this);

        setAdapter(new SelectEncryptKeyAdapter(getContext(), null));
        setLayoutManager(new LinearLayoutManager(getContext()));

        // Start out with a progress indicator.
        hideList(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    public long[] getSelectedMasterKeyIds() {
        return getAdapter() != null ?
                getAdapter().getMasterKeyIds() : new long[0];
    }

    public String[] getSelectedRawUserIds() {
        return getAdapter() != null ?
                getAdapter().getRawUserIds() : new String[0];
    }

    public OpenPgpUtils.UserId[] getSelectedUserIds() {
        return getAdapter() != null ?
                getAdapter().getUserIds() : new OpenPgpUtils.UserId[0];
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();

        // These are the rows that we will retrieve.
        String[] projection = new String[]{
                KeyRings._ID,
                KeyRings.MASTER_KEY_ID,
                KeyRings.USER_ID,
                KeyRings.IS_EXPIRED,
                KeyRings.IS_REVOKED,
                KeyRings.HAS_ENCRYPT,
                KeyRings.VERIFIED,
                KeyRings.HAS_DUPLICATE_USER_ID,
                KeyRings.CREATION,
        };

        String inMasterKeyList = null;
        if (mSelectedMasterKeyIds != null && mSelectedMasterKeyIds.length > 0) {
            inMasterKeyList = Tables.KEYS + "." + KeyRings.MASTER_KEY_ID + " IN (";
            for (int i = 0; i < mSelectedMasterKeyIds.length; ++i) {
                if (i != 0) {
                    inMasterKeyList += ", ";
                }
                inMasterKeyList += DatabaseUtils.sqlEscapeString("" + mSelectedMasterKeyIds[i]);
            }
            inMasterKeyList += ")";
        }

        String orderBy = KeyRings.USER_ID + " ASC";
        if (inMasterKeyList != null) {
            // sort by selected master keys
            orderBy = inMasterKeyList + " DESC, " + orderBy;
        }
        String where = null;
        String whereArgs[] = null;
        if (mQuery != null) {
            String[] words = mQuery.trim().split("\\s+");
            whereArgs = new String[words.length];
            for (int i = 0; i < words.length; ++i) {
                if (where == null) {
                    where = "";
                } else {
                    where += " AND ";
                }
                where += KeyRings.USER_ID + " LIKE ?";
                whereArgs[i] = "%" + words[i] + "%";
            }
        }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, projection, where, whereArgs, orderBy);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        getAdapter().setQuery(mQuery);
        getAdapter().swapCursor(SelectEncryptKeyAdapter.PublicKeyCursor.wrap(data));

        // The list should now be shown.
        if (isResumed()) {
            showList(true);
        } else {
            showList(false);
        }

        // preselect given master keys
        getAdapter().preselectMasterKeyIds(mSelectedMasterKeyIds);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        getAdapter().swapCursor(null);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        mQuery = !TextUtils.isEmpty(editable.toString()) ? editable.toString() : null;
        getLoaderManager().restartLoader(0, null, this);
    }
}
