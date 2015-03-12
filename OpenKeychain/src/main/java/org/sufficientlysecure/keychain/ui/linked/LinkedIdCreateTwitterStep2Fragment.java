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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.pgp.linked.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.RawLinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.resources.TwitterResource;
import org.sufficientlysecure.keychain.ui.util.Notify;

import java.util.List;

public class LinkedIdCreateTwitterStep2Fragment extends LinkedIdCreateFinalFragment {

    public static final String ARG_HANDLE = "handle";

    EditText mEditTweetPreview;

    String mResourceHandle;
    String mResourceString;
    private int mNonce;

    public static LinkedIdCreateTwitterStep2Fragment newInstance
            (String handle) {

        LinkedIdCreateTwitterStep2Fragment frag = new LinkedIdCreateTwitterStep2Fragment();

        int proofNonce = RawLinkedIdentity.generateNonce();

        Bundle args = new Bundle();
        args.putString(ARG_HANDLE, handle);
        args.putInt(ARG_NONCE, proofNonce);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNonce = LinkedIdentity.generateNonce();
        mResourceString =
                TwitterResource.generate(getActivity(), mLinkedIdWizard.mFingerprint, mNonce);

        mResourceHandle = getArguments().getString(ARG_HANDLE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mEditTweetPreview = (EditText) view.findViewById(R.id.linked_create_twitter_preview);
        mEditTweetPreview.setText(mResourceString);

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
        return TwitterResource.searchInTwitterStream(mResourceHandle, mResourceString);
    }

    @Override
    protected View newView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.linked_create_twitter_fragment_step3, container, false);
    }

    private void proofShare() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mResourceString);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void proofSend() {

        Intent tweetIntent = new Intent(Intent.ACTION_SEND);
        tweetIntent.putExtra(Intent.EXTRA_TEXT, mResourceString);
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
