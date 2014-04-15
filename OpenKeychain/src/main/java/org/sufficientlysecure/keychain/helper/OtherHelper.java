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

package org.sufficientlysecure.keychain.helper;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Iterator;
import java.util.Set;

public class OtherHelper {

    /**
     * Logs bundle content to debug for inspecting the content
     *
     * @param bundle
     * @param bundleName
     */
    public static void logDebugBundle(Bundle bundle, String bundleName) {
        if (Constants.DEBUG) {
            if (bundle != null) {
                Set<String> ks = bundle.keySet();
                Iterator<String> iterator = ks.iterator();

                Log.d(Constants.TAG, "Bundle " + bundleName + ":");
                Log.d(Constants.TAG, "------------------------------");
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    Object value = bundle.get(key);

                    if (value != null) {
                        Log.d(Constants.TAG, key + " : " + value.toString());
                    } else {
                        Log.d(Constants.TAG, key + " : null");
                    }
                }
                Log.d(Constants.TAG, "------------------------------");
            } else {
                Log.d(Constants.TAG, "Bundle " + bundleName + ": null");
            }
        }
    }

    public static SpannableStringBuilder strikeOutText(CharSequence text) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        sb.setSpan(new StrikethroughSpan(), 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return sb;
    }

}
