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


import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;

public class SecurityKeyShopFragment extends Fragment {

    public static final String webShopURL = "https://shop.cotech.de/";

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.security_key_shop_fragment, container, false);

        WebView webView = view.findViewById(R.id.shop_webView);
        webView.setWebViewClient(new SecurityKeyShopWebViewClient(
                view.findViewById(R.id.shop_progressbar),
                view.findViewById(R.id.shop_progressbar_label)
        ));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(webShopURL);
        return view;
    }

    class SecurityKeyShopWebViewClient extends WebViewClient {
        private ProgressBar progressBar;
        private TextView progressBarLabel;

        SecurityKeyShopWebViewClient(ProgressBar progressBar, TextView progressBarLabel) {
            this.progressBar = progressBar;
            this.progressBarLabel = progressBarLabel;
            progressBar.setVisibility(View.VISIBLE);
            progressBarLabel.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageCommitVisible(WebView view, String url) {
            super.onPageCommitVisible(view, url);
            progressBar.setVisibility(View.GONE);
            progressBarLabel.setVisibility(View.GONE);
        }
    }

}
