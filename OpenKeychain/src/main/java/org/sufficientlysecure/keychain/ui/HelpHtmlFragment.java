/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import org.sufficientlysecure.htmltextview.HtmlTextView;

public class HelpHtmlFragment extends Fragment {
    private Activity mActivity;

    private int mHtmlFile;

    public static final String ARG_HTML_FILE = "htmlFile";

    /**
     * Create a new instance of HelpHtmlFragment, providing "htmlFile" as an argument.
     */
    static HelpHtmlFragment newInstance(int htmlFile) {
        HelpHtmlFragment f = new HelpHtmlFragment();

        // Supply html raw file input as an argument.
        Bundle args = new Bundle();
        args.putInt(ARG_HTML_FILE, htmlFile);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = getActivity();

        mHtmlFile = getArguments().getInt(ARG_HTML_FILE);

        ScrollView scroller = new ScrollView(mActivity);
        HtmlTextView text = new HtmlTextView(mActivity);

        // padding
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mActivity
                .getResources().getDisplayMetrics());
        text.setPadding(padding, padding, padding, 0);

        scroller.addView(text);

        // load html from raw resource (Parsing handled by HtmlTextView library)
        text.setHtmlFromRawResource(getActivity(), mHtmlFile);

        // no flickering when clicking textview for Android < 4
        text.setTextColor(getResources().getColor(android.R.color.black));

        return scroller;
    }
}
