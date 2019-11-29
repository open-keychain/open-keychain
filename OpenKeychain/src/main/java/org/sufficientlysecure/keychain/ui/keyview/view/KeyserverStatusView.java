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

package org.sufficientlysecure.keychain.ui.keyview.view;


import java.util.Date;

import android.content.Context;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;


public class KeyserverStatusView extends FrameLayout {
    private final View vLayout;
    private final TextView vTitle;
    private final TextView vSubtitle;
    private final ImageView vIcon;

    public KeyserverStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.key_keyserver_status_layout, this, true);

        vLayout = view.findViewById(R.id.keyserver_status_layout);

        vTitle = view.findViewById(R.id.keyserver_status_title);
        vSubtitle = view.findViewById(R.id.keyserver_status_subtitle);
        vIcon = view.findViewById(R.id.keyserver_icon);
//        vExpander = (ImageView) view.findViewById(R.id.key_health_expander);
    }

    private enum KeyserverDisplayStatus {
        PUBLISHED (R.string.keyserver_title_published, R.drawable.ic_cloud_black_24dp, R.color.key_flag_gray),
        NOT_PUBLISHED (R.string.keyserver_title_not_published, R.drawable.ic_cloud_off_24dp, R.color.key_flag_gray),
        UNKNOWN (R.string.keyserver_title_unknown, R.drawable.ic_cloud_unknown_24dp, R.color.key_flag_gray);

        @StringRes
        private final int title;
        @DrawableRes
        private final int icon;
        @ColorRes
        private final int iconColor;

        KeyserverDisplayStatus(@StringRes int title, @DrawableRes int icon, @ColorRes int iconColor) {
            this.title = title;
            this.icon = icon;
            this.iconColor = iconColor;
        }
    }

    public void setDisplayStatusPublished() {
        setDisplayStatus(KeyserverDisplayStatus.PUBLISHED);
    }

    public void setDisplayStatusNotPublished() {
        setDisplayStatus(KeyserverDisplayStatus.NOT_PUBLISHED);
    }

    public void setDisplayStatusUnknown() {
        setDisplayStatus(KeyserverDisplayStatus.UNKNOWN);
        vSubtitle.setText(R.string.keyserver_last_updated_never);
    }

    public void setLastUpdated(Date lastUpdated) {
        String lastUpdatedText = DateFormat.getMediumDateFormat(getContext()).format(lastUpdated);
        vSubtitle.setText(getResources().getString(R.string.keyserver_last_updated, lastUpdatedText));
    }

    private void setDisplayStatus(KeyserverDisplayStatus displayStatus) {
        vTitle.setText(displayStatus.title);
        vIcon.setImageResource(displayStatus.icon);
        vIcon.setColorFilter(ContextCompat.getColor(getContext(), displayStatus.iconColor));

        setVisibility(View.VISIBLE);
    }
}
