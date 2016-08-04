package org.sufficientlysecure.keychain.ui.bindings;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BindingAdapter;
import android.graphics.Color;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

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

    @BindingAdapter({"app:keyRevoked", "app:keyExpired"})
    public static void setStatus(ImageView imageView, boolean revoked, boolean expired) {
        Context context = imageView.getContext();

        if (revoked) {
            KeyFormattingUtils.setStatusImage(context, imageView, null,
                    KeyFormattingUtils.State.REVOKED, R.color.key_flag_gray);
        } else if (expired) {
            KeyFormattingUtils.setStatusImage(context, imageView, null,
                    KeyFormattingUtils.State.EXPIRED, R.color.key_flag_gray);
        }
    }

}
