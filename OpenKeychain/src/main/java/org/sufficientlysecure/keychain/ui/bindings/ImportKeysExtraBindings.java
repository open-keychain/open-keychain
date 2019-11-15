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

package org.sufficientlysecure.keychain.ui.bindings;

import android.content.Context;
import android.content.res.Resources;
import androidx.databinding.BindingAdapter;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class ImportKeysExtraBindings {

    @BindingAdapter({"keyRevoked", "keyExpired", "keySecure"})
    public static void setStatus(ImageView imageView, boolean revoked, boolean expired, boolean secure) {
        Context context = imageView.getContext();

        if (revoked) {
            KeyFormattingUtils.setStatusImage(context, imageView, null,
                    KeyFormattingUtils.State.REVOKED, R.color.key_flag_gray);
        } else if (expired) {
            KeyFormattingUtils.setStatusImage(context, imageView, null,
                    KeyFormattingUtils.State.EXPIRED, R.color.key_flag_gray);
        } else if (!secure) {
            KeyFormattingUtils.setStatusImage(context, imageView, null,
                    KeyFormattingUtils.State.INSECURE, R.color.key_flag_gray);
        }
    }

    @BindingAdapter({"keyId"})
    public static void setKeyId(TextView textView, String keyId) {
        Context context = textView.getContext();
        String text;
        if (keyId != null) {
            text = KeyFormattingUtils.beautifyKeyId(keyId);
        } else {
            Resources resources = context.getResources();
            text = resources.getString(R.string.unknown);
        }
        textView.setText(text);
    }

    @BindingAdapter({"keyUserIds", "query"})
    public static void setUserIds(LinearLayout linearLayout, ArrayList userIds, String query) {

        linearLayout.removeAllViews();

        if (userIds != null) {
            Context context = linearLayout.getContext();
            Highlighter highlighter = ImportKeysBindingsUtils.getHighlighter(context, query);

            ArrayList<Map.Entry<String, HashSet<String>>> uIds = userIds;
            for (Map.Entry<String, HashSet<String>> pair : uIds) {
                String name = pair.getKey();
                HashSet<String> emails = pair.getValue();

                LayoutInflater inflater = LayoutInflater.from(context);

                TextView uidView = (TextView) inflater.inflate(
                        R.layout.import_keys_list_entry_user_id, null);
                uidView.setText(highlighter.highlight(name));
                uidView.setPadding(0, 0, FormattingUtils.dpToPx(context, 8), 0);
                uidView.setTextColor(FormattingUtils.getColorFromAttr(context, R.attr.colorText));
                linearLayout.addView(uidView);

                for (String email : emails) {
                    TextView emailView = (TextView) inflater.inflate(
                            R.layout.import_keys_list_entry_user_id, null);
                    emailView.setPadding(
                            FormattingUtils.dpToPx(context, 16), 0,
                            FormattingUtils.dpToPx(context, 8), 0);
                    emailView.setText(highlighter.highlight(email));
                    emailView.setTextColor(FormattingUtils.getColorFromAttr(context, R.attr.colorText));
                    linearLayout.addView(emailView);
                }
            }
        }
    }

}
