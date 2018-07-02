package com.pchmn.materialchips.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

public class ViewUtil {

    private static int windowWidthPortrait = 0;
    private static int windowWidthLandscape = 0;

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int getWindowWidth(Context context) {
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            return getWindowWidthPortrait(context);
        }
        else {
            return getWindowWidthLandscape(context);
        }
    }

    private static int getWindowWidthPortrait(Context context) {
        if(windowWidthPortrait == 0) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            windowWidthPortrait = metrics.widthPixels;
        }

        return windowWidthPortrait;
    }

    private static int getWindowWidthLandscape(Context context) {
        if(windowWidthLandscape == 0) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            windowWidthLandscape = metrics.widthPixels;
        }

        return windowWidthLandscape;
    }

    public static int getNavBarHeight(Context context) {
        int result = 0;
        boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        if(!hasMenuKey && !hasBackKey) {
            //The device has a navigation bar
            Resources resources = context.getResources();

            int orientation = context.getResources().getConfiguration().orientation;
            int resourceId;
            if (isTablet(context)){
                resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
            }  else {
                resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_width", "dimen", "android");
            }

            if (resourceId > 0) {
                return context.getResources().getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }


    private static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
