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

package org.sufficientlysecure.keychain.ui.adapter;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PositionAwareInputStream;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class ImportKeysListLoader extends AsyncTaskLoader<List<ImportKeysListEntry>> {
    Context mContext;

    InputData mInputData;

    ArrayList<ImportKeysListEntry> data = new ArrayList<ImportKeysListEntry>();

    public ImportKeysListLoader(Context context, InputData inputData) {
        super(context);
        this.mContext = context;
        this.mInputData = inputData;
    }

    @Override
    public List<ImportKeysListEntry> loadInBackground() {
        if (mInputData == null) {
            Log.e(Constants.TAG, "Input data is null!");
            return data;
        }

        generateListOfKeyrings(mInputData);

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
    public void deliverResult(List<ImportKeysListEntry> data) {
        super.deliverResult(data);
    }

    /**
     * Reads all PGPKeyRing objects from input
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
        ImportKeysListEntry item = new ImportKeysListEntry(keyring);
        data.add(item);
    }

}
