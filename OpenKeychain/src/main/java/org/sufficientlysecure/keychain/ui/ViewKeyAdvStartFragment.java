/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.markdown4j.Markdown4jProcessor;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;

public class ViewKeyAdvStartFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_adv_start_fragment, container, false);

        HtmlTextView textView = (HtmlTextView)
                view.findViewById(R.id.view_key_adv_start_text);

        // load markdown from raw resource
        try {
            String html = new Markdown4jProcessor().process(
                    getActivity().getResources().openRawResource(R.raw.advanced));
            textView.setHtmlFromString(html, new HtmlTextView.LocalImageGetter());
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException", e);
        }

        return view;
    }

}
