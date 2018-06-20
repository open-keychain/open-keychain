/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.Nullable;

import timber.log.Timber;


public class ClipboardReflection {

    @Nullable
    public static String getClipboardText(@Nullable Context context) {
        if (context == null) {
            return null;
        }
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return null;
        }

        try {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                Timber.e("No clipboard data!");
                return null;
            }

            ClipData.Item item = clip.getItemAt(0);
            CharSequence seq = item.coerceToText(context);
            if (seq != null) {
                return seq.toString();
            }
            return null;
        } catch (SecurityException e) {
            Timber.e(e, "Not allowed to read clipboard");
            return null;
        }
    }
}
