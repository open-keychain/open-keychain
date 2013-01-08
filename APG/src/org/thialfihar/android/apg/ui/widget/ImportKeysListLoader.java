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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
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

    ArrayList<Map<String, String>> data = new ArrayList<Map<String, String>>();

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

        generateListOfKeyrings(inputData);

        return data;
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
    private void generateListOfKeyrings(InputData inputData) {
        PositionAwareInputStream progressIn = new PositionAwareInputStream(
                inputData.getInputStream());

        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armour blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        try {

            // read all available blocks... (asc files can contain many blocks with BEGIN END)
            while (bufferedInput.available() > 0) {
                InputStream in = PGPUtil.getDecoderStream(bufferedInput);
                PGPObjectFactory objectFactory = new PGPObjectFactory(in);

                // go through all objects in this block
                Object obj;
                while ((obj = objectFactory.nextObject()) != null) {
                    Log.d(Constants.TAG, "Found class: " + obj.getClass());

                    if (obj instanceof PGPKeyRing) {
                        PGPKeyRing newKeyring = (PGPKeyRing) obj;
                        addToData(newKeyring);
                    } else {
                        Log.e(Constants.TAG, "Object not recognized as PGPKeyRing!");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception on parsing key file!", e);
        }
    }

    private void addToData(PGPKeyRing keyring) {
        String userId = PGPHelper.getMainUserId(keyring.getPublicKey());

        if (keyring instanceof PGPSecretKeyRing) {
            userId = mContext.getString(R.string.secretKeyring) + " " + userId;
        }

        String fingerprint = PGPHelper.convertFingerprintToHex(keyring.getPublicKey()
                .getFingerprint());

        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(MAP_ATTR_USER_ID, userId);
        attrs.put(MAP_ATTR_FINGERPINT, mContext.getString(R.string.fingerprint) + "\n"
                + fingerprint);
        data.add(attrs);
    }

}
