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
            Log.e("ProjectsException", "There was and error getting the text from the clipboard: "
                    + e.getMessage());

            return null;
        }
    }
}
