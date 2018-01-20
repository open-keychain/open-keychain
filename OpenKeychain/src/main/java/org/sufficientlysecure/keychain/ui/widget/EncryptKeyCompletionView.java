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

package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.tokenautocomplete.TokenCompleteTextView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter.KeyItem;
import timber.log.Timber;


public class EncryptKeyCompletionView extends TokenCompleteTextView<KeyItem>
        implements LoaderCallbacks<Cursor> {

    public static final String ARG_QUERY = "query";

    private KeyAdapter mAdapter;
    private LoaderManager mLoaderManager;

    public EncryptKeyCompletionView(Context context) {
        super(context);
        initView();
    }

    public EncryptKeyCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public EncryptKeyCompletionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        allowDuplicates(false);

        mAdapter = new KeyAdapter(getContext(), null, 0);
        setAdapter(mAdapter);
    }

    @Override
    protected View getViewForObject(KeyItem keyItem) {
        LayoutInflater l = LayoutInflater.from(getContext());
        View view = l.inflate(R.layout.recipient_box_entry, null);
        ((TextView) view.findViewById(android.R.id.text1)).setText(keyItem.getReadableName());

        if (keyItem.mIsRevoked || !keyItem.mHasEncrypt || keyItem.mIsExpired) {
            ((TextView) view.findViewById(android.R.id.text1)).setTextColor(Color.RED);
        }

        return view;
    }

    @Override
    protected KeyItem defaultObject(String completionText) {
        return null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (getContext() instanceof FragmentActivity) {
            mLoaderManager = ((FragmentActivity) getContext()).getSupportLoaderManager();
        } else {
            Timber.e("EncryptKeyCompletionView must be attached to a FragmentActivity, this is " +
                    getContext().getClass());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLoaderManager = null;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // These are the rows that we will retrieve.
        Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();

        String[] projection = KeyAdapter.getProjectionWith(new String[]{
                KeychainContract.KeyRings.HAS_ENCRYPT,
        });

        String where = KeyRings.HAS_ENCRYPT + " NOT NULL AND "
                + KeyRings.IS_EXPIRED + " = 0 AND "
                + Tables.KEYS + "." + KeyRings.IS_REVOKED + " = 0";

        String query = args.getString(ARG_QUERY);
        mAdapter.setSearchQuery(query);

        where += " AND " + KeyRings.USER_ID + " LIKE ?";

        return new CursorLoader(getContext(), baseUri, projection, where,
                new String[]{"%" + query + "%"}, null);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public void showDropDown() {
        if (mAdapter == null || mAdapter.getCursor() == null || mAdapter.getCursor().isClosed()) {
            return;
        }
        super.showDropDown();
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        if (hasFocus) {
            ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    protected void performFiltering(@NonNull CharSequence text, int start, int end, int keyCode) {
//        super.performFiltering(text, start, end, keyCode);
        String query = text.subSequence(start, end).toString();
        if (TextUtils.isEmpty(query) || query.length() < 2) {
            mAdapter.swapCursor(null);
            return;
        }
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        mLoaderManager.restartLoader(0, args, this);
    }

}
