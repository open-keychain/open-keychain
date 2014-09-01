/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.DeleteFileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EncryptFileActivity extends DrawerActivity implements EncryptActivityInterface {

    /* Intents */
    public static final String ACTION_ENCRYPT_FILE = Constants.INTENT_PREFIX + "ENCRYPT_FILE";

    // enables ASCII Armor for file encryption when uri is given
    public static final String EXTRA_ASCII_ARMOR = "ascii_armor";

    // preselect ids, for internal use
    public static final String EXTRA_SIGNATURE_KEY_ID = "signature_key_id";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryption_key_ids";

    // view
    private int mCurrentMode = MODE_ASYMMETRIC;

    // tabs
    private static final int MODE_ASYMMETRIC = 0;
    private static final int MODE_SYMMETRIC = 1;

    // model used by fragments
    private long mEncryptionKeyIds[] = null;
    private String mEncryptionUserIds[] = null;
    private long mSigningKeyId = Constants.key.none;
    private String mPassphrase = "";
    private boolean mUseArmor;
    private boolean mDeleteAfterEncrypt = false;
    private boolean mShareAfterEncrypt = false;
    private ArrayList<Uri> mInputUris;
    private ArrayList<Uri> mOutputUris;
    private String mMessage = "";

    public boolean isModeSymmetric() {
        return MODE_SYMMETRIC == mCurrentMode;
    }

    @Override
    public boolean isUseArmor() {
        return mUseArmor;
    }

    @Override
    public long getSignatureKey() {
        return mSigningKeyId;
    }

    @Override
    public long[] getEncryptionKeys() {
        return mEncryptionKeyIds;
    }

    @Override
    public String[] getEncryptionUsers() {
        return mEncryptionUserIds;
    }

    @Override
    public void setSignatureKey(long signatureKey) {
        mSigningKeyId = signatureKey;
        notifyUpdate();
    }

    @Override
    public void setEncryptionKeys(long[] encryptionKeys) {
        mEncryptionKeyIds = encryptionKeys;
        notifyUpdate();
    }

    @Override
    public void setEncryptionUsers(String[] encryptionUsers) {
        mEncryptionUserIds = encryptionUsers;
        notifyUpdate();
    }

    @Override
    public void setPassphrase(String passphrase) {
        mPassphrase = passphrase;
    }

    @Override
    public ArrayList<Uri> getInputUris() {
        if (mInputUris == null) mInputUris = new ArrayList<Uri>();
        return mInputUris;
    }

    @Override
    public ArrayList<Uri> getOutputUris() {
        if (mOutputUris == null) mOutputUris = new ArrayList<Uri>();
        return mOutputUris;
    }

    @Override
    public void setInputUris(ArrayList<Uri> uris) {
        mInputUris = uris;
        notifyUpdate();
    }

    @Override
    public void setOutputUris(ArrayList<Uri> uris) {
        mOutputUris = uris;
        notifyUpdate();
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    @Override
    public void setMessage(String message) {
        mMessage = message;
    }

    @Override
    public void notifyUpdate() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof EncryptActivityInterface.UpdateListener) {
                ((UpdateListener) fragment).onNotifyUpdate();
            }
        }
    }

    @Override
    public void startEncrypt(boolean share) {
        mShareAfterEncrypt = share;
        startEncrypt();
    }

    public void startEncrypt() {
        if (!inputIsValid()) {
            // Notify was created by inputIsValid.
            return;
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_ENCRYPT_SIGN);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, createEncryptBundle());

        // Message is received after encrypting is done in KeychainIntentService
        KeychainIntentServiceHandler serviceHandler = new KeychainIntentServiceHandler(this,
                getString(R.string.progress_encrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    Notify.showNotify(EncryptFileActivity.this, R.string.encrypt_sign_successful, Notify.Style.INFO);

                    if (mDeleteAfterEncrypt) {
                        for (Uri inputUri : mInputUris) {
                            DeleteFileDialogFragment deleteFileDialog = DeleteFileDialogFragment.newInstance(inputUri);
                            deleteFileDialog.show(getSupportFragmentManager(), "deleteDialog");
                        }
                        mInputUris.clear();
                        notifyUpdate();
                    }

                    if (mShareAfterEncrypt) {
                        // Share encrypted message/file
                        startActivity(sendWithChooserExcludingEncrypt(message));
                    } else {
                        // Copy to clipboard
                        copyToClipboard(message);
                        Notify.showNotify(EncryptFileActivity.this,
                                R.string.encrypt_sign_clipboard_successful, Notify.Style.INFO);
                    }
                }
            }
        };
        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(serviceHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        serviceHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    private Bundle createEncryptBundle() {
        // fill values for this action
        Bundle data = new Bundle();

        data.putInt(KeychainIntentService.SOURCE, KeychainIntentService.IO_URIS);
        data.putParcelableArrayList(KeychainIntentService.ENCRYPT_INPUT_URIS, mInputUris);

        data.putInt(KeychainIntentService.TARGET, KeychainIntentService.IO_URIS);
        data.putParcelableArrayList(KeychainIntentService.ENCRYPT_OUTPUT_URIS, mOutputUris);

        data.putInt(KeychainIntentService.ENCRYPT_COMPRESSION_ID,
                Preferences.getPreferences(this).getDefaultFileCompression());

        // Always use armor for messages
        data.putBoolean(KeychainIntentService.ENCRYPT_USE_ASCII_ARMOR, mUseArmor);

        if (isModeSymmetric()) {
            Log.d(Constants.TAG, "Symmetric encryption enabled!");
            String passphrase = mPassphrase;
            if (passphrase.length() == 0) {
                passphrase = null;
            }
            data.putString(KeychainIntentService.ENCRYPT_SYMMETRIC_PASSPHRASE, passphrase);
        } else {
            data.putLong(KeychainIntentService.ENCRYPT_SIGNATURE_KEY_ID, mSigningKeyId);
            data.putLongArray(KeychainIntentService.ENCRYPT_ENCRYPTION_KEYS_IDS, mEncryptionKeyIds);
        }
        return data;
    }

    private void copyToClipboard(Message message) {
        ClipboardReflection.copyToClipboard(this, new String(message.getData().getByteArray(KeychainIntentService.RESULT_BYTES)));
    }

    /**
     * Create Intent Chooser but exclude OK's EncryptActivity.
     * <p/>
     * Put together from some stackoverflow posts...
     *
     * @param message
     * @return
     */
    private Intent sendWithChooserExcludingEncrypt(Message message) {
        Intent prototype = createSendIntent(message);

        String title = getString(R.string.title_share_file);

        // Disabled, produced an empty list on Huawei U8860 with Android Version 4.0.3
//        // fallback on Android 2.3, otherwise we would get weird results
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            return Intent.createChooser(prototype, title);
//        }
//
//        // prevent recursion aka Inception :P
//        String[] blacklist = new String[]{Constants.PACKAGE_NAME + ".ui.EncryptActivity"};
//
//        List<LabeledIntent> targetedShareIntents = new ArrayList<LabeledIntent>();
//
//        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(prototype, 0);
//        List<ResolveInfo> resInfoListFiltered = new ArrayList<ResolveInfo>();
//        if (!resInfoList.isEmpty()) {
//            for (ResolveInfo resolveInfo : resInfoList) {
//                // do not add blacklisted ones
//                if (resolveInfo.activityInfo == null || Arrays.asList(blacklist).contains(resolveInfo.activityInfo.name))
//                    continue;
//
//                resInfoListFiltered.add(resolveInfo);
//            }
//
//            if (!resInfoListFiltered.isEmpty()) {
//                // sorting for nice readability
//                Collections.sort(resInfoListFiltered, new Comparator<ResolveInfo>() {
//                    @Override
//                    public int compare(ResolveInfo first, ResolveInfo second) {
//                        String firstName = first.loadLabel(getPackageManager()).toString();
//                        String secondName = second.loadLabel(getPackageManager()).toString();
//                        return firstName.compareToIgnoreCase(secondName);
//                    }
//                });
//
//                // create the custom intent list
//                for (ResolveInfo resolveInfo : resInfoListFiltered) {
//                    Intent targetedShareIntent = (Intent) prototype.clone();
//                    targetedShareIntent.setPackage(resolveInfo.activityInfo.packageName);
//                    targetedShareIntent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
//
//                    LabeledIntent lIntent = new LabeledIntent(targetedShareIntent,
//                            resolveInfo.activityInfo.packageName,
//                            resolveInfo.loadLabel(getPackageManager()),
//                            resolveInfo.activityInfo.icon);
//                    targetedShareIntents.add(lIntent);
//                }
//
//                // Create chooser with only one Intent in it
//                Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(targetedShareIntents.size() - 1), title);
//                // append all other Intents
//                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
//                return chooserIntent;
//            }
//
//        }

        // fallback to Android's default chooser
        return Intent.createChooser(prototype, title);
    }

    private Intent createSendIntent(Message message) {
        Intent sendIntent;
        // file
        if (mOutputUris.size() == 1) {
            sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, mOutputUris.get(0));
        } else {
            sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.putExtra(Intent.EXTRA_STREAM, mOutputUris);
        }
        sendIntent.setType("application/octet-stream");

        if (!isModeSymmetric() && mEncryptionUserIds != null) {
            Set<String> users = new HashSet<String>();
            for (String user : mEncryptionUserIds) {
                String[] userId = KeyRing.splitUserId(user);
                if (userId[1] != null) {
                    users.add(userId[1]);
                }
            }
            sendIntent.putExtra(Intent.EXTRA_EMAIL, users.toArray(new String[users.size()]));
        }
        return sendIntent;
    }

    private boolean inputIsValid() {
        // file checks

        if (mInputUris.isEmpty()) {
            Notify.showNotify(this, R.string.no_file_selected, Notify.Style.ERROR);
            return false;
        } else if (mInputUris.size() > 1 && !mShareAfterEncrypt) {
            // This should be impossible...
            return false;
        } else if (mInputUris.size() != mOutputUris.size()) {
            // This as well
            return false;
        }

        if (isModeSymmetric()) {
            // symmetric encryption checks

            if (mPassphrase == null) {
                Notify.showNotify(this, R.string.passphrases_do_not_match, Notify.Style.ERROR);
                return false;
            }
            if (mPassphrase.isEmpty()) {
                Notify.showNotify(this, R.string.passphrase_must_not_be_empty, Notify.Style.ERROR);
                return false;
            }

        } else {
            // asymmetric encryption checks

            boolean gotEncryptionKeys = (mEncryptionKeyIds != null
                    && mEncryptionKeyIds.length > 0);

            // Files must be encrypted, only text can be signed-only right now
            if (!gotEncryptionKeys) {
                Notify.showNotify(this, R.string.select_encryption_key, Notify.Style.ERROR);
                return false;
            }

            try {
                if (mSigningKeyId != 0 && PassphraseCacheService.getCachedPassphrase(this, mSigningKeyId) == null) {
                    PassphraseDialogFragment.show(this, mSigningKeyId,
                            new Handler() {
                                @Override
                                public void handleMessage(Message message) {
                                    if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                                        // restart
                                        startEncrypt();
                                    }
                                }
                            }
                    );

                    return false;
                }
            } catch (PassphraseCacheService.KeyNotFoundException e) {
                Log.e(Constants.TAG, "Key not found!", e);
            }
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.encrypt_file_activity);

        // if called with an intent action, do not init drawer navigation
        if (ACTION_ENCRYPT_FILE.equals(getIntent().getAction())) {
            // lock drawer
            ((DrawerLayout) findViewById(R.id.drawer_layout)).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            // TODO: back button to key?
        } else {
            setupDrawerNavigation(savedInstanceState);
        }

        // Handle intent actions
        handleActions(getIntent());
        updateModeFragment();

        mUseArmor = Preferences.getPreferences(this).getDefaultAsciiArmor();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.encrypt_file_activity, menu);
        menu.findItem(R.id.check_use_armor).setChecked(mUseArmor);
        return super.onCreateOptionsMenu(menu);
    }

    private void updateModeFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.encrypt_pager_mode,
                        mCurrentMode == MODE_SYMMETRIC
                                ? new EncryptSymmetricFragment()
                                : new EncryptAsymmetricFragment())
                .commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isCheckable()) {
            item.setChecked(!item.isChecked());
        }
        switch (item.getItemId()) {
            case R.id.check_use_symmetric:
                mCurrentMode = item.isChecked() ? MODE_SYMMETRIC : MODE_ASYMMETRIC;
                updateModeFragment();
                notifyUpdate();
                break;
            case R.id.check_use_armor:
                mUseArmor = item.isChecked();
                notifyUpdate();
                break;
            case R.id.check_delete_after_encrypt:
                mDeleteAfterEncrypt = item.isChecked();
                notifyUpdate();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Handles all actions with this intent
     *
     * @param intent
     */
    private void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();
        ArrayList<Uri> uris = new ArrayList<Uri>();

        if (extras == null) {
            extras = new Bundle();
        }

        if (intent.getData() != null) {
            uris.add(intent.getData());
        }

        /*
         * Android's Action
         */

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // When sending to OpenKeychain Encrypt via share menu
            /* if ("text/plain".equals(type)) {
                // TODO handle, maybe forward to "encrypt text" activity?

                Plain text
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // handle like normal text encryption, override action and extras to later
                    // executeServiceMethod ACTION_ENCRYPT_FILE in main actions
                    extras.putString(EXTRA_TEXT, sharedText);
                    extras.putBoolean(EXTRA_ASCII_ARMOR, true);
                    action = ACTION_ENCRYPT_FILE;
                }

            } else */ {
                // Files via content provider, override uri and action
                uris.clear();
                uris.add(intent.<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
            }
        }

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }

        if (extras.containsKey(EXTRA_ASCII_ARMOR)) {
            mUseArmor = extras.getBoolean(EXTRA_ASCII_ARMOR, true);
        }

        // preselect keys given by intent
        mSigningKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
        mEncryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);

        // Save uris
        mInputUris = uris;

    }

}
