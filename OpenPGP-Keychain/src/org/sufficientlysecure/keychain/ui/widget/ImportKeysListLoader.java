/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.widget;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.helper.PgpHelper;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PositionAwareInputStream;
import org.sufficientlysecure.keychain.R;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

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
        String userId = PgpHelper.getMainUserId(keyring.getPublicKey());

        if (keyring instanceof PGPSecretKeyRing) {
            userId = mContext.getString(R.string.secretKeyring) + " " + userId;
        }

        String fingerprint = PgpHelper.convertFingerprintToHex(keyring.getPublicKey()
                .getFingerprint());

        Map<String, String> attrs = new HashMap<String, String>();
        attrs.put(MAP_ATTR_USER_ID, userId);
        attrs.put(MAP_ATTR_FINGERPINT, mContext.getString(R.string.fingerprint) + "\n"
                + fingerprint);
        data.add(attrs);
    }

}
