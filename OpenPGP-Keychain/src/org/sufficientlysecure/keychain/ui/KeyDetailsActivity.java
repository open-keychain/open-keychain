/*
 * Copyright (C) 2013 Bahtiar 'kalkin' Gadimov
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class KeyDetailsActivity extends SherlockActivity {
    private Uri mDataUri;

    private PGPPublicKey mPublicKey;

    private TextView mAlgorithm;
    private TextView mFingerint;
    private TextView mExpiry;
    private TextView mCreation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        setContentView(R.layout.key_view);

        mFingerint = (TextView) this.findViewById(R.id.fingerprint);
        mExpiry = (TextView) this.findViewById(R.id.expiry);
        mCreation = (TextView) this.findViewById(R.id.creation);
        mAlgorithm = (TextView) this.findViewById(R.id.algorithm);

        Intent intent = getIntent();
        mDataUri = intent.getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of app!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + mDataUri);
            loadData(mDataUri);
        }
    }

    private void loadData(Uri dataUri) {
        PGPPublicKeyRing ring = (PGPPublicKeyRing) ProviderHelper.getPGPKeyRing(this, dataUri);
        mPublicKey = ring.getPublicKey();

        mFingerint.setText(PgpKeyHelper.shortifyFingerprint(PgpKeyHelper
                .convertFingerprintToHex(mPublicKey.getFingerprint())));
        String[] mainUserId = splitUserId("");

        Date expiryDate = PgpKeyHelper.getExpiryDate(mPublicKey);
        if (expiryDate == null) {
            mExpiry.setText("");
        } else {
            mExpiry.setText(DateFormat.getDateFormat(getApplicationContext()).format(expiryDate));
        }

        mCreation.setText(DateFormat.getDateFormat(getApplicationContext()).format(
                PgpKeyHelper.getCreationDate(mPublicKey)));
        mAlgorithm.setText(PgpKeyHelper.getAlgorithmInfo(mPublicKey));
    }

    private String[] splitUserId(String userId) {

        String[] result = new String[] { "", "", "" };
        Log.v("UserID", userId);

        Pattern withComment = Pattern.compile("^(.*) [(](.*)[)] <(.*)>$");
        Matcher matcher = withComment.matcher(userId);
        if (matcher.matches()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(2);
            result[2] = matcher.group(3);
            return result;
        }

        Pattern withoutComment = Pattern.compile("^(.*) <(.*)>$");
        matcher = withoutComment.matcher(userId);
        if (matcher.matches()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(2);
            return result;
        }
        return result;
    }
}
