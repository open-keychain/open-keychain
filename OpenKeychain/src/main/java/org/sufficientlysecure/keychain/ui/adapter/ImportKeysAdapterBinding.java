package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BindingAdapter;
import android.graphics.Color;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;

public class ImportKeysAdapterBinding {

    @BindingAdapter({"app:userId", "app:secret", "app:revokedOrExpired", "app:query"})
    public static void setUserId(TextView textView, CharSequence userId, boolean secret,
                                 boolean revokedOrExpired, String query) {

        Context context = textView.getContext();
        Resources resources = context.getResources();

        if (userId == null)
            userId = resources.getString(R.string.user_id_no_name);

        if (secret) {
            userId = resources.getString(R.string.secret_key) + " " + userId;
        } else {
            Highlighter highlighter = new Highlighter(context, query);
            userId = highlighter.highlight(userId);
        }
        textView.setText(userId);

        if (revokedOrExpired) {
            textView.setTextColor(resources.getColor(R.color.key_flag_gray));
        } else if (secret) {
            textView.setTextColor(Color.RED);
        }
    }

    @BindingAdapter({"app:userEmail", "app:revokedOrExpired", "app:query"})
    public static void setUserEmail(TextView textView, CharSequence userEmail,
                                    boolean revokedOrExpired, String query) {

        Context context = textView.getContext();

        if (userEmail == null)
            userEmail = "";

        Highlighter highlighter = new Highlighter(context, query);
        textView.setText(highlighter.highlight(userEmail));

        if (revokedOrExpired) {
            Resources resources = context.getResources();
            textView.setTextColor(resources.getColor(R.color.key_flag_gray));
        }
    }

    @BindingAdapter({"app:keyId", "app:revokedOrExpired"})
    public static void setKeyId(TextView textView, String keyId, boolean revokedOrExpired) {
        Context context = textView.getContext();

        if (keyId == null)
            keyId = "";

        if (revokedOrExpired) {
            Resources resources = context.getResources();
            textView.setTextColor(resources.getColor(R.color.key_flag_gray));
        }
        textView.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(keyId));
    }

}
