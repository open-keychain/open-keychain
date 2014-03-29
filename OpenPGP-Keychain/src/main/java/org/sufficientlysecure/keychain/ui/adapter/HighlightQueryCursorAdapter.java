/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import org.sufficientlysecure.keychain.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class HighlightQueryCursorAdapter extends CursorAdapter {

    private String mCurQuery;

    public HighlightQueryCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mCurQuery = null;
    }

    public void setSearchQuery(String searchQuery) {
        mCurQuery = searchQuery;
    }

    public String getSearchQuery() {
        return mCurQuery;
    }

    protected Spannable highlightSearchQuery(String text) {
        Spannable highlight = Spannable.Factory.getInstance().newSpannable(text);

        if (mCurQuery != null) {
            Pattern pattern = Pattern.compile("(?i)" + mCurQuery);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                highlight.setSpan(
                        new ForegroundColorSpan(mContext.getResources().getColor(R.color.emphasis)),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return highlight;
        } else {
            return highlight;
        }
    }
}
