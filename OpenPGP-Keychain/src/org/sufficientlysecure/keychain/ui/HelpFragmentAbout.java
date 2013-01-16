/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui;

import java.io.IOException;
import java.io.InputStream;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.JellyBeanSpanFixTextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.helper.OtherHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.R;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class HelpFragmentAbout extends SherlockFragment {

    /**
     * Workaround for Android Bug. See
     * http://stackoverflow.com/questions/8748064/starting-activity-from
     * -fragment-causes-nullpointerexception
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setUserVisibleHint(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.help_fragment_about, container, false);

        // load html from html file from /res/raw
        InputStream inputStreamText = getResources().openRawResource(R.raw.help_about);

        TextView versionText = (TextView) view.findViewById(R.id.help_about_version);
        versionText.setText(getString(R.string.help_about_version) + " " + getVersion());

        JellyBeanSpanFixTextView aboutTextView = (JellyBeanSpanFixTextView) view
                .findViewById(R.id.help_about_text);

        // load html into textview
        HtmlSpanner htmlSpanner = new HtmlSpanner();
        htmlSpanner.setStripExtraWhiteSpace(true);
        try {
            aboutTextView.setText(htmlSpanner.fromHtml(inputStreamText));
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error while reading raw resources as stream", e);
        }

        // make links work
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());

        // no flickering when clicking textview for Android < 4
        aboutTextView.setTextColor(getResources().getColor(android.R.color.black));

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
            Log.w(Constants.TAG, "Unable to get application version: " + e.getMessage());
            result = "Unable to get application version.";
        }

        return result;
    }

}