/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.ui.widget;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongycastle.openpgp.PGPKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.PGPConversionHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.util.InputData;
import org.thialfihar.android.apg.util.Log;
import org.thialfihar.android.apg.util.PositionAwareInputStream;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * A custom Loader to search for bad adware apps, based on
 * https://github.com/brosmike/AirPush-Detector. Daniel Bjorge licensed it under Apachev2 after
 * asking him by mail.
 */
public class ImportKeysListLoader extends AsyncTaskLoader<List<Map<String, String>>> {
    public static final String MAP_ATTR_USER_ID = "user_id";
    public static final String MAP_ATTR_FINGERPINT = "fingerprint";

    Context mContext;
    List<String> mItems;

    byte[] mKeyringBytes;
    String mImportFilename;

    public ImportKeysListLoader(Context context, byte[] keyringBytes, String importFilename) {
        super(context);
        this.mContext = context;
        this.mKeyringBytes = keyringBytes;
        this.mImportFilename = importFilename;
    }

    @Override
    public List<Map<String, String>> loadInBackground() {
        InputData inputData = null;
        if (mKeyringBytes != null) {
            inputData = new InputData(new ByteArrayInputStream(mKeyringBytes), mKeyringBytes.length);
        } else {
            try {
                inputData = new InputData(new FileInputStream(mImportFilename),
                        mImportFilename.length());
            } catch (FileNotFoundException e) {
                Log.e(Constants.TAG, "Failed to init FileInputStream!", e);
            }
        }

        return generateListOfKeyrings(inputData);
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void deliverResult(List<Map<String, String>> data) {
        super.deliverResult(data);
    }

    /**
     * Similar to PGPMain.importKeyRings
     * 
     * @param keyringBytes
     * @return
     */
    private ArrayList<Map<String, String>> generateListOfKeyrings(InputData inputData) {
        ArrayList<Map<String, String>> output = new ArrayList<Map<String, String>>();

        PositionAwareInputStream progressIn = new PositionAwareInputStream(
                inputData.getInputStream());
        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armour blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        try {
            PGPKeyRing keyring = PGPConversionHelper.decodeKeyRing(bufferedInput);
            while (keyring != null) {
                String userId = PGPHelper.getMainUserId(keyring.getPublicKey());

                String fingerprint = PGPHelper.convertFingerprintToHex(keyring.getPublicKey()
                        .getFingerprint());

                Map<String, String> attrs = new HashMap<String, String>();
                attrs.put(MAP_ATTR_USER_ID, userId);
                attrs.put(MAP_ATTR_FINGERPINT, mContext.getString(R.string.fingerprint) + "\n"
                        + fingerprint);
                output.add(attrs);

                keyring = PGPConversionHelper.decodeKeyRing(bufferedInput);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception", e);
        }

        return output;
    }

}
