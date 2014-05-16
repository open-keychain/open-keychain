/*
 * Copyright (C) 2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.util;

import android.content.Context;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import org.sufficientlysecure.keychain.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Highlighter {
    private Context mContext;
    private String mQuery;

    public Highlighter(Context context, String query) {
        mContext = context;
        mQuery = query;
    }

    public Spannable highlight(String text) {
        Spannable highlight = Spannable.Factory.getInstance().newSpannable(text);

        if (mQuery == null) {
            return highlight;
        }

        Pattern pattern = Pattern.compile("(?i)(" + mQuery.trim().replaceAll("\\s+", "|") + ")");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            highlight.setSpan(
                    new ForegroundColorSpan(mContext.getResources().getColor(R.color.emphasis)),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return highlight;
    }
}
