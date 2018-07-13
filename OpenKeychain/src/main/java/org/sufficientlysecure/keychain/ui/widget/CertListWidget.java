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

package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.Certification.CertDetails;

public class CertListWidget extends ViewAnimator {
    private TextView vCollapsed;
    private ListView vExpanded;
    private View vExpandButton;

    public CertListWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View root = getRootView();
        vCollapsed = root.findViewById(R.id.cert_collapsed_list);
        vExpanded = root.findViewById(R.id.cert_expanded_list);
        vExpandButton = root.findViewById(R.id.cert_expand_button);

        // for now
        vExpandButton.setVisibility(View.GONE);
        vExpandButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleExpanded();
            }
        });

        // vExpanded.setAdapter(null);

    }

    void toggleExpanded() {
        setDisplayedChild(getDisplayedChild() == 1 ? 0 : 1);
    }

    void setExpanded(boolean expanded) {
        setDisplayedChild(expanded ? 1 : 0);
    }

    public void setData(CertDetails certDetails, boolean isSecret) {
        if (certDetails != null) {
            CharSequence relativeTimeStr = DateUtils
                    .getRelativeTimeSpanString(certDetails.creation(), System.currentTimeMillis(), 0, DateUtils.FORMAT_ABBREV_ALL);
            if (isSecret) {
                vCollapsed.setText("You created this identity " + relativeTimeStr + ".");
            } else {
                vCollapsed.setText("You verified and confirmed this identity " + relativeTimeStr + ".");
            }
        } else {
            vCollapsed.setText("This identity is not yet verified or confirmed.");
        }

    }

}
