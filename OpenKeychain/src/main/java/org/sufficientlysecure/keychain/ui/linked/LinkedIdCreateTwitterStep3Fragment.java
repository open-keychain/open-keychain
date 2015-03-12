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

package org.sufficientlysecure.keychain.ui.linked;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.pgp.linked.resources.TwitterResource;
import org.sufficientlysecure.keychain.ui.util.Notify;

import java.util.List;

public class LinkedIdCreateTwitterStep3Fragment extends LinkedIdCreateFinalFragment {

    public static final String ARG_HANDLE = "uri", ARG_TEXT = "text", ARG_CUSTOM = "custom";

    EditText mEditTweetPreview;

    String mResourceHandle, mCustom, mFullString;
    String mResourceString;
    private int mNonce;

    public static LinkedIdCreateTwitterStep3Fragment newInstance
            (String handle, int proofNonce, String proofText, String customText) {

        LinkedIdCreateTwitterStep3Fragment frag = new LinkedIdCreateTwitterStep3Fragment();

        Bundle args = new Bundle();
        args.putString(ARG_HANDLE, handle);
        args.putInt(ARG_NONCE, proofNonce);
        args.putString(ARG_TEXT, proofText);
        args.putString(ARG_CUSTOM, customText);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mResourceHandle = args.getString(ARG_HANDLE);
        mResourceString = args.getString(ARG_TEXT);
        mCustom = args.getString(ARG_CUSTOM);
        mNonce = args.getInt(ARG_NONCE);

        mFullString = mCustom.isEmpty() ? mResourceString : (mCustom + " " + mResourceString);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mEditTweetPreview = (EditText) view.findViewById(R.id.linked_create_twitter_preview);
        mEditTweetPreview.setText(mFullString);

        view.findViewById(R.id.button_send).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofSend();
            }
        });

        view.findViewById(R.id.button_share).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofShare();
            }
        });

        return view;
    }

    @Override
    LinkedCookieResource getResource() {
        return TwitterResource.searchInTwitterStream(mResourceHandle, mFullString);
    }

    @Override
    protected View newView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.linked_create_twitter_fragment_step3, container, false);
    }

    private void proofShare() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mFullString);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void proofSend() {

        Intent tweetIntent = new Intent(Intent.ACTION_SEND);
        tweetIntent.putExtra(Intent.EXTRA_TEXT, mFullString);
        tweetIntent.setType("text/plain");

        PackageManager packManager = getActivity().getPackageManager();
        List<ResolveInfo> resolvedInfoList = packManager.queryIntentActivities(tweetIntent,
                PackageManager.MATCH_DEFAULT_ONLY);

        boolean resolved = false;
        for(ResolveInfo resolveInfo : resolvedInfoList) {
            if(resolveInfo.activityInfo.packageName.startsWith("com.twitter.android")) {
                tweetIntent.setClassName(
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name );
                resolved = true;
                break;
            }
        }

        if (resolved) {
            startActivity(tweetIntent);
        } else {
            Notify.showNotify(getActivity(),
                    "Twitter app is not installed, please use the send intent!",
                    Notify.Style.ERROR);
        }

    }

}
