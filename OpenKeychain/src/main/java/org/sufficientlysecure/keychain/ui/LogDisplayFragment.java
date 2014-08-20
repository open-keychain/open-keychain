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

package org.sufficientlysecure.keychain.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogEntryParcel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogLevel;
import org.sufficientlysecure.keychain.util.Log;

import java.util.HashMap;

public class LogDisplayFragment extends ListFragment implements OnTouchListener {

    HashMap<LogLevel,LogAdapter> mAdapters = new HashMap<LogLevel, LogAdapter>();
    LogAdapter mAdapter;
    LogLevel mLevel = LogLevel.DEBUG;

    OperationResultParcel mResult;

    GestureDetector mDetector;

    public static final String EXTRA_RESULT = "log";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        if (intent.getExtras() == null || !intent.getExtras().containsKey(EXTRA_RESULT)) {
            getActivity().finish();
            return;
        }

        mResult = intent.<OperationResultParcel>getParcelableExtra(EXTRA_RESULT);
        if (mResult == null) {
            getActivity().finish();
            return;
        }

        mAdapter = new LogAdapter(getActivity(), mResult.getLog(), LogLevel.DEBUG);
        mAdapters.put(LogLevel.DEBUG, mAdapter);
        setListAdapter(mAdapter);

        mDetector = new GestureDetector(getActivity(), new SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                Log.d(Constants.TAG, "x: " + vx + ", y: " + vy);
                if (vx < -2000) {
                    decreaseLogLevel();
                } else if (vx > 2000) {
                    increaseLogLevel();
                }
                return true;
            }
        });

    }

    public void decreaseLogLevel() {
        switch (mLevel) {
            case DEBUG: mLevel = LogLevel.INFO; break;
            case INFO:  mLevel = LogLevel.WARN; break;
        }
        refreshLevel();
    }

    public void increaseLogLevel() {
        switch (mLevel) {
            case INFO: mLevel = LogLevel.DEBUG; break;
            case WARN: mLevel = LogLevel.INFO; break;
        }
        refreshLevel();
    }

    private void refreshLevel() {
        /* TODO not sure if this is a good idea
        if (!mAdapters.containsKey(mLevel)) {
            mAdapters.put(mLevel, new LogAdapter(getActivity(), mResult.getLog(), mLevel));
        }
        mAdapter = mAdapters.get(mLevel);
        setListAdapter(mAdapter);
        */
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setDividerHeight(0);
        getListView().setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mDetector.onTouchEvent(event);
        return false;
    }

    private class LogAdapter extends ArrayAdapter<LogEntryParcel> {

        private LayoutInflater mInflater;
        private int dipFactor;

        public LogAdapter(Context context, OperationResultParcel.OperationLog log, LogLevel level) {
            super(context, R.layout.log_display_item);
            mInflater = LayoutInflater.from(getContext());
            dipFactor = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 8, getResources().getDisplayMetrics());
            // we can't use addAll for a LogLevel.DEBUG shortcut here, unfortunately :(
            for (LogEntryParcel e : log) {
                if (e.mLevel.ordinal() >= level.ordinal()) {
                    add(e);
                }
            }
            notifyDataSetChanged();
        }

        private class ItemHolder {
            final TextView mText;
            final ImageView mImg;
            public ItemHolder(TextView text, ImageView image) {
                mText = text;
                mImg = image;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LogEntryParcel entry = getItem(position);
            ItemHolder ih;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.log_display_item, parent, false);
                ih = new ItemHolder(
                        (TextView) convertView.findViewById(R.id.log_text),
                        (ImageView) convertView.findViewById(R.id.log_img)
                );
                convertView.setTag(ih);
            } else {
                ih = (ItemHolder) convertView.getTag();
            }

            // special case: first parameter may be a quantity
            if (entry.mParameters != null && entry.mParameters.length > 0
                    && entry.mParameters[0] instanceof Integer) {
                ih.mText.setText(getResources().getQuantityString(entry.mType.getMsgId(),
                        (Integer) entry.mParameters[0],
                        entry.mParameters));
            } else {
                Log.d(Constants.TAG, "entry.mType.getMsgId() "+entry.mType.name());
                ih.mText.setText(getResources().getString(entry.mType.getMsgId(),
                        entry.mParameters));
            }
            ih.mText.setTextColor(entry.mLevel == LogLevel.DEBUG ? Color.GRAY : Color.BLACK);
            convertView.setPadding((entry.mIndent) * dipFactor, 0, 0, 0);
            switch (entry.mLevel) {
                case DEBUG: ih.mImg.setBackgroundColor(Color.GRAY); break;
                case INFO: ih.mImg.setBackgroundColor(Color.BLACK); break;
                case WARN: ih.mImg.setBackgroundColor(Color.YELLOW); break;
                case ERROR: ih.mImg.setBackgroundColor(Color.RED); break;
                case START: ih.mImg.setBackgroundColor(Color.GREEN); break;
                case OK: ih.mImg.setBackgroundColor(Color.GREEN); break;
            }

            return convertView;
        }

    }
}
