/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.bindings;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BindingAdapter;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.Highlighter;

import java.util.Date;

public class ImportKeysBindings {

    @BindingAdapter({"keyUserId", "keySecret", "keyRevokedOrExpiredOrInsecure", "query"})
    public static void setUserId(TextView textView, CharSequence userId, boolean secret,
                                 boolean revokedOrExpiredOrInsecure, String query) {

        Context context = textView.getContext();
        Resources resources = context.getResources();

        if (userId == null) {
            userId = resources.getString(R.string.user_id_no_name);
        }

        if (secret) {
            userId = resources.getString(R.string.secret_key) + " " + userId;
        } else {
            Highlighter highlighter = ImportKeysBindingsUtils.getHighlighter(context, query);
            userId = highlighter.highlight(userId);
        }
        textView.setText(userId);
        textView.setTextColor(ImportKeysBindingsUtils.getColor(context, revokedOrExpiredOrInsecure));

        if (secret) {
            textView.setTextColor(Color.RED);
        }
    }

    @BindingAdapter({"keyUserEmail", "keyRevokedOrExpiredOrInsecure", "query"})
    public static void setUserEmail(TextView textView, CharSequence userEmail,
                                    boolean revokedOrExpiredOrInsecure, String query) {

        Context context = textView.getContext();

        if (userEmail == null) {
            userEmail = "";
        }

        Highlighter highlighter = ImportKeysBindingsUtils.getHighlighter(context, query);
        textView.setText(highlighter.highlight(userEmail));
        textView.setTextColor(ImportKeysBindingsUtils.getColor(context, revokedOrExpiredOrInsecure));
    }

    @BindingAdapter({"keyCreation", "keyRevokedOrExpiredOrInsecure"})
    public static void setCreation(TextView textView, Date creationDate, boolean revokedOrExpiredOrInsecure) {
        Context context = textView.getContext();

        String text = "";
        if (creationDate != null) {
            text = DateFormat.getDateFormat(context).format(creationDate);
        }

        textView.setText(text);
        textView.setTextColor(ImportKeysBindingsUtils.getColor(context, revokedOrExpiredOrInsecure));
    }

}
