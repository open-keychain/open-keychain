/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.compatibility;

import java.lang.reflect.Method;

import android.content.Context;

import org.sufficientlysecure.keychain.util.Log;

public class ClipboardReflection {

    private static final String clipboardLabel = "Keychain";

    /**
     * Wrapper around ClipboardManager based on Android version using Reflection API
     * 
     * @param context
     * @param text
     */
    public static void copyToClipboard(Context context, String text) {
        Object clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if ("android.text.ClipboardManager".equals(clipboard.getClass().getName())) {
                Method methodSetText = clipboard.getClass()
                        .getMethod("setText", CharSequence.class);
                methodSetText.invoke(clipboard, text);
            } else if ("android.content.ClipboardManager".equals(clipboard.getClass().getName())) {
                Class<?> classClipData = Class.forName("android.content.ClipData");
                Method methodNewPlainText = classClipData.getMethod("newPlainText",
                        CharSequence.class, CharSequence.class);
                Object clip = methodNewPlainText.invoke(null, clipboardLabel, text);
                methodNewPlainText = clipboard.getClass()
                        .getMethod("setPrimaryClip", classClipData);
                methodNewPlainText.invoke(clipboard, clip);
            }
        } catch (Exception e) {
            Log.e("ProjectsException", "There was an error copying the text to the clipboard: "
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
                Method methodGetText = clipboard.getClass().getMethod("getText");
                Object text = methodGetText.invoke(clipboard);

                return (CharSequence) text;
            } else if ("android.content.ClipboardManager".equals(clipboard.getClass().getName())) {
                // ClipData clipData = clipboard.getPrimaryClip();
                Method methodGetPrimaryClip = clipboard.getClass().getMethod("getPrimaryClip");
                Object clipData = methodGetPrimaryClip.invoke(clipboard);

                // ClipData.Item clipDataItem = clipData.getItemAt(0);
                Method methodGetItemAt = clipData.getClass().getMethod("getItemAt", int.class);
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
            Log.e("ProjectsException", "There was an error getting the text from the clipboard: "
                    + e.getMessage());

            return null;
        }
    }
}
