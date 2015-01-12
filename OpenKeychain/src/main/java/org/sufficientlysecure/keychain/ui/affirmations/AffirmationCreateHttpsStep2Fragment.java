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
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

public class AffirmationCreateHttpsStep2Fragment extends Fragment {

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    public static final String URI = "uri", NONCE = "nonce", TEXT = "text";

    AffirmationWizard mAffirmationWizard;

    EditText mEditUri;
    ImageView mVerifyImage;
    View mVerifyProgress;
    TextView mVerifyStatus;

    String mResourceUri;
    String mResourceNonce, mResourceString;

    /**
     * Creates new instance of this fragment
     */
    public static AffirmationCreateHttpsStep2Fragment newInstance
            (String uri, String proofNonce, String proofText) {

        AffirmationCreateHttpsStep2Fragment frag = new AffirmationCreateHttpsStep2Fragment();

        Bundle args = new Bundle();
        args.putString(URI, uri);
        args.putString(NONCE, proofNonce);
        args.putString(TEXT, proofText);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.affirmation_create_https_fragment_step2, container, false);

        mResourceUri = getArguments().getString(URI);
        mResourceNonce = getArguments().getString(NONCE);
        mResourceString = getArguments().getString(TEXT);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // AffirmationCreateHttpsStep2Fragment frag =
                        // AffirmationCreateHttpsStep2Fragment.newInstance();

                // mAffirmationWizard.loadFragment(null, frag, AffirmationWizard.FRAG_ACTION_TO_RIGHT);
            }
        });

        mVerifyImage = (ImageView) view.findViewById(R.id.verify_image);
        mVerifyProgress = view.findViewById(R.id.verify_progress);
        mVerifyStatus = (TextView) view.findViewById(R.id.verify_status);

        view.findViewById(R.id.button_send).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofSend();
            }
        });

        view.findViewById(R.id.button_save).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofSave();
            }
        });

        view.findViewById(R.id.button_verify).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofVerify();
            }
        });

        mEditUri = (EditText) view.findViewById(R.id.affirmation_create_https_uri);
        mEditUri.setText(mResourceUri);

        setVerifyProgress(false, null);
        mVerifyStatus.setText(R.string.linked_verify_pending);

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

        try {
            final GenericHttpsResource resource = GenericHttpsResource.createNew(new URI(mResourceUri));

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

    }

    private void proofSend() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mResourceString);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void proofSave () {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Notify.showNotify(getActivity(), "External storage not available!", Style.ERROR);
            return;
        }

        String targetName = "pgpkey.txt";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            File targetFile = new File(Constants.Path.APP_DIR, targetName);
            FileHelper.saveFile(this, getString(R.string.title_decrypt_to_file),
                    getString(R.string.specify_file_to_decrypt_to), targetFile, REQUEST_CODE_OUTPUT);
        } else {
            FileHelper.saveDocument(this, "text/plain", targetName, REQUEST_CODE_OUTPUT);
        }
    }

    private void saveFile(Uri uri) {
        try {
            PrintWriter out =
                    new PrintWriter(getActivity().getContentResolver().openOutputStream(uri));
            out.print(mResourceString);
            if (out.checkError()) {
                Notify.showNotify(getActivity(), "Error writing file!", Style.ERROR);
            }
        } catch (FileNotFoundException e) {
            Notify.showNotify(getActivity(), "File could not be opened for writing!", Style.ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OUTPUT:
                if (data == null) {
                    return;
                }
                Uri uri = data.getData();
                saveFile(uri);
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
