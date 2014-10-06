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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.results.OperationResult;
import org.sufficientlysecure.keychain.service.results.OperationResult.LogEntryParcel;
import org.sufficientlysecure.keychain.service.results.OperationResult.LogLevel;
import org.sufficientlysecure.keychain.service.results.OperationResult.SubLogEntryParcel;

public class LogDisplayFragment extends ListFragment implements OnItemClickListener {

    LogAdapter mAdapter;

    OperationResult mResult;

    public static final String EXTRA_RESULT = "log";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        if (intent == null) {
            getActivity().finish();
            return;
        }

        mResult = intent.getParcelableExtra(EXTRA_RESULT);
        if (mResult == null) {
            getActivity().finish();
            return;
        }

        mAdapter = new LogAdapter(getActivity(), mResult.getLog());
        setListAdapter(mAdapter);

        getListView().setOnItemClickListener(this);

        getListView().setFastScrollEnabled(true);
        getListView().setDividerHeight(0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LogEntryParcel parcel = mAdapter.getItem(position);
        if ( ! (parcel instanceof SubLogEntryParcel)) {
            return;
        }
        Intent intent = new Intent(
                getActivity(), LogDisplayActivity.class);
        intent.putExtra(LogDisplayFragment.EXTRA_RESULT, ((SubLogEntryParcel) parcel).getSubResult());
        startActivity(intent);
    }

    private class LogAdapter extends ArrayAdapter<LogEntryParcel> {

        private LayoutInflater mInflater;
        private int dipFactor;

        public LogAdapter(Context context, OperationResult.OperationLog log) {
            super(context, R.layout.log_display_item, log.toList());
            mInflater = LayoutInflater.from(getContext());
            dipFactor = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 8, getResources().getDisplayMetrics());
        }

        private class ItemHolder {
            final View mSecond;
            final TextView mText, mSecondText;
            final ImageView mImg, mSecondImg, mSub;
            public ItemHolder(TextView text, ImageView image, ImageView sub, View second, TextView secondText, ImageView secondImg) {
                mText = text;
                mImg = image;
                mSub = sub;
                mSecond = second;
                mSecondText = secondText;
                mSecondImg = secondImg;
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
                        (ImageView) convertView.findViewById(R.id.log_img),
                        (ImageView) convertView.findViewById(R.id.log_sub),
                        convertView.findViewById(R.id.log_second),
                        (TextView) convertView.findViewById(R.id.log_second_text),
                        (ImageView) convertView.findViewById(R.id.log_second_img)
                );
                convertView.setTag(ih);
            } else {
                ih = (ItemHolder) convertView.getTag();
            }

            if (entry instanceof SubLogEntryParcel) {
                ih.mSub.setVisibility(View.VISIBLE);
                convertView.setClickable(false);

                OperationResult result = ((SubLogEntryParcel) entry).getSubResult();
                LogEntryParcel subEntry = result.getLog().getLast();
                if (subEntry != null) {
                    ih.mSecond.setVisibility(View.VISIBLE);
                    // special case: first parameter may be a quantity
                    if (subEntry.mParameters != null && subEntry.mParameters.length > 0
                            && subEntry.mParameters[0] instanceof Integer) {
                        ih.mSecondText.setText(getResources().getQuantityString(subEntry.mType.getMsgId(),
                                (Integer) subEntry.mParameters[0],
                                subEntry.mParameters));
                    } else {
                        ih.mSecondText.setText(getResources().getString(subEntry.mType.getMsgId(),
                                subEntry.mParameters));
                    }
                    ih.mSecondText.setTextColor(subEntry.mType.mLevel == LogLevel.DEBUG ? Color.GRAY : Color.BLACK);
                    switch (subEntry.mType.mLevel) {
                        case DEBUG: ih.mSecondImg.setBackgroundColor(Color.GRAY); break;
                        case INFO: ih.mSecondImg.setBackgroundColor(Color.BLACK); break;
                        case WARN: ih.mSecondImg.setBackgroundColor(Color.YELLOW); break;
                        case ERROR: ih.mSecondImg.setBackgroundColor(Color.RED); break;
                        case START: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.emphasis)); break;
                        case OK: ih.mSecondImg.setBackgroundColor(Color.GREEN); break;
                        case CANCELLED: ih.mSecondImg.setBackgroundColor(Color.RED); break;
                    }
                } else {
                    ih.mSecond.setVisibility(View.GONE);
                }

            } else {
                ih.mSub.setVisibility(View.GONE);
                ih.mSecond.setVisibility(View.GONE);
                convertView.setClickable(true);
            }

            // special case: first parameter may be a quantity
            if (entry.mParameters != null && entry.mParameters.length > 0
                    && entry.mParameters[0] instanceof Integer) {
                ih.mText.setText(getResources().getQuantityString(entry.mType.getMsgId(),
                        (Integer) entry.mParameters[0],
                        entry.mParameters));
            } else {
                ih.mText.setText(getResources().getString(entry.mType.getMsgId(),
                        entry.mParameters));
            }
            convertView.setPadding((entry.mIndent) * dipFactor, 0, 0, 0);
            ih.mText.setTextColor(entry.mType.mLevel == LogLevel.DEBUG ? Color.GRAY : Color.BLACK);
            switch (entry.mType.mLevel) {
                case DEBUG: ih.mImg.setBackgroundColor(Color.GRAY); break;
                case INFO: ih.mImg.setBackgroundColor(Color.BLACK); break;
                case WARN: ih.mImg.setBackgroundColor(Color.YELLOW); break;
                case ERROR: ih.mImg.setBackgroundColor(Color.RED); break;
                case START: ih.mImg.setBackgroundColor(getResources().getColor(R.color.emphasis)); break;
                case OK: ih.mImg.setBackgroundColor(Color.GREEN); break;
                case CANCELLED: ih.mImg.setBackgroundColor(Color.RED); break;
            }

            return convertView;
        }

    }
}
