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


import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;

public class KeySpinner extends AppCompatSpinner {
    public static final String ARG_SUPER_STATE = "super_state";
    public static final String ARG_KEY_ID = "key_id";

    public interface OnKeyChangedListener {
        void onKeyChanged(long masterKeyId);
    }

    protected Long preSelectedKeyId;
    protected KeyChoiceSpinnerAdapter spinnerAdapter;
    protected OnKeyChangedListener mListener;

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
        spinnerAdapter = new KeyChoiceSpinnerAdapter(getContext());

        setAdapter(spinnerAdapter);
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

    public void setShowNone(@StringRes Integer noneStringRes) {
        spinnerAdapter.setNoneItemString(noneStringRes);
    }

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        throw new UnsupportedOperationException();
    }

    public void setOnKeyChangedListener(OnKeyChangedListener listener) {
        mListener = listener;
    }

    public void setData(List<UnifiedKeyInfo> keyInfos) {
        spinnerAdapter.setData(keyInfos);
        maybeSelectPreselection(keyInfos);
    }

    private void maybeSelectPreselection(List<UnifiedKeyInfo> keyInfos) {
        if (spinnerAdapter.hasNoneItem() && keyInfos.size() == 1) {
            setSelection(1);
            return;
        }
        if (preSelectedKeyId == null) {
            return;
        }
        for (UnifiedKeyInfo keyInfo : keyInfos) {
            if (keyInfo.master_key_id() == preSelectedKeyId) {
                int position = keyInfos.indexOf(keyInfo);
                if (spinnerAdapter.hasNoneItem()) {
                    position += 1;
                }
                setSelection(position);
            }
        }
    }

    public boolean isSingleEntry() {
        return spinnerAdapter.isSingleEntry();
    }

    public long getSelectedKeyId() {
        Object item = getSelectedItem();
        return getSelectedKeyId(item);
    }

    public long getSelectedKeyId(Object item) {
        if (item instanceof UnifiedKeyInfo) {
            return ((UnifiedKeyInfo) item).master_key_id();
        }
        return Constants.key.none;
    }

    public void setPreSelectedKeyId(long selectedKeyId) {
        preSelectedKeyId = selectedKeyId;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;

        preSelectedKeyId = bundle.getLong(ARG_KEY_ID);

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
