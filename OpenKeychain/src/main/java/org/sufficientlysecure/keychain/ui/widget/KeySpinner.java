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

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.AppCompatSpinner;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.Log;

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
                    mListener.onKeyChanged(id);
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
            Log.e(Constants.TAG, "KeySpinner must be attached to FragmentActivity, this is " + getContext().getClass());
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
        return getSelectedItemId();
    }

    public void setPreSelectedKeyId(long selectedKeyId) {
        mPreSelectedKeyId = selectedKeyId;
    }

    protected class SelectKeyAdapter extends BaseAdapter implements SpinnerAdapter {
        private CursorAdapter inner;
        private int mIndexUserId;
        private int mIndexDuplicate;
        private int mIndexMasterKeyId;
        private int mIndexCreationDate;

        public SelectKeyAdapter() {
            inner = new CursorAdapter(getContext(), null, 0) {
                @Override
                public View newView(Context context, Cursor cursor, ViewGroup parent) {
                    return View.inflate(getContext(), R.layout.keyspinner_item, null);
                }

                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    TextView vKeyName = (TextView) view.findViewById(R.id.keyspinner_key_name);
                    ImageView vKeyStatus = (ImageView) view.findViewById(R.id.keyspinner_key_status);
                    TextView vKeyEmail = (TextView) view.findViewById(R.id.keyspinner_key_email);
                    TextView vDuplicate = (TextView) view.findViewById(R.id.keyspinner_duplicate);

                    KeyRing.UserId userId = KeyRing.splitUserId(cursor.getString(mIndexUserId));
                    vKeyName.setText(userId.name);
                    vKeyEmail.setText(userId.email);

                    boolean duplicate = cursor.getLong(mIndexDuplicate) > 0;
                    if (duplicate) {
                        String dateTime = DateUtils.formatDateTime(context,
                                cursor.getLong(mIndexCreationDate) * 1000,
                                DateUtils.FORMAT_SHOW_DATE
                                        | DateUtils.FORMAT_SHOW_YEAR
                                        | DateUtils.FORMAT_ABBREV_MONTH);

                        vDuplicate.setText(context.getString(R.string.label_key_created, dateTime));
                        vDuplicate.setVisibility(View.VISIBLE);
                    } else {
                        vDuplicate.setVisibility(View.GONE);
                    }

                    boolean valid = setStatus(getContext(), cursor, vKeyStatus);
                    setItemEnabled(view, valid);
                }

                @Override
                public long getItemId(int position) {
                    try {
                        return ((Cursor) getItem(position)).getLong(mIndexMasterKeyId);
                    } catch (Exception e) {
                        // This can happen on concurrent modification :(
                        return Constants.key.none;
                    }
                }
            };
        }

        private void setItemEnabled(View view, boolean enabled) {
            TextView vKeyName = (TextView) view.findViewById(R.id.keyspinner_key_name);
            ImageView vKeyStatus = (ImageView) view.findViewById(R.id.keyspinner_key_status);
            TextView vKeyEmail = (TextView) view.findViewById(R.id.keyspinner_key_email);
            TextView vKeyDuplicate = (TextView) view.findViewById(R.id.keyspinner_duplicate);

            if (enabled) {
                vKeyName.setTextColor(Color.BLACK);
                vKeyEmail.setTextColor(Color.BLACK);
                vKeyDuplicate.setTextColor(Color.BLACK);
                vKeyStatus.setVisibility(View.GONE);
                view.setClickable(false);
            } else {
                vKeyName.setTextColor(Color.GRAY);
                vKeyEmail.setTextColor(Color.GRAY);
                vKeyDuplicate.setTextColor(Color.GRAY);
                vKeyStatus.setVisibility(View.VISIBLE);
                // this is a HACK. the trick is, if the element itself is clickable, the
                // click is not passed on to the view list
                view.setClickable(true);
            }
        }

        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == null) return inner.swapCursor(null);

            mIndexDuplicate = newCursor.getColumnIndex(KeychainContract.KeyRings.HAS_DUPLICATE_USER_ID);
            mIndexUserId = newCursor.getColumnIndex(KeychainContract.KeyRings.USER_ID);
            mIndexMasterKeyId = newCursor.getColumnIndex(KeychainContract.KeyRings.MASTER_KEY_ID);
            mIndexCreationDate = newCursor.getColumnIndex(KeychainContract.KeyRings.CREATION);

            // pre-select key if mPreSelectedKeyId is given
            if (mPreSelectedKeyId != Constants.key.none && newCursor.moveToFirst()) {
                do {
                    if (newCursor.getLong(mIndexMasterKeyId) == mPreSelectedKeyId) {
                        setSelection(newCursor.getPosition() + 1);
                    }
                } while (newCursor.moveToNext());
            }
            return inner.swapCursor(newCursor);
        }

        @Override
        public int getCount() {
            return inner.getCount() + 1;
        }

        @Override
        public Object getItem(int position) {
            if (position == 0) return null;
            return inner.getItem(position - 1);
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) return Constants.key.none;
            return inner.getItemId(position - 1);
        }

        @SuppressLint("ViewHolder") // inflate call is for the preview only
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            try {
                View v = getDropDownView(position, convertView, parent);
                v.findViewById(R.id.keyspinner_key_email).setVisibility(View.GONE);
                return v;
            } catch (NullPointerException e) {
                // This is for the preview...
                return View.inflate(getContext(), android.R.layout.simple_list_item_1, null);
            }
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view;
            if (position == 0) {
                if (convertView == null) {
                    view = inner.newView(null, null, parent);
                } else {
                    view = convertView;
                }
                TextView vKeyName = (TextView) view.findViewById(R.id.keyspinner_key_name);
                ImageView vKeyStatus = (ImageView) view.findViewById(R.id.keyspinner_key_status);
                TextView vKeyEmail = (TextView) view.findViewById(R.id.keyspinner_key_email);
                TextView vKeyDuplicate = (TextView) view.findViewById(R.id.keyspinner_duplicate);

                vKeyName.setText(R.string.choice_none);
                vKeyEmail.setVisibility(View.GONE);
                vKeyDuplicate.setVisibility(View.GONE);
                vKeyStatus.setVisibility(View.GONE);
                setItemEnabled(view, true);
            } else {
                view = inner.getView(position - 1, convertView, parent);
                TextView vKeyEmail = (TextView) view.findViewById(R.id.keyspinner_key_email);
                vKeyEmail.setVisibility(View.VISIBLE);
            }
            return view;
        }
    }

    boolean setStatus(Context context, Cursor cursor, ImageView statusView) {
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
}
