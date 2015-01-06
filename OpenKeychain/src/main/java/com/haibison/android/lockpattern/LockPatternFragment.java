

package com.haibison.android.lockpattern;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.haibison.android.lockpattern.widget.LockPatternUtils;
import com.haibison.android.lockpattern.widget.LockPatternView;


public class LockPatternFragment extends Fragment {
    public static final String NUMBER_OF_MEASUREMENTS = "number_of_measurements";
    public static final String PATTERN_STRING = "pattern_string";

    private String mPatternString;
    private LockPatternView.OnPatternListener mEvents;

    public static LockPatternFragment newInstance(String pattern) {
        LockPatternFragment fragment = new LockPatternFragment();
        Bundle args = new Bundle();
        args.putString(PATTERN_STRING, pattern);
        fragment.setArguments(args);
        return fragment;
    }

    public LockPatternFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mEvents = (LockPatternView.OnPatternListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Get the number of measurements from the bundle, or load the default:
        mPatternString = getArguments().getString(PATTERN_STRING);

        View rootView = inflater.inflate(R.layout.alp_42447968_lock_pattern_activity, container, false);

        final LockPatternView lpv = (LockPatternView) rootView.findViewById(R.id.alp_42447968_view_lock_pattern);
        lpv.setPattern(LockPatternView.DisplayMode.Correct, LockPatternUtils.stringToPattern(mPatternString));

        lpv.setOnPatternListener(mEvents);

        return rootView;
    }
}
