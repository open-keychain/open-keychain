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

package org.sufficientlysecure.keychain.ui.affirmations;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.pgp.affirmation.resources.GenericHttpsResource;
import org.sufficientlysecure.keychain.pgp.affirmation.resources.TwitterResource;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class AffirmationCreateTwitterStep3Fragment extends Fragment {

    public static final String HANDLE = "uri", NONCE = "nonce", TEXT = "text", CUSTOM = "custom";

    AffirmationWizard mAffirmationWizard;

    EditText mEditTweetPreview;
    ImageView mVerifyImage;
    View mVerifyProgress;
    TextView mVerifyStatus;

    String mResourceHandle, mCustom, mFullString;
    String mResourceNonce, mResourceString;

    /**
     * Creates new instance of this fragment
     */
    public static AffirmationCreateTwitterStep3Fragment newInstance
            (String handle, String proofNonce, String proofText, String customText) {

        AffirmationCreateTwitterStep3Fragment frag = new AffirmationCreateTwitterStep3Fragment();

        Bundle args = new Bundle();
        args.putString(HANDLE, handle);
        args.putString(NONCE, proofNonce);
        args.putString(TEXT, proofText);
        args.putString(CUSTOM, customText);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.affirmation_create_twitter_fragment_step3, container, false);

        mResourceHandle = getArguments().getString(HANDLE);
        mResourceNonce = getArguments().getString(NONCE);
        mResourceString = getArguments().getString(TEXT);
        mCustom = getArguments().getString(CUSTOM);

        mFullString = mCustom.isEmpty() ? mResourceString : (mCustom + " " + mResourceString);

        mVerifyImage = (ImageView) view.findViewById(R.id.verify_image);
        mVerifyProgress = view.findViewById(R.id.verify_progress);
        mVerifyStatus = (TextView) view.findViewById(R.id.verify_status);

        mEditTweetPreview = (EditText) view.findViewById(R.id.linked_create_twitter_preview);
        mEditTweetPreview.setText(mFullString);

        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAffirmationWizard.loadFragment(null, null, AffirmationWizard.FRAG_ACTION_TO_LEFT);
            }
        });

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

        view.findViewById(R.id.button_verify).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofVerify();
            }
        });

        setVerifyProgress(false, null);
        mVerifyStatus.setText(R.string.linked_verify_pending);


        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // AffirmationCreateHttpsStep2Fragment frag =
                // AffirmationCreateHttpsStep2Fragment.newInstance();

                // mAffirmationWizard.loadFragment(null, frag, AffirmationWizard.FRAG_ACTION_TO_RIGHT);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAffirmationWizard = (AffirmationWizard) getActivity();
    }

    public void setVerifyProgress(boolean on, Boolean success) {
        mVerifyProgress.setVisibility(on ? View.VISIBLE : View.GONE);
        mVerifyImage.setVisibility(on ?  View.GONE : View.VISIBLE);
        if (success == null) {
            mVerifyStatus.setText(R.string.linked_verifying);
            mVerifyImage.setImageResource(R.drawable.status_signature_unverified_cutout);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                    PorterDuff.Mode.SRC_IN);
        } else if (success) {
            mVerifyStatus.setText(R.string.linked_verify_success);
            mVerifyImage.setImageResource(R.drawable.status_signature_verified_cutout);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.android_green_dark),
                    PorterDuff.Mode.SRC_IN);
        } else {
            mVerifyStatus.setText(R.string.linked_verify_error);
            mVerifyImage.setImageResource(R.drawable.status_signature_unknown_cutout);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.android_red_dark),
                    PorterDuff.Mode.SRC_IN);
        }
    }

    public void proofVerify() {
        setVerifyProgress(true, null);

        /*
        try {
            final TwitterResource resource = TwitterResource.createNew(new URI(mResourceHandle));

            new AsyncTask<Void,Void,LinkedVerifyResult>() {

                @Override
                protected LinkedVerifyResult doInBackground(Void... params) {
                    return resource.verify(mAffirmationWizard.mFingerprint, mResourceNonce);
                }

                @Override
                protected void onPostExecute(LinkedVerifyResult result) {
                    super.onPostExecute(result);
                    if (result.success()) {
                        setVerifyProgress(false, true);
                    } else {
                        setVerifyProgress(false, false);
                        // on error, show error message
                        result.createNotify(getActivity()).show();
                    }
                }
            }.execute();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        */

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
        for(ResolveInfo resolveInfo : resolvedInfoList){
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
