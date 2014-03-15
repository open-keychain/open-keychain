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

package org.sufficientlysecure.keychain.helper;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static SpannableStringBuilder colorizeFingerprint(String fingerprint) {
        SpannableStringBuilder sb = new SpannableStringBuilder(fingerprint);
        try {
            // for each 4 characters of the fingerprint + 1 space
            for (int i = 0; i < fingerprint.length(); i += 5) {
                int spanEnd = Math.min(i + 4, fingerprint.length());
                String fourChars = fingerprint.substring(i, spanEnd);

                int raw = Integer.parseInt(fourChars, 16);
                byte[] bytes = {(byte) ((raw >> 8) & 0xff - 128), (byte) (raw & 0xff - 128)};
                int[] color = OtherHelper.getRgbForData(bytes);
                int r = color[0];
                int g = color[1];
                int b = color[2];

                // we cannot change black by multiplication, so adjust it to an almost-black grey,
                // which will then be brightened to the minimal brightness level
                if (r == 0 && g == 0 && b == 0) {
                    r = 1;
                    g = 1;
                    b = 1;
                }

                // Convert rgb to brightness
                double brightness = 0.2126 * r + 0.7152 * g + 0.0722 * b;

                // If a color is too dark to be seen on black,
                // then brighten it up to a minimal brightness.
                if (brightness < 80) {
                    double factor = 80.0 / brightness;
                    r = Math.min(255, (int) (r * factor));
                    g = Math.min(255, (int) (g * factor));
                    b = Math.min(255, (int) (b * factor));

                    // If it is too light, then darken it to a respective maximal brightness.
                } else if (brightness > 180) {
                    double factor = 180.0 / brightness;
                    r = (int) (r * factor);
                    g = (int) (g * factor);
                    b = (int) (b * factor);
                }

                // Create a foreground color with the 3 digest integers as RGB
                // and then converting that int to hex to use as a color
                sb.setSpan(new ForegroundColorSpan(Color.rgb(r, g, b)),
                        i, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Colorization failed", e);
            // if anything goes wrong, then just display the fingerprint without colour,
            // instead of partially correct colour or wrong colours
            return new SpannableStringBuilder(fingerprint);
        }

        return sb;
    }

    /**
     * Converts the given bytes to a unique RGB color using SHA1 algorithm
     *
     * @param bytes
     * @return an integer array containing 3 numeric color representations (Red, Green, Black)
     * @throws NoSuchAlgorithmException
     * @throws DigestException
     */
    public static int[] getRgbForData(byte[] bytes) throws NoSuchAlgorithmException, DigestException {
        MessageDigest md = MessageDigest.getInstance("SHA1");

        md.update(bytes);
        byte[] digest = md.digest();

        int[] result = {((int) digest[0] + 256) % 256,
                ((int) digest[1] + 256) % 256,
                ((int) digest[2] + 256) % 256};
        return result;
    }

}
