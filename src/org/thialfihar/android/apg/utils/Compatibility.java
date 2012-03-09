package org.thialfihar.android.apg.utils;

import java.lang.reflect.Method;

import android.content.Context;
import android.util.Log;

public class Compatibility {

    private static final String clipboardLabel = "APG";

    /**
     * Wrapper around ClipboardManager based on Android version using Reflection API, from
     * http://www.projectsexception.com/blog/?p=87
     * 
     * @param context
     * @param text
     */
    public static void copyToClipboard(Context context, String text) {
        Object clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if ("android.text.ClipboardManager".equals(clipboard.getClass().getName())) {
                Method method = clipboard.getClass().getMethod("setText", CharSequence.class);
                method.invoke(clipboard, text);
            } else if ("android.content.ClipboardManager".equals(clipboard.getClass().getName())) {
                Class<?> clazz = Class.forName("android.content.ClipData");
                Method method = clazz.getMethod("newPlainText", CharSequence.class,
                        CharSequence.class);
                Object clip = method.invoke(null, clipboardLabel, text);
                method = clipboard.getClass().getMethod("setPrimaryClip", clazz);
                method.invoke(clipboard, clip);
            }
        } catch (Exception e) {
            Log.e("ProjectsException", "There was and error copying the text to the clipboard: "
                    + e.getMessage());
        }
    }

    /**
     * Wrapper around ClipboardManager based on Android version using Reflection API
     * 
     * @param context
     * @param text
     */
    public static CharSequence getClipboardText(Context context) {
        Object clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if ("android.text.ClipboardManager".equals(clipboard.getClass().getName())) {
                // CharSequence text = clipboard.getText();
                Method method = clipboard.getClass().getMethod("getText");
                Object text = method.invoke(clipboard);

                return (CharSequence) text;
            } else if ("android.content.ClipboardManager".equals(clipboard.getClass().getName())) {
                // ClipData clipData = clipboard.getPrimaryClip();
                Method methodGetPrimaryClip = clipboard.getClass().getMethod("getPrimaryClip");
                Object clipData = methodGetPrimaryClip.invoke(clipboard);

                // ClipData.Item clipDataItem = clipData.getItemAt(0);
                Method methodGetItemAt = clipData.getClass().getMethod("getItemAt", Integer.TYPE);
                Object clipDataItem = methodGetItemAt.invoke(clipData, 0);

                // CharSequence text = clipDataItem.coerceToText(context);
                Method methodGetString = clipDataItem.getClass().getMethod("coerceToText",
                        Context.class);
                Object text = methodGetString.invoke(clipDataItem, context);

                return (CharSequence) text;
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.e("ProjectsException", "There was and error getting the text from the clipboard: "
                    + e.getMessage());

            return null;
        }
    }
}
