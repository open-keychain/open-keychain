/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.util;

import java.lang.reflect.Method;

import android.content.Context;
import org.thialfihar.android.apg.util.Log;

public class Compatibility {

    private static final String clipboardLabel = "APG";

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
