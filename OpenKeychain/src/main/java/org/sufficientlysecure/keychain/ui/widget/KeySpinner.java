/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter;
import org.sufficientlysecure.keychain.ui.adapter.KeyAdapter.KeyItem;


/**
 * Use AppCompatSpinner from AppCompat lib instead of Spinner. Fixes white dropdown icon.
 * Related: http://stackoverflow.com/a/27713090
 */
public abstract class KeySpinner extends AppCompatSpinner implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String ARG_SUPER_STATE = "super_state";
    public static final String ARG_KEY_ID = "key_id";

    public interface OnKeyChangedListener {
        void onKeyChanged(long masterKeyId);
    }

    protected long mPreSelectedKeyId = Constants.key.none;
    protected SelectKeyAdapter mAdapter = new SelectKeyAdapter();
    protected OnKeyChangedListener mListener;

    // this shall note collide with other loaders inside the activity
    protected int LOADER_ID = 2343;

    public KeySpinner(Context context) {
        super(context);
        initView();
    }

    public KeySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public KeySpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        setAdapter(mAdapter);
        super.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    long keyId = getSelectedKeyId(getItemAtPosition(position));
                    mListener.onKeyChanged(keyId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (mListener != null) {
                    mListener.onKeyChanged(Constants.key.none);
                }
            }
        });
    }

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        throw new UnsupportedOperationException();
    }

    public void setOnKeyChangedListener(OnKeyChangedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        reload();
    }

    public void reload() {
        if (getContext() instanceof FragmentActivity) {
            ((FragmentActivity) getContext()).getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        } else {
            throw new AssertionError("KeySpinner must be attached to FragmentActivity, this is "
                    + getContext().getClass());
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == LOADER_ID) {
            mAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == LOADER_ID) {
            mAdapter.swapCursor(null);
        }
    }

    public long getSelectedKeyId() {
        Object item = getSelectedItem();
        return getSelectedKeyId(item);
    }

    public long getSelectedKeyId(Object item) {
        if (item instanceof KeyItem) {
            return ((KeyItem) item).mKeyId;
        }
        return Constants.key.none;
    }

    public void setPreSelectedKeyId(long selectedKeyId) {
        mPreSelectedKeyId = selectedKeyId;
    }

    protected class SelectKeyAdapter extends BaseAdapter implements SpinnerAdapter {
        private KeyAdapter inner;
        private int mIndexMasterKeyId;

        public SelectKeyAdapter() {
            inner = new KeyAdapter(getContext(), null, 0) {

                @Override
                public boolean isEnabled(Cursor cursor) {
                    return KeySpinner.this.isItemEnabled(cursor);
                }

            };
        }

        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == null) return inner.swapCursor(null);

            mIndexMasterKeyId = newCursor.getColumnIndex(KeychainContract.KeyRings.MASTER_KEY_ID);

            Cursor oldCursor = inner.swapCursor(newCursor);

            // pre-select key if mPreSelectedKeyId is given
            if (mPreSelectedKeyId != Constants.key.none && newCursor.moveToFirst()) {
                do {
                    if (newCursor.getLong(mIndexMasterKeyId) == mPreSelectedKeyId) {
                        setSelection(newCursor.getPosition() +1);
                    }
                } while (newCursor.moveToNext());
            }
            return oldCursor;
        }

        @Override
        public int getCount() {
            return inner.getCount() +1;
        }

        @Override
        public KeyItem getItem(int position) {
            if (position == 0) {
                return null;
            }
            return inner.getItem(position -1);
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) {
                return Constants.key.none;
            }
            return inner.getItemId(position -1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // Unfortunately, SpinnerAdapter does not support multiple view
            // types. For this reason, we throw away convertViews of a bad
            // type.  This is sort of a hack, but since the number of elements
            // we deal with in KeySpinners is usually very small (number of
            // secret keys), this is the easiest solution. (I'm sorry.)
            if (convertView != null) {
                // This assumes that the inner view has non-null tags on its views!
                boolean isWrongType = (convertView.getTag() == null) != (position == 0);
                if (isWrongType) {
                    convertView = null;
                }
            }

            if (position > 0) {
                return inner.getView(position -1, convertView, parent);
            }

            View view = convertView != null ? convertView :
                    LayoutInflater.from(getContext()).inflate(
                            R.layout.keyspinner_item_none, parent, false);
            ((TextView) view.findViewById(R.id.keyspinner_key_name)).setText(getNoneString());
            return view;
        }

    }

    boolean isItemEnabled(Cursor cursor) {
        return true;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;

        mPreSelectedKeyId = bundle.getLong(ARG_KEY_ID);

        // restore super state
        super.onRestoreInstanceState(bundle.getParcelable(ARG_SUPER_STATE));

    }

    @NonNull
    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();

        // save super state
        bundle.putParcelable(ARG_SUPER_STATE, super.onSaveInstanceState());

        bundle.putLong(ARG_KEY_ID, getSelectedKeyId());
        return bundle;
    }

    public @StringRes int getNoneString() {
        return R.string.cert_none;
    }

}
