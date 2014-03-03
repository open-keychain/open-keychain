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

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.PositionAwareInputStream;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class ImportKeysListLoader extends AsyncTaskLoader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {

    public static class FileHasNoContent extends Exception {

    }

    public static class NonPGPPart extends Exception {
        private int count;
        public NonPGPPart(int count) {
            this.count = count;
        }
        public int getCount() {
            return count;
        }
    }

    Context mContext;

    InputData mInputData;

    ArrayList<ImportKeysListEntry> data = new ArrayList<ImportKeysListEntry>();
    AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> entryListWrapper;

    public ImportKeysListLoader(Context context, InputData inputData) {
        super(context);
        this.mContext = context;
        this.mInputData = inputData;
    }

    @Override
    public AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> loadInBackground() {

        entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(data, null);

        if (mInputData == null) {
            Log.e(Constants.TAG, "Input data is null!");
            return entryListWrapper;
        }

        generateListOfKeyrings(mInputData);

        return entryListWrapper;
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
    public void deliverResult(AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> data) {
        super.deliverResult(data);
    }

    /**
     * Reads all PGPKeyRing objects from input
     * 
     * @param keyringBytes
     * @return
     */
    private void generateListOfKeyrings(InputData inputData) {

        boolean isEmpty = true;
        int nonPGPcounter = 0;

        PositionAwareInputStream progressIn = new PositionAwareInputStream(
                inputData.getInputStream());

        // need to have access to the bufferedInput, so we can reuse it for the possible
        // PGPObject chunks after the first one, e.g. files with several consecutive ASCII
        // armour blocks
        BufferedInputStream bufferedInput = new BufferedInputStream(progressIn);
        try {

            // read all available blocks... (asc files can contain many blocks with BEGIN END)
            while (bufferedInput.available() > 0) {
                isEmpty = false;
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
                        nonPGPcounter++;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception on parsing key file!", e);
            entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>(data, e);
            nonPGPcounter = 0;
        }

        if(isEmpty) {
            Log.e(Constants.TAG, "File has no content!", new FileHasNoContent());
            entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>
                    (data, new FileHasNoContent());
        }

        if(nonPGPcounter > 0) {
            entryListWrapper = new AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>
                    (data, new NonPGPPart(nonPGPcounter));
        }
    }

    private void addToData(PGPKeyRing keyring) {
        ImportKeysListEntry item = new ImportKeysListEntry(keyring);
        data.add(item);
    }

}
