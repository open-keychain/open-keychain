package com.pchmn.materialchips.util;


import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;

import com.pchmn.materialchips.R;

public class ColorUtil {

    public static int lighter(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) / 255 + factor) * 255);
        int green = (int) ((Color.green(color) * (1 - factor) / 255 + factor) * 255);
        int blue = (int) ((Color.blue(color) * (1 - factor) / 255 + factor) * 255);
        return Color.argb(Color.alpha(color), red, green, blue);
    }

    public static int lighter(ColorStateList color, float factor) {
        return lighter(color.getDefaultColor(), factor);
    }

    public static int alpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public static boolean isColorDark(int color){
        double darkness = 1 - (0.2126*Color.red(color) + 0.7152*Color.green(color) + 0.0722*Color.blue(color))/255;
        return darkness >= 0.5;
    }

    public static int getThemeAccentColor (final Context context) {
        final TypedValue value = new TypedValue ();
        context.getTheme ().resolveAttribute (R.attr.colorAccent, value, true);
        return value.data;
    }
}
