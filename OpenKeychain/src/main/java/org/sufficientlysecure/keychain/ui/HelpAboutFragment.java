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

package org.sufficientlysecure.keychain.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.markdown4j.Markdown4jProcessor;
import org.sufficientlysecure.htmltextview.HtmlResImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.keychain.R;
import timber.log.Timber;

import java.io.IOException;


public class HelpAboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.help_about_fragment, container, false);

        TextView versionText = view.findViewById(R.id.help_about_version);
        versionText.setText(getString(R.string.help_about_version) + " " + getVersion());

        HtmlTextView aboutTextView = view.findViewById(R.id.help_about_text);

        // load markdown from raw resource
        try {
            String html = new Markdown4jProcessor().process(
                    getActivity().getResources().openRawResource(R.raw.help_about));
            aboutTextView.setHtml(html, new HtmlResImageGetter(aboutTextView));
        } catch (IOException e) {
            Timber.e(e, "IOException");
        }

        return view;
    }

    /**
     * Get the current package version.
     *
     * @return The current version.
     */
    private String getVersion() {
        String result = "";
        try {
            PackageManager manager = getActivity().getPackageManager();
            PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);

            result = String.format("%s (%s)", info.versionName, info.versionCode);
        } catch (NameNotFoundException e) {
            Timber.w("Unable to get application version: " + e.getMessage());
            result = "Unable to get application version.";
        }

        return result;
    }

}
