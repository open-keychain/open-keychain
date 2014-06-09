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
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogEntryParcel;

import java.util.ArrayList;

public class LogDisplayFragment extends ListFragment {

    LogAdapter mAdapter;

    public static final String EXTRA_RESULT = "log";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Intent intent = getActivity().getIntent();
        if (intent.getExtras() == null || !intent.getExtras().containsKey(EXTRA_RESULT)) {
            getActivity().finish();
            return;
        }

        OperationResultParcel result = intent.<OperationResultParcel>getParcelableExtra(EXTRA_RESULT);
        if (result == null) {
            getActivity().finish();
            return;
        }

        mAdapter = new LogAdapter(getActivity(), result.getLog());
        setListAdapter(mAdapter);

    }

    private class LogAdapter extends ArrayAdapter<LogEntryParcel> {

        private LayoutInflater mInflater;
        private int dipFactor;

        public LogAdapter(Context context, ArrayList<LogEntryParcel> log) {
            super(context, R.layout.log_display_item, log);
            mInflater = LayoutInflater.from(getContext());
            dipFactor = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 6, getResources().getDisplayMetrics());

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LogEntryParcel entry = getItem(position);
            TextView text;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.log_display_item, parent, false);
                text = (TextView) convertView.findViewById(R.id.log_text);
                convertView.setTag(text);
            } else {
                text = (TextView) convertView.getTag();
            }

            text.setPadding(entry.mIndent*dipFactor, 0, 0, 0);
            text.setText(getResources().getString(entry.mType.getMsgId(), (Object[]) entry.mParameters));
            switch (entry.mLevel) {
                case DEBUG: text.setTextColor(Color.GRAY); break;
                case INFO: text.setTextColor(Color.BLACK); break;
                case WARN: text.setTextColor(Color.YELLOW); break;
                case ERROR: text.setTextColor(Color.RED); break;
            }

            return convertView;
        }

    }
}
