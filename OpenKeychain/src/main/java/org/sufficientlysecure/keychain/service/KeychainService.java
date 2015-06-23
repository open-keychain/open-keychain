/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.prover.Prover;

import org.json.JSONObject;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserver;
import org.sufficientlysecure.keychain.keyimport.Keyserver;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.DeleteOperation;
import org.sufficientlysecure.keychain.operations.EditKeyOperation;
import org.sufficientlysecure.keychain.operations.ImportExportOperation;
import org.sufficientlysecure.keychain.operations.PromoteKeyOperation;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.DeleteResult;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKeyRing;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerify;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.pgp.Progressable;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralMsgIdException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler.MessageStatus;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.measite.minidns.Client;
import de.measite.minidns.DNSMessage;
import de.measite.minidns.Question;
import de.measite.minidns.Record;
import de.measite.minidns.record.Data;
import de.measite.minidns.record.TXT;

/**
 * This Service contains all important long lasting operations for OpenKeychain. It receives Intents with
 * data from the activities or other apps, executes them, and stops itself after doing them.
 */
public class KeychainService extends Service implements Progressable {

    /* extras that can be given by intent */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_DATA = "data";

    /* possible actions */

    public static final String ACTION_VERIFY_KEYBASE_PROOF = Constants.INTENT_PREFIX + "VERIFY_KEYBASE_PROOF";

    public static final String ACTION_CONSOLIDATE = Constants.INTENT_PREFIX + "CONSOLIDATE";

    public static final String ACTION_CANCEL = Constants.INTENT_PREFIX + "CANCEL";

    /* keys for data bundle */

    // keybase proof
    public static final String KEYBASE_REQUIRED_FINGERPRINT = "keybase_required_fingerprint";
    public static final String KEYBASE_PROOF = "keybase_proof";

    // consolidate
    public static final String CONSOLIDATE_RECOVERY = "consolidate_recovery";

    Messenger mMessenger;

    // this attribute can possibly merged with the one above? not sure...
    private AtomicBoolean mActionCanceled = new AtomicBoolean(false);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This is run on the main thread, we need to spawn a runnable which runs on another thread for the actual operation
     */
    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        if (ACTION_CANCEL.equals(intent.getAction())) {
            mActionCanceled.set(true);
            return START_NOT_STICKY;
        }

        Runnable actionRunnable = new Runnable() {
            @Override
            public void run() {
                // We have not been cancelled! (yet)
                mActionCanceled.set(false);

                Bundle extras = intent.getExtras();
                if (extras == null) {
                    Log.e(Constants.TAG, "Extras bundle is null!");
                    return;
                }

                if (!(extras.containsKey(EXTRA_MESSENGER) || extras.containsKey(EXTRA_DATA) || (intent
                        .getAction() == null))) {
                    Log.e(Constants.TAG,
                            "Extra bundle must contain a messenger, a data bundle, and an action!");
                    return;
                }

                Uri dataUri = intent.getData();

                mMessenger = (Messenger) extras.get(EXTRA_MESSENGER);
                Bundle data = extras.getBundle(EXTRA_DATA);
                if (data == null) {
                    Log.e(Constants.TAG, "data extra is null!");
                    return;
                }

                Log.logDebugBundle(data, "EXTRA_DATA");

                ProviderHelper providerHelper = new ProviderHelper(KeychainService.this);

                String action = intent.getAction();

                // executeServiceMethod action from extra bundle
                switch (action) {
                    case ACTION_CONSOLIDATE: {

                        // Operation
                        ConsolidateResult result;
                        if (data.containsKey(CONSOLIDATE_RECOVERY) && data.getBoolean(CONSOLIDATE_RECOVERY)) {
                            result = providerHelper.consolidateDatabaseStep2(KeychainService.this);
                        } else {
                            result = providerHelper.consolidateDatabaseStep1(KeychainService.this);
                        }

                        // Result
                        sendMessageToHandler(MessageStatus.OKAY, result);

                        break;
                    }
                    case ACTION_VERIFY_KEYBASE_PROOF: {

                        try {
                            Proof proof = new Proof(new JSONObject(data.getString(KEYBASE_PROOF)));
                            setProgress(R.string.keybase_message_fetching_data, 0, 100);

                            Prover prover = Prover.findProverFor(proof);

                            if (prover == null) {
                                sendProofError(getString(R.string.keybase_no_prover_found) + ": " + proof
                                        .getPrettyName());
                                return;
                            }

                            if (!prover.fetchProofData()) {
                                sendProofError(prover.getLog(), getString(R.string.keybase_problem_fetching_evidence));
                                return;
                            }
                            String requiredFingerprint = data.getString(KEYBASE_REQUIRED_FINGERPRINT);
                            if (!prover.checkFingerprint(requiredFingerprint)) {
                                sendProofError(getString(R.string.keybase_key_mismatch));
                                return;
                            }

                            String domain = prover.dnsTxtCheckRequired();
                            if (domain != null) {
                                DNSMessage dnsQuery = new Client().query(new Question(domain, Record.TYPE.TXT));
                                if (dnsQuery == null) {
                                    sendProofError(prover.getLog(), getString(R.string.keybase_dns_query_failure));
                                    return;
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
                                    sendProofError(prover.getLog(), null);
                                    return;
                                }
                            }

                            byte[] messageBytes = prover.getPgpMessage().getBytes();
                            if (prover.rawMessageCheckRequired()) {
                                InputStream messageByteStream = PGPUtil.getDecoderStream(new ByteArrayInputStream
                                        (messageBytes));
                                if (!prover.checkRawMessageBytes(messageByteStream)) {
                                    sendProofError(prover.getLog(), null);
                                    return;
                                }
                            }

                            PgpDecryptVerify op = new PgpDecryptVerify(KeychainService.this, providerHelper,
                                    KeychainService.this);

                            PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel(messageBytes)
                                    .setSignedLiteralData(true)
                                    .setRequiredSignerFingerprint(requiredFingerprint);

                            DecryptVerifyResult decryptVerifyResult = op.execute(input, new CryptoInputParcel());

                            if (!decryptVerifyResult.success()) {
                                OperationLog log = decryptVerifyResult.getLog();
                                OperationResult.LogEntryParcel lastEntry = null;
                                for (OperationResult.LogEntryParcel entry : log) {
                                    lastEntry = entry;
                                }
                                sendProofError(getString(lastEntry.mType.getMsgId()));
                                return;
                            }

                            if (!prover.validate(new String(decryptVerifyResult.getOutputBytes()))) {
                                sendProofError(getString(R.string.keybase_message_payload_mismatch));
                                return;
                            }

                            Bundle resultData = new Bundle();
                            resultData.putString(ServiceProgressHandler.DATA_MESSAGE, "OK");

                            // these help the handler construct a useful human-readable message
                            resultData.putString(ServiceProgressHandler.KEYBASE_PROOF_URL, prover.getProofUrl());
                            resultData.putString(ServiceProgressHandler.KEYBASE_PRESENCE_URL, prover.getPresenceUrl());
                            resultData.putString(ServiceProgressHandler.KEYBASE_PRESENCE_LABEL, prover
                                    .getPresenceLabel());
                            sendMessageToHandler(MessageStatus.OKAY, resultData);
                        } catch (Exception e) {
                            sendErrorToHandler(e);
                        }

                        break;
                    }
                }
                stopSelf();
            }
        };

