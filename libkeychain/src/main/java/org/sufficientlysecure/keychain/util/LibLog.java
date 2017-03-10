/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;

import android.os.Bundle;

import org.sufficientlysecure.keychain.LibConstants;

import java.util.Iterator;
import java.util.Set;

/**
 * Wraps Android Logging to enable or disable debug output using Constants
 */
public final class LibLog {

    public static void v(String tag, String msg) {
        if (LibConstants.DEBUG) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (LibConstants.DEBUG) {
            android.util.Log.v(tag, msg, tr);
        }
    }

    public static void d(String tag, String msg) {
        if (LibConstants.DEBUG) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (LibConstants.DEBUG) {
            android.util.Log.d(tag, msg, tr);
        }
    }

    public static void dEscaped(String tag, String msg) {
        if (LibConstants.DEBUG) {
            android.util.Log.d(tag, removeUnicodeAndEscapeChars(msg));
        }
    }

    public static void dEscaped(String tag, String msg, Throwable tr) {
        if (LibConstants.DEBUG) {
            android.util.Log.d(tag, removeUnicodeAndEscapeChars(msg), tr);
        }
    }

    public static void i(String tag, String msg) {
        if (LibConstants.DEBUG) {
            android.util.Log.i(tag, msg);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (LibConstants.DEBUG) {
            android.util.Log.i(tag, msg, tr);
        }
    }

    public static void w(String tag, String msg) {
        android.util.Log.w(tag, msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        android.util.Log.w(tag, msg, tr);
    }

    public static void w(String tag, Throwable tr) {
        android.util.Log.w(tag, tr);
    }

    public static void e(String tag, String msg) {
        android.util.Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        android.util.Log.e(tag, msg, tr);
    }


    /**
     * Logs bundle content to debug for inspecting the content
     *
     * @param bundle
     * @param bundleName
     */
    public static void logDebugBundle(Bundle bundle, String bundleName) {
        if (LibConstants.DEBUG) {
            if (bundle != null) {
                Set<String> ks = bundle.keySet();
                Iterator<String> iterator = ks.iterator();

                LibLog.d(LibConstants.TAG, "Bundle " + bundleName + ":");
                LibLog.d(LibConstants.TAG, "------------------------------");
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = bundle.get(key);

                    if (value != null) {
                        LibLog.d(LibConstants.TAG, key + " : " + value.toString());
                    } else {
                        LibLog.d(LibConstants.TAG, key + " : null");
                    }
                }
                LibLog.d(LibConstants.TAG, "------------------------------");
            } else {
                LibLog.d(LibConstants.TAG, "Bundle " + bundleName + ": null");
            }
        }
    }

    public static String removeUnicodeAndEscapeChars(String input) {
        StringBuilder buffer = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            if ((int) input.charAt(i) > 256) {
                buffer.append("\\u").append(Integer.toHexString((int) input.charAt(i)));
            } else {
                if (input.charAt(i) == '\n') {
                    buffer.append("\\n");
                } else if (input.charAt(i) == '\t') {
                    buffer.append("\\t");
                } else if (input.charAt(i) == '\r') {
                    buffer.append("\\r");
                } else if (input.charAt(i) == '\b') {
                    buffer.append("\\b");
                } else if (input.charAt(i) == '\f') {
                    buffer.append("\\f");
                } else if (input.charAt(i) == '\'') {
                    buffer.append("\\'");
                } else if (input.charAt(i) == '\"') {
                    buffer.append("\\");
                } else if (input.charAt(i) == '\\') {
                    buffer.append("\\\\");
                } else {
                    buffer.append(input.charAt(i));
                }
            }
        }
        return buffer.toString();
    }
}
