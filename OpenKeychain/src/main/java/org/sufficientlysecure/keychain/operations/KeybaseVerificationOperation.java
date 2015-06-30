/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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

import android.content.Context;

import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.prover.Prover;
import de.measite.minidns.Client;
import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.record.Data;
import de.measite.minidns.record.TXT;
import org.json.JSONObject;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.KeybaseVerificationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeybaseVerificationParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class KeybaseVerificationOperation extends BaseOperation<KeybaseVerificationParcel> {

    public KeybaseVerificationOperation(Context context, ProviderHelper providerHelper,
                                        Progressable progressable) {
        super(context, providerHelper, progressable);
    }

    @Override
    public KeybaseVerificationResult execute(KeybaseVerificationParcel keybaseInput,
                                             CryptoInputParcel cryptoInput) {

        String requiredFingerprint = keybaseInput.mRequiredFingerprint;

        OperationResult.OperationLog log = new OperationResult.OperationLog();
        log.add(OperationResult.LogType.MSG_KEYBASE_VERIFICATION, 0, requiredFingerprint);

        try {
            String keybaseProof = keybaseInput.mKeybaseProof;
            Proof proof = new Proof(new JSONObject(keybaseProof));
            mProgressable.setProgress(R.string.keybase_message_fetching_data, 0, 100);

            Prover prover = Prover.findProverFor(proof);

            if (prover == null) {
                log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_NO_PROVER, 1,
                        proof.getPrettyName());
                return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
            }

            if (!prover.fetchProofData()) {
                log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_FETCH_PROOF, 1);
                return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
            }

            if (!prover.checkFingerprint(requiredFingerprint)) {
                log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_FINGERPRINT_MISMATCH, 1);
                return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
            }

            String domain = prover.dnsTxtCheckRequired();
            if (domain != null) {
                DNSMessage dnsQuery = new Client().query(new Question(domain, Record.TYPE.TXT));
                if (dnsQuery == null) {
                    log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_DNS_FAIL, 1);
                    log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_SPECIFIC, 2,
                            getFlattenedProverLog(prover));
                    return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
                }
                Record[] records = dnsQuery.getAnswers();
                List<List<byte[]>> extents = new ArrayList<List<byte[]>>();
                for (Record r : records) {
                    Data d = r.getPayload();
                    if (d instanceof TXT) {
                        extents.add(((TXT) d).getExtents());
                    }
                }
                if (!prover.checkDnsTxt(extents)) {
                    log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_SPECIFIC, 1,
                            getFlattenedProverLog(prover));
                    return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
                }
            }

            byte[] messageBytes = prover.getPgpMessage().getBytes();
            if (prover.rawMessageCheckRequired()) {
                InputStream messageByteStream = PGPUtil.getDecoderStream(new
                        ByteArrayInputStream
                        (messageBytes));
                if (!prover.checkRawMessageBytes(messageByteStream)) {
                    log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_SPECIFIC, 1,
                            getFlattenedProverLog(prover));
                    return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
                }
            }

            PgpDecryptVerify op = new PgpDecryptVerify(mContext, mProviderHelper, mProgressable);

            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel(messageBytes)
                    .setSignedLiteralData(true)
                    .setRequiredSignerFingerprint(requiredFingerprint);

            DecryptVerifyResult decryptVerifyResult = op.execute(input, new CryptoInputParcel());

            if (!decryptVerifyResult.success()) {
                log.add(decryptVerifyResult, 1);
                return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
            }

            if (!prover.validate(new String(decryptVerifyResult.getOutputBytes()))) {
                log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_PAYLOAD_MISMATCH, 1);
                return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
            }

            return new KeybaseVerificationResult(OperationResult.RESULT_OK, log, prover);
        } catch (Exception e) {
            // just adds the passed parameter, in this case e.getMessage()
            log.add(OperationResult.LogType.MSG_KEYBASE_ERROR_SPECIFIC, 1, e.getMessage());
            return new KeybaseVerificationResult(OperationResult.RESULT_ERROR, log);
        }
    }

    private String getFlattenedProverLog(Prover prover) {
        String log = "";
        for (String line : prover.getLog()) {
            log += line + "\n";
        }
        return log;
    }
}
