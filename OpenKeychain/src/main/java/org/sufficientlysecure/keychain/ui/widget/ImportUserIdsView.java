package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class ImportUserIdsView extends LinearLayout {

    public ImportUserIdsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(VERTICAL);
    }

    public void setEntry(ImportKeysListEntry entry) {
        removeAllViews();

        Context context = getContext();
        Highlighter highlighter = new Highlighter(context, entry.getQuery());

        // we want conventional gpg UserIDs first, then Keybase ”proofs”
        ArrayList<Map.Entry<String, HashSet<String>>> sortedIds = entry.getSortedUserIds();
        for (Map.Entry<String, HashSet<String>> pair : sortedIds) {
            String cUserId = pair.getKey();
            HashSet<String> cEmails = pair.getValue();

            LayoutInflater inflater = LayoutInflater.from(context);

            TextView uidView = (TextView) inflater.inflate(
                    R.layout.import_keys_list_entry_user_id, null);
            uidView.setText(highlighter.highlight(cUserId));
            uidView.setPadding(0, 0, FormattingUtils.dpToPx(context, 8), 0);

            if (entry.isRevokedOrExpired()) {
                uidView.setTextColor(context.getResources().getColor(R.color.key_flag_gray));
            } else {
                uidView.setTextColor(FormattingUtils.getColorFromAttr(context, R.attr.colorText));
            }
            addView(uidView);

            for (String email : cEmails) {
                TextView emailView = (TextView) inflater.inflate(
                        R.layout.import_keys_list_entry_user_id, null);
                emailView.setPadding(
                        FormattingUtils.dpToPx(context, 16), 0,
                        FormattingUtils.dpToPx(context, 8), 0);
                emailView.setText(highlighter.highlight(email));

                if (entry.isRevokedOrExpired()) {
                    emailView.setTextColor(context.getResources().getColor(R.color.key_flag_gray));
                } else {
                    emailView.setTextColor(FormattingUtils.getColorFromAttr(context, R.attr.colorText));
                }
                addView(emailView);
            }
        }
    }
}
