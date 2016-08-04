package org.sufficientlysecure.keychain.ui.bindings;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.view.LayoutInflater;
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

    @BindingAdapter({"app:keyId"})
    public static void setKeyId(TextView textView, String keyId) {
        if (keyId == null)
            keyId = "";

        textView.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(keyId));
    }

    @BindingAdapter({"app:keyUserIds", "app:query"})
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
