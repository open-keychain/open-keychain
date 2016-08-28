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

    @BindingAdapter({"app:keyUserId", "app:keySecret", "app:keyRevokedOrExpired", "app:query"})
    public static void setUserId(TextView textView, CharSequence userId, boolean secret,
                                 boolean revokedOrExpired, String query) {

        Context context = textView.getContext();
        Resources resources = context.getResources();

        if (userId == null)
            userId = resources.getString(R.string.user_id_no_name);

        if (secret) {
            userId = resources.getString(R.string.secret_key) + " " + userId;
        } else {
            Highlighter highlighter = ImportKeysBindingsUtils.getHighlighter(context, query);
            userId = highlighter.highlight(userId);
        }
        textView.setText(userId);
        textView.setTextColor(ImportKeysBindingsUtils.getColor(context, revokedOrExpired));

        if (secret) {
            textView.setTextColor(Color.RED);
        }
    }

    @BindingAdapter({"app:keyUserEmail", "app:keyRevokedOrExpired", "app:query"})
    public static void setUserEmail(TextView textView, CharSequence userEmail,
                                    boolean revokedOrExpired, String query) {

        Context context = textView.getContext();

        if (userEmail == null)
            userEmail = "";

        Highlighter highlighter = ImportKeysBindingsUtils.getHighlighter(context, query);
        textView.setText(highlighter.highlight(userEmail));
        textView.setTextColor(ImportKeysBindingsUtils.getColor(context, revokedOrExpired));
    }

    @BindingAdapter({"app:keyCreation", "app:keyRevokedOrExpired"})
    public static void setCreation(TextView textView, Date creationDate, boolean revokedOrExpired) {
        Context context = textView.getContext();

        String text = "";
        if (creationDate != null) {
            text = DateFormat.getDateFormat(context).format(creationDate);
        }

        textView.setText(text);
        textView.setTextColor(ImportKeysBindingsUtils.getColor(context, revokedOrExpired));
    }

}