        Thread actionThread = new Thread(actionRunnable);
        actionThread.start();

        return START_NOT_STICKY;
    }

    private void sendProofError(List<String> log, String label) {
        String msg = null;
        label = (label == null) ? "" : label + ": ";
        for (String m : log) {
            Log.e(Constants.TAG, label + m);
            msg = m;
        }
        sendProofError(label + msg);
    }

    private void sendProofError(String msg) {
        Bundle bundle = new Bundle();
        bundle.putString(ServiceProgressHandler.DATA_ERROR, msg);
        sendMessageToHandler(MessageStatus.OKAY, bundle);
    }

    private void sendErrorToHandler(Exception e) {
        // TODO: Implement a better exception handling here
        // contextualize the exception, if necessary
        String message;
        if (e instanceof PgpGeneralMsgIdException) {
            e = ((PgpGeneralMsgIdException) e).getContextualized(KeychainService.this);
            message = e.getMessage();
        } else {
            message = e.getMessage();
        }
        Log.d(Constants.TAG, "KeychainService Exception: ", e);

        Bundle data = new Bundle();
        data.putString(ServiceProgressHandler.DATA_ERROR, message);
        sendMessageToHandler(MessageStatus.EXCEPTION, null, data);
    }

    private void sendMessageToHandler(MessageStatus status, Integer arg2, Bundle data) {

        Message msg = Message.obtain();
        assert msg != null;
        msg.arg1 = status.ordinal();
        if (arg2 != null) {
            msg.arg2 = arg2;
        }
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }

    private void sendMessageToHandler(MessageStatus status, OperationResult data) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(OperationResult.EXTRA_RESULT, data);
        sendMessageToHandler(status, null, bundle);
    }

    private void sendMessageToHandler(MessageStatus status, Bundle data) {
        sendMessageToHandler(status, null, data);
    }

    private void sendMessageToHandler(MessageStatus status) {
        sendMessageToHandler(status, null, null);
    }

    /**
     * Set progress of ProgressDialog by sending message to handler on UI thread
     */
    @Override
    public void setProgress(String message, int progress, int max) {
        Log.d(Constants.TAG, "Send message by setProgress with progress=" + progress + ", max="
                + max);

        Bundle data = new Bundle();
        if (message != null) {
            data.putString(ServiceProgressHandler.DATA_MESSAGE, message);
        }
        data.putInt(ServiceProgressHandler.DATA_PROGRESS, progress);
        data.putInt(ServiceProgressHandler.DATA_PROGRESS_MAX, max);

        sendMessageToHandler(MessageStatus.UPDATE_PROGRESS, null, data);
    }

    @Override
    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    @Override
    public void setProgress(int progress, int max) {
        setProgress(null, progress, max);
    }

    @Override
    public void setPreventCancel() {
        sendMessageToHandler(MessageStatus.PREVENT_CANCEL);
    }
}