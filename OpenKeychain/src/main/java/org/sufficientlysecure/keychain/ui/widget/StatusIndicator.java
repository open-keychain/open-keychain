package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import org.sufficientlysecure.keychain.R;


public class StatusIndicator extends ToolableViewAnimator {

    public StatusIndicator(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.status_indicator, this, true);
    }

    public StatusIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.status_indicator, this, true);
    }

}
