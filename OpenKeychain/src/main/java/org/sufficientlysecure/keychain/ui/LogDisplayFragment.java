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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogEntryParcel;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogLevel;
import org.sufficientlysecure.keychain.operations.results.OperationResult.SubLogEntryParcel;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

import java.io.IOException;
import java.io.OutputStream;


public class LogDisplayFragment extends ListFragment implements OnItemClickListener {

    LogAdapter mAdapter;

    OperationResult mResult;

    public static final String EXTRA_RESULT = "log";
    protected int mTextColor;

    private Uri mLogTempFile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTextColor = FormattingUtils.getColorFromAttr(getActivity(), R.attr.colorText);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        if (intent == null) {
            getActivity().finish();
            return;
        }

        if (savedInstanceState != null) {
            mResult = savedInstanceState.getParcelable(EXTRA_RESULT);
        } else {
            mResult = intent.getParcelableExtra(EXTRA_RESULT);
        }

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // need to parcel this again, logs are only single-instance parcelable
        outState.putParcelable(EXTRA_RESULT, mResult);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.log_display, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_log_display_export_log:
                shareLog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareLog() {

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        String log = mResult.getLog().getPrintableOperationLog(getResources(), 0);

        // if there is no log temp file yet, create one
        if (mLogTempFile == null) {
            mLogTempFile = TemporaryStorageProvider.createFile(getActivity(), "openkeychain_log.txt", "text/plain");
            try {
                OutputStream outputStream = activity.getContentResolver().openOutputStream(mLogTempFile);
                outputStream.write(log.getBytes());
            } catch (IOException e) {
                Notify.create(activity, R.string.error_log_share_internal, Style.ERROR).show();
                return;
            }
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, mLogTempFile);
        intent.setType("text/plain");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);

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
        // Check if convertView.setPadding is redundant
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
                convertView.setPadding((entry.mIndent) * dipFactor, 0, 0, 0);

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
                    ih.mSecondText.setTextColor(subEntry.mType.mLevel == LogLevel.DEBUG ? Color.GRAY : mTextColor);
                    switch (subEntry.mType.mLevel) {
                        case DEBUG: ih.mSecondImg.setBackgroundColor(Color.GRAY); break;
                        case INFO: ih.mSecondImg.setBackgroundColor(mTextColor); break;
                        case WARN: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.android_orange_light)); break;
                        case ERROR: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.android_red_light)); break;
                        case START: ih.mSecondImg.setBackgroundColor(mTextColor); break;
                        case OK: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.android_green_light)); break;
                        case CANCELLED: ih.mSecondImg.setBackgroundColor(getResources().getColor(R.color.android_red_light)); break;
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
            ih.mText.setTextColor(entry.mType.mLevel == LogLevel.DEBUG ? Color.GRAY : mTextColor);
            switch (entry.mType.mLevel) {
                case DEBUG: ih.mImg.setBackgroundColor(Color.GRAY); break;
                case INFO: ih.mImg.setBackgroundColor(mTextColor); break;
                case WARN: ih.mImg.setBackgroundColor(getResources().getColor(R.color.android_orange_light)); break;
                case ERROR: ih.mImg.setBackgroundColor(getResources().getColor(R.color.android_red_light)); break;
                case START: ih.mImg.setBackgroundColor(mTextColor); break;
                case OK: ih.mImg.setBackgroundColor(getResources().getColor(R.color.android_green_light)); break;
                case CANCELLED: ih.mImg.setBackgroundColor(getResources().getColor(R.color.android_red_light)); break;
            }

            return convertView;
        }

    }
}
