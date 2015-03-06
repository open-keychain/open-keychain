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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.pgp.linked.resources.DnsResource;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class LinkedIdCreateDnsStep2Fragment extends LinkedIdCreateFinalFragment {

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    public static final String DOMAIN = "domain", TEXT = "text";

    TextView mTextView;

    String mResourceDomain;
    String mResourceString;

    public static LinkedIdCreateDnsStep2Fragment newInstance
            (String uri, int proofNonce, String proofText) {

        LinkedIdCreateDnsStep2Fragment frag = new LinkedIdCreateDnsStep2Fragment();

        Bundle args = new Bundle();
        args.putInt(ARG_NONCE, proofNonce);
        args.putString(DOMAIN, uri);
        args.putString(TEXT, proofText);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResourceDomain = getArguments().getString(DOMAIN);
        mResourceString = getArguments().getString(TEXT);

    }

    @Override
    protected View newView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.linked_create_dns_fragment_step2, container, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

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

        mTextView = (TextView) view.findViewById(R.id.linked_create_dns_text);
        mTextView.setText(mResourceString);

        return view;
    }

    @Override
    LinkedCookieResource getResource() {
        return DnsResource.createNew(mResourceDomain);
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
