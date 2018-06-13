package org.sufficientlysecure.keychain.util;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;


public class ResourceUtils {
    public static Bitmap getDrawableAsNotificationBitmap(@NonNull Context context, @DrawableRes int iconRes) {
        Drawable iconDrawable = ContextCompat.getDrawable(context, iconRes);
        if (iconDrawable == null) {
            return null;
        }
        Resources resources = context.getResources();
        int largeIconWidth = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        int largeIconHeight = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        Bitmap b = Bitmap.createBitmap(largeIconWidth, largeIconHeight, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        iconDrawable.setBounds(0, 0, largeIconWidth, largeIconHeight);
        iconDrawable.draw(c);
        return b;
    }
}
