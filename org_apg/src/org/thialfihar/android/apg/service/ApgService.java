/**
 * TODO:
 * - Reimplement all the threads in the activitys as intents in this intentService
 * - This IntentService stopps itself after an action is executed
 */

package org.thialfihar.android.apg.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Apg;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.ProgressDialogUpdater;
import org.thialfihar.android.apg.ui.widget.KeyEditor;
import org.thialfihar.android.apg.ui.widget.SectionView;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

//TODO: ProgressDialogUpdater rework???
public class ApgService extends IntentService implements ProgressDialogUpdater {

    // extras that can be given by intent
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_DATA = "data";

    // keys for data bundle
    // edit keys
    public static final String DATA_NEW_PASSPHRASE = "new_passphrase";
    public static final String DATA_CURRENT_PASSPHRASE = "current_passphrase";
    public static final String DATA_USER_IDS = "user_ids";
    public static final String DATA_KEYS = "keys";
    public static final String DATA_KEYS_USAGES = "keys_usages";
    public static final String DATA_MASTER_KEY_ID = "master_key_id";

    // possible ints for EXTRA_ACTION
    public static final int ACTION_SAVE_KEYRING = 1;

    // possible messages send from this service to handler on ui
    public static final int MESSAGE_OKAY = 1;
    public static final int MESSAGE_EXCEPTION = 2;

    // possible data keys for messages
    public static final String MESSAGE_DATA_ERROR = "error";

    Messenger mMessenger;

    public ApgService() {
        super("ApgService");
    }

    /**
     * The IntentService calls this method from the default worker thread with the intent that
     * started the service. When this method returns, IntentService stops the service, as
     * appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {

            if (!extras.containsKey(EXTRA_ACTION)) {
                Log.e(Constants.TAG, "Extra bundle must contain a action!");
                return;
            }

            if (!extras.containsKey(EXTRA_MESSENGER)) {
                Log.e(Constants.TAG, "Extra bundle must contain a messenger!");
                return;
            }
            mMessenger = (Messenger) extras.get(EXTRA_MESSENGER);

            Bundle data = null;
            if (extras.containsKey(EXTRA_DATA)) {
                data = extras.getBundle(EXTRA_DATA);
            }

            int action = extras.getInt(EXTRA_ACTION);

            // will be filled if error occurs
            String error = "";

            // execute action from extra bundle
            switch (action) {
            case ACTION_SAVE_KEYRING:

                try {
                    String oldPassPhrase = data.getString(DATA_CURRENT_PASSPHRASE);
                    String newPassPhrase = data.getString(DATA_NEW_PASSPHRASE);
                    if (newPassPhrase == null) {
                        newPassPhrase = oldPassPhrase;
                    }
                    ArrayList<String> userIds = (ArrayList<String>) data
                            .getSerializable(DATA_USER_IDS);

                    byte[] keysBytes = data.getByteArray(DATA_KEYS);

                    // convert back from byte[] to ArrayList<PGPSecretKey>
                    PGPObjectFactory factory = new PGPObjectFactory(keysBytes);
                    PGPSecretKeyRing keyRing = null;
                    if ((keyRing = (PGPSecretKeyRing) factory.nextObject()) == null) {
                        Log.e(Constants.TAG, "No keys given!");
                    }
                    ArrayList<PGPSecretKey> keys = new ArrayList<PGPSecretKey>();

                    Iterator<PGPSecretKey> itr = keyRing.getSecretKeys();
                    while (itr.hasNext()) {
                        keys.add(itr.next());
                        Log.d(Constants.TAG, "added...");
                    }

                    ArrayList<Integer> keysUsages = (ArrayList<Integer>) data
                            .getSerializable(DATA_KEYS_USAGES);
                    long masterKeyId = data.getLong(DATA_MASTER_KEY_ID);

                    Apg.buildSecretKey(this, userIds, keys, keysUsages, masterKeyId, oldPassPhrase,
                            newPassPhrase, this);
                    Apg.setCachedPassPhrase(masterKeyId, newPassPhrase);
                } catch (Exception e) {
                    error = e.getMessage();
                    Log.e(Constants.TAG, "Exception: " + error);
                    e.printStackTrace();
                    sendErrorToUi(error);
                }

                sendMessageToUi(MESSAGE_OKAY, null, null);
                break;

            default:
                break;
            }

        }
    }

    private void sendErrorToUi(String error) {
        Bundle data = new Bundle();
        data.putString(MESSAGE_DATA_ERROR, error);
        sendMessageToUi(MESSAGE_EXCEPTION, null, data);
    }

    private void sendMessageToUi(Integer arg1, Integer arg2, Bundle data) {
        Message msg = Message.obtain();
        msg.arg1 = arg1;
        if (arg2 != null) {
            msg.arg2 = arg2;
        }
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }

    public void setProgress(int resourceId, int progress, int max) {
        setProgress(getString(resourceId), progress, max);
    }

    public void setProgress(int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Constants.extras.STATUS, Id.message.progress_update);
        data.putInt(Constants.extras.PROGRESS, progress);
        data.putInt(Constants.extras.PROGRESS_MAX, max);
        msg.setData(data);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void setProgress(String message, int progress, int max) {
        Message msg = new Message();
        Bundle data = new Bundle();
        data.putInt(Constants.extras.STATUS, Id.message.progress_update);
        data.putString(Constants.extras.MESSAGE, message);
        data.putInt(Constants.extras.PROGRESS, progress);
        data.putInt(Constants.extras.PROGRESS_MAX, max);
        msg.setData(data);
        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
