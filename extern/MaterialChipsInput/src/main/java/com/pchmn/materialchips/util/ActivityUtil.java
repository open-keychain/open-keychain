package com.pchmn.materialchips.util;


import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

public class ActivityUtil {

    public static Activity scanForActivity(Context context) {
        if (context == null)
            return null;
        else if (context instanceof Activity)
            return (Activity)context;
        else if (context instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper)context).getBaseContext());

        return null;
    }
}
