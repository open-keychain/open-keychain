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

package org.sufficientlysecure.keychain.ui.linked;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.linked.resources.GenericHttpsResource;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

public class LinkedIdCreateHttpsStep2Fragment extends LinkedIdCreateFinalFragment {

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    public static final String ARG_URI = "uri", ARG_TEXT = "text";

    EditText mEditUri;

    URI mResourceUri;
    String mResourceString;

    public static LinkedIdCreateHttpsStep2Fragment newInstance
            (String uri, String proofText) {

        LinkedIdCreateHttpsStep2Fragment frag = new LinkedIdCreateHttpsStep2Fragment();

        Bundle args = new Bundle();
        args.putString(ARG_URI, uri);
        args.putString(ARG_TEXT, proofText);
        frag.setArguments(args);

        return frag;
    }

    @Override
    GenericHttpsResource getResource(OperationLog log) {
        return GenericHttpsResource.createNew(mResourceUri);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mResourceUri = new URI(getArguments().getString(ARG_URI));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            getActivity().finish();
        }

        mResourceString = getArguments().getString(ARG_TEXT);

    }

    protected View newView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.linked_create_https_fragment_step2, container, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (view != null) {

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

            mEditUri = (EditText) view.findViewById(R.id.linked_create_https_uri);
            mEditUri.setText(mResourceUri.toString());
        }

        return view;
    }

    private void proofSend () {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mResourceString);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void proofSave () {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Notify.create(getActivity(), "External storage not available!", Style.ERROR).show();
            return;
        }

        String targetName = "pgpkey.txt";

        // TODO: not supported on Android < 4.4
        FileHelper.saveDocument(this, targetName, "text/plain", REQUEST_CODE_OUTPUT);
    }

    private void saveFile(Uri uri) {
        try {
            PrintWriter out =
                    new PrintWriter(getActivity().getContentResolver().openOutputStream(uri));
            out.print(mResourceString);
            if (out.checkError()) {
                Notify.create(getActivity(), "Error writing file!", Style.ERROR).show();
            }
        } catch (FileNotFoundException e) {
            Notify.create(getActivity(), "File could not be opened for writing!", Style.ERROR).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // For saving a file
            case REQUEST_CODE_OUTPUT:
                if (data == null) {
                    return;
                }
                Uri uri = data.getData();
                saveFile(uri);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
