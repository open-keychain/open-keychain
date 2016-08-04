package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.BindingAdapter;
import android.graphics.Color;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.LruCache;

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
            Highlighter highlighter = getHighlighter(context, query);
            userId = highlighter.highlight(userId);
        }
        textView.setText(userId);
        textView.setTextColor(getColor(context, revokedOrExpired));

        if (secret) {
            textView.setTextColor(Color.RED);
        }
    }

    @BindingAdapter({"app:userEmail", "app:revokedOrExpired", "app:query"})
    public static void setUserEmail(TextView textView, CharSequence userEmail,
                                    boolean revokedOrExpired, String query) {

        Context context = textView.getContext();

        if (userEmail == null)
            userEmail = "";

        Highlighter highlighter = getHighlighter(context, query);
        textView.setText(highlighter.highlight(userEmail));
        textView.setTextColor(getColor(context, revokedOrExpired));
    }

    @BindingAdapter({"app:keyId", "app:revokedOrExpired"})
    public static void setKeyId(TextView textView, String keyId, boolean revokedOrExpired) {
        Context context = textView.getContext();

        if (keyId == null)
            keyId = "";

        textView.setText(KeyFormattingUtils.beautifyKeyIdWithPrefix(keyId));
        textView.setTextColor(getColor(context, revokedOrExpired));
    }

    private static int getColor(Context context, boolean revokedOrExpired) {
        if (revokedOrExpired) {
            return context.getResources().getColor(R.color.key_flag_gray);
        } else {
            return FormattingUtils.getColorFromAttr(context, R.attr.colorText);
        }
    }

    private static LruCache<String, Highlighter> highlighterCache = new LruCache<>(1);

    private static Highlighter getHighlighter(Context context, String query) {
        Highlighter highlighter = highlighterCache.get(query);
        if (highlighter == null) {
            highlighter = new Highlighter(context, query);
            highlighterCache.put(query, highlighter);
        }

        return highlighter;
    }

}
