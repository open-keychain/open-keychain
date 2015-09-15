/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.operations;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.FieldParser;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.InputDataResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyOperation;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.service.InputDataParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.Log;


/** This operation deals with input data, trying to determine its type as it goes. */
public class InputDataOperation extends BaseOperation<InputDataParcel> {

    final private byte[] buf = new byte[256];

    public InputDataOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public InputDataResult execute(InputDataParcel input,
                                     CryptoInputParcel cryptoInput) {

        final OperationLog log = new OperationLog();

        log.add(LogType.MSG_MIME_PARSING, 0);

        Uri currentUri;

        PgpDecryptVerifyInputParcel decryptInput = input.getDecryptInput();
        if (decryptInput != null) {

            PgpDecryptVerifyOperation op =
                    new PgpDecryptVerifyOperation(mContext, mProviderHelper, mProgressable);

            decryptInput.setInputUri(input.getInputUri());

            currentUri = TemporaryStorageProvider.createFile(mContext);
            decryptInput.setOutputUri(currentUri);

            DecryptVerifyResult result = op.execute(decryptInput, cryptoInput);
            if (result.isPending()) {
                return new InputDataResult(log, result);
            }

        } else {
            currentUri = input.getInputUri();
        }

        // If we aren't supposed to attempt mime decode, we are done here
        if (!input.getMimeDecode()) {

            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(currentUri);
            return new InputDataResult(InputDataResult.RESULT_OK, log, uris);

        }

        try {
            InputStream in = mContext.getContentResolver().openInputStream(currentUri);

            MimeStreamParser parser = new MimeStreamParser((MimeConfig) null);

            final ArrayList<Uri> outputUris = new ArrayList<>();

            parser.setContentDecoding(true);
            parser.setRecurse();
            parser.setContentHandler(new AbstractContentHandler() {
                String mFilename;

                @Override
                public void startHeader() throws MimeException {
                    mFilename = null;
                }

                @Override
                public void field(Field field) throws MimeException {
                    field = DefaultFieldParser.getParser().parse(field, DecodeMonitor.SILENT);
                    if (field instanceof ContentDispositionField) {
                        mFilename = ((ContentDispositionField) field).getFilename();
                    }
                }

                @Override
                public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {

                    // log.add(LogType.MSG_MIME_PART, 0, bd.getMimeType());

                    Uri uri = TemporaryStorageProvider.createFile(mContext, mFilename, bd.getMimeType());
                    OutputStream out = mContext.getContentResolver().openOutputStream(uri, "w");

                    if (out == null) {
                        Log.e(Constants.TAG, "error!");
                        return;
                    }

                    int len;
                    while ( (len = is.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    out.close();
                    outputUris.add(uri);

                }
            });

            parser.parse(in);

            log.add(LogType.MSG_MIME_PARSING_SUCCESS, 1);

            return new InputDataResult(InputDataResult.RESULT_OK, log, outputUris);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new InputDataResult(InputDataResult.RESULT_ERROR, log, null);
        } catch (MimeException e) {
            e.printStackTrace();
            return new InputDataResult(InputDataResult.RESULT_ERROR, log, null);
        } catch (IOException e) {
            e.printStackTrace();
            return new InputDataResult(InputDataResult.RESULT_ERROR, log, null);
        }

    }

}
