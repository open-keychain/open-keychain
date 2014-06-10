package org.sufficientlysecure.keychain.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogEntryParcel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogLevel;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
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

        public LogAdapter(Context context, ArrayList<LogEntryParcel> log, LogLevel level) {
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
            ImageView img = (ImageView) convertView.findViewById(R.id.log_img);

            text.setText(getResources().getString(entry.mType.getMsgId(), (Object[]) entry.mParameters));
            text.setTextColor(entry.mLevel == LogLevel.DEBUG ? Color.GRAY : Color.BLACK);
            convertView.setPadding((entry.mIndent) * dipFactor, 0, 0, 0);
            switch (entry.mLevel) {
                case DEBUG: img.setBackgroundColor(Color.GRAY); break;
                case INFO: img.setBackgroundColor(Color.BLACK); break;
                case WARN: img.setBackgroundColor(Color.YELLOW); break;
                case ERROR: img.setBackgroundColor(Color.RED); break;
                case START: img.setBackgroundColor(Color.GREEN); break;
                case OK: img.setBackgroundColor(Color.GREEN); break;
            }

            return convertView;
        }

    }
}
