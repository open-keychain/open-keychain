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
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;
import org.openintents.openpgp.OpenPgpMetadata;
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


/** This operation deals with input data, trying to determine its type as it goes. */
public class InputDataOperation extends BaseOperation<InputDataParcel> {

    final private byte[] buf = new byte[256];

    public InputDataOperation(Context context, ProviderHelper providerHelper, Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @NonNull
    @Override
    public InputDataResult execute(InputDataParcel input, CryptoInputParcel cryptoInput) {

        final OperationLog log = new OperationLog();

        log.add(LogType.MSG_DATA, 0);

        Uri currentInputUri;

        DecryptVerifyResult decryptResult = null;

        PgpDecryptVerifyInputParcel decryptInput = input.getDecryptInput();
        if (decryptInput != null) {

            log.add(LogType.MSG_DATA_DECRYPT, 1);

            PgpDecryptVerifyOperation op =
                    new PgpDecryptVerifyOperation(mContext, mProviderHelper, mProgressable);

            decryptInput.setInputUri(input.getInputUri());

            currentInputUri = TemporaryStorageProvider.createFile(mContext);
            decryptInput.setOutputUri(currentInputUri);

            decryptResult = op.execute(decryptInput, cryptoInput);
            if (decryptResult.isPending()) {
                return new InputDataResult(log, decryptResult);
            }
            log.addByMerge(decryptResult, 2);

        } else {
            currentInputUri = input.getInputUri();
        }

        // If we aren't supposed to attempt mime decode, we are done here
        if (!input.getMimeDecode()) {

            if (decryptInput == null) {
                throw new AssertionError("no decryption or mime decoding, this is probably a bug");
            }

            log.add(LogType.MSG_DATA_SKIP_MIME, 1);

            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(currentInputUri);
            ArrayList<OpenPgpMetadata> metadatas = new ArrayList<>();
            metadatas.add(decryptResult.getDecryptionMetadata());

            log.add(LogType.MSG_DATA_OK, 1);
            return new InputDataResult(InputDataResult.RESULT_OK, log, decryptResult, uris, metadatas);

        }

        log.add(LogType.MSG_DATA_MIME, 1);

        InputStream in;
        try {
            in = mContext.getContentResolver().openInputStream(currentInputUri);
        } catch (FileNotFoundException e) {
            log.add(LogType.MSG_DATA_ERROR_IO, 2);
            return new InputDataResult(InputDataResult.RESULT_ERROR, log);
        }
        MimeStreamParser parser = new MimeStreamParser((MimeConfig) null);

        final ArrayList<Uri> outputUris = new ArrayList<>();
        final ArrayList<OpenPgpMetadata> metadatas = new ArrayList<>();

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

                // we read first, no need to create an output file if nothing was read!
                int len = is.read(buf);
                if (len < 0) {
                    return;
                }

                log.add(LogType.MSG_DATA_MIME_PART, 2);

                log.add(LogType.MSG_DATA_MIME_TYPE, 3, bd.getMimeType());
                if (mFilename != null) {
                    log.add(LogType.MSG_DATA_MIME_FILENAME, 3, mFilename);
                }

                Uri uri = TemporaryStorageProvider.createFile(mContext, mFilename, bd.getMimeType());
                OutputStream out = mContext.getContentResolver().openOutputStream(uri, "w");

                if (out == null) {
                    throw new IOException("Error getting file for writing!");
                }

                int totalLength = 0;
                do {
                    totalLength += len;
                    out.write(buf, 0, len);
                } while ((len = is.read(buf)) > 0);

                log.add(LogType.MSG_DATA_MIME_LENGTH, 3, totalLength);

                String charset = bd.getCharset();
                // the charset defaults to us-ascii, but we want to default to utf-8
                if ("us-ascii".equals(charset)) {
                    charset = "utf-8";
                }

                OpenPgpMetadata metadata = new OpenPgpMetadata(mFilename, bd.getMimeType(), 0L, totalLength, charset);

                out.close();
                outputUris.add(uri);
                metadatas.add(metadata);

            }


        });

        try {

            parser.parse(in);

            // if no mime data parsed, just return the raw data as fallback
            if (outputUris.isEmpty()) {

                log.add(LogType.MSG_DATA_MIME_NONE, 2);

                OpenPgpMetadata metadata;
                if (decryptResult != null) {
                    metadata = decryptResult.getDecryptionMetadata();
                } else {
                    // if we neither decrypted nor mime-decoded, should this be treated as an error?
                    // either way, we know nothing about the data
                    metadata = new OpenPgpMetadata();
                }

                outputUris.add(currentInputUri);
                metadatas.add(metadata);

                log.add(LogType.MSG_DATA_OK, 1);
                return new InputDataResult(InputDataResult.RESULT_OK, log, decryptResult, outputUris, metadatas);
            }

            log.add(LogType.MSG_DATA_MIME_OK, 2);

            log.add(LogType.MSG_DATA_OK, 1);
            return new InputDataResult(InputDataResult.RESULT_OK, log, decryptResult, outputUris, metadatas);

        } catch (IOException e) {
            e.printStackTrace();
            log.add(LogType.MSG_DATA_MIME_ERROR, 2);
            return new InputDataResult(InputDataResult.RESULT_ERROR, log);
        } catch (MimeException e) {
            e.printStackTrace();
            log.add(LogType.MSG_DATA_MIME_ERROR, 2);
            return new InputDataResult(InputDataResult.RESULT_ERROR, log);
        }

    }

}
