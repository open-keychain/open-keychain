package org.sufficientlysecure.keychain.ui.bindings;

import android.content.Context;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.util.LruCache;

public class ImportKeysBindingsUtils {

    private static LruCache<String, Highlighter> highlighterCache = new LruCache<>(1);

    public static Highlighter getHighlighter(Context context, String query) {
        Highlighter highlighter = highlighterCache.get(query);
        if (highlighter == null) {
            highlighter = new Highlighter(context, query);
            highlighterCache.put(query, highlighter);
        }

        return highlighter;
    }

    public static int getColor(Context context, boolean revokedOrExpired) {
        if (revokedOrExpired) {
            return context.getResources().getColor(R.color.key_flag_gray);
        } else {
            return FormattingUtils.getColorFromAttr(context, R.attr.colorText);
        }
    }

}
