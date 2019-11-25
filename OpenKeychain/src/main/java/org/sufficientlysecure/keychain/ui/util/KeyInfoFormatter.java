package org.sufficientlysecure.keychain.ui.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;

import java.util.List;

public class KeyInfoFormatter {

    private static final long JUST_NOW_THRESHOLD = DateUtils.MINUTE_IN_MILLIS * 5;

    private Context context;
    private Highlighter highlighter;
    private UnifiedKeyInfo keyInfo;

    public KeyInfoFormatter(Context context) {
        this.context = context;
        highlighter = new Highlighter(context);
    }

    public void setKeyInfo(UnifiedKeyInfo keyInfo) {
        this.keyInfo = keyInfo;
    }

    public void setHighlightString(String highlight) {
        highlighter.setQuery(highlight);
    }

    public void formatUserId(TextView name, TextView email) {
        if (keyInfo.name() == null && keyInfo.email() == null) {
            String readableKeyId = KeyFormattingUtils.beautifyKeyId(keyInfo.master_key_id());
            name.setText(context.getString(R.string.keylist_item_key_id, readableKeyId));
            email.setVisibility(View.GONE);
        } else if (keyInfo.name() == null) {
            name.setText(highlighter.highlight(keyInfo.email()));
            email.setVisibility(View.GONE);
        } else if (keyInfo.email() == null) {
            name.setText(highlighter.highlight(keyInfo.name()));
            email.setVisibility(View.GONE);
        } else {
            name.setText(highlighter.highlight(keyInfo.name()));
            email.setText(highlighter.highlight(keyInfo.email()));
            email.setVisibility(View.VISIBLE);
        }
    }

    public void formatCreationDate(TextView creationDate) {
        if (keyInfo.has_duplicate() || keyInfo.has_any_secret()) {
            creationDate.setText(getSecretKeyReadableTime(context, keyInfo));
            creationDate.setVisibility(View.VISIBLE);
        } else {
            creationDate.setVisibility(View.GONE);
        }
    }

    public void greyInvalidKeys(List<TextView> textviews) {
        int textColor;

        // Note: order is important!
        if (keyInfo.is_revoked()) {
            textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
        } else if (keyInfo.is_expired()) {
            textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
        } else if (!keyInfo.is_secure()) {
            textColor = ContextCompat.getColor(context, R.color.key_flag_gray);
        } else if (keyInfo.has_any_secret()) {
            textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
        } else {
            textColor = FormattingUtils.getColorFromAttr(context, R.attr.colorText);
        }
        for (TextView textView : textviews) {
            textView.setTextColor(textColor);
        }
    }

    public void formatStatusIcon(ImageView statusIcon) {

        // Note: order is important!
        if (keyInfo.is_revoked()) {
            KeyFormattingUtils.setStatusImage(
                    context,
                    statusIcon,
                    null,
                    KeyFormattingUtils.State.REVOKED,
                    R.color.key_flag_gray
            );

            statusIcon.setVisibility(View.VISIBLE);
        } else if (keyInfo.is_expired()) {
            KeyFormattingUtils.setStatusImage(
                    context,
                    statusIcon,
                    null,
                    KeyFormattingUtils.State.EXPIRED,
                    R.color.key_flag_gray
            );

            statusIcon.setVisibility(View.VISIBLE);
        } else if (!keyInfo.is_secure()) {
            KeyFormattingUtils.setStatusImage(
                    context,
                    statusIcon,
                    null,
                    KeyFormattingUtils.State.INSECURE,
                    R.color.key_flag_gray
            );

            statusIcon.setVisibility(View.VISIBLE);
        } else if (keyInfo.has_any_secret()) {
            statusIcon.setVisibility(View.GONE);
        } else {
            // this is a public key - show if it's verified
            if (keyInfo.is_verified()) {
                KeyFormattingUtils.setStatusImage(
                        context,
                        statusIcon,
                        KeyFormattingUtils.State.VERIFIED
                );

                statusIcon.setVisibility(View.VISIBLE);
            } else {
                statusIcon.setVisibility(View.GONE);
            }
        }
    }

    public void formatTrustIcon(ImageView trustIdIcon) {
        trustIdIcon.setVisibility(View.GONE);
    }

    @NonNull
    private String getSecretKeyReadableTime(Context context, SubKey.UnifiedKeyInfo keyInfo) {
        long creationMillis = keyInfo.creation() * 1000;

        boolean allowRelativeTimestamp = keyInfo.has_duplicate();
        if (allowRelativeTimestamp) {
            long creationAgeMillis = System.currentTimeMillis() - creationMillis;
            if (creationAgeMillis < JUST_NOW_THRESHOLD) {
                return context.getString(R.string.label_key_created_just_now);
            }
        }

        String dateTime = DateUtils.formatDateTime(context,
                creationMillis,
                DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR
                        | DateUtils.FORMAT_ABBREV_MONTH);
        return context.getString(R.string.label_key_created, dateTime);
    }
}
