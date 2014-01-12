/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.ui;

import java.io.File;
import java.util.Vector;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.helper.ActionBarHelper;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.dialog.DeleteFileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.FileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Choice;
import org.sufficientlysecure.keychain.util.Log;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.beardedhen.androidbootstrap.BootstrapButton;

public class EncryptActivity extends DrawerActivity {

    /* Intents */
    public static final String ACTION_ENCRYPT = Constants.INTENT_PREFIX + "ENCRYPT";

    /* EXTRA keys for input */
    public static final String EXTRA_TEXT = "text";

    // enables ASCII Armor for file encryption when uri is given
    public static final String EXTRA_ASCII_ARMOR = "ascii_armor";

    // preselect ids, for internal use
    public static final String EXTRA_SIGNATURE_KEY_ID = "signature_key_id";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryption_key_ids";

    private long mEncryptionKeyIds[] = null;

    private EditText mMessage = null;
    private BootstrapButton mSelectKeysButton = null;

    private boolean mEncryptEnabled = false;
    private String mEncryptString = "";
    private boolean mEncryptToClipboardEnabled = false;
    private String mEncryptToClipboardString = "";

    private CheckBox mSign = null;
    private TextView mMainUserId = null;
    private TextView mMainUserIdRest = null;

    private ViewFlipper mSource = null;
    private TextView mSourceLabel = null;
    private ImageView mSourcePrevious = null;
    private ImageView mSourceNext = null;

    private ViewFlipper mMode = null;
    private TextView mModeLabel = null;
    private ImageView mModePrevious = null;
    private ImageView mModeNext = null;

    private int mEncryptTarget;

    private EditText mPassPhrase = null;
    private EditText mPassPhraseAgain = null;
    private CheckBox mAsciiArmor = null;
    private Spinner mFileCompression = null;

    private EditText mFilename = null;
    private CheckBox mDeleteAfter = null;
    private BootstrapButton mBrowse = null;

    private String mInputFilename = null;
    private String mOutputFilename = null;

    private boolean mAsciiArmorDemand = false;
    private boolean mOverrideAsciiArmor = false;

    private boolean mGenerateSignature = false;

    private long mSecretKeyId = Id.key.none;

    private FileDialogFragment mFileDialog;

    /**
     * ActionBar menu is created based on class variables to change it at runtime
     * 
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mEncryptToClipboardEnabled) {
            menu.add(1, Id.menu.option.encrypt_to_clipboard, 0, mEncryptToClipboardString)
                    .setShowAsAction(
                            MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        if (mEncryptEnabled) {
            menu.add(1, Id.menu.option.encrypt, 1, mEncryptString).setShowAsAction(
                    MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case Id.menu.option.encrypt_to_clipboard:
            encryptToClipboardClicked();

            return true;

        case Id.menu.option.encrypt:
            encryptClicked();

            return true;

        default:
            return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.encrypt_activity);

        // set actionbar without home button if called from another app
        ActionBarHelper.setBackButton(this);

        initView();

        setupDrawerNavigation(savedInstanceState);

        // Handle intent actions
        handleActions(getIntent());

        updateView();
        updateSource();
        updateMode();

        updateActionBarButtons();
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
        Uri uri = intent.getData();

        if (extras == null) {
            extras = new Bundle();
        }

        /*
         * Android's Action
         */
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // When sending to APG Encrypt via share menu
            if ("text/plain".equals(type)) {
                // Plain text
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    // handle like normal text encryption, override action and extras to later
                    // execute ACTION_ENCRYPT in main actions
                    extras.putString(EXTRA_TEXT, sharedText);
                    extras.putBoolean(EXTRA_ASCII_ARMOR, true);
                    action = ACTION_ENCRYPT;
                }
            } else {
                // Files via content provider, override uri and action
                uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                action = ACTION_ENCRYPT;
            }
        }

        if (extras.containsKey(EXTRA_ASCII_ARMOR)) {
            mAsciiArmorDemand = extras.getBoolean(EXTRA_ASCII_ARMOR, true);
            mOverrideAsciiArmor = true;
            mAsciiArmor.setChecked(mAsciiArmorDemand);
        }

        String textData = extras.getString(EXTRA_TEXT);

        long signatureKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
        long[] encryptionKeyIds = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);

        // preselect keys given by intent
        preselectKeys(signatureKeyId, encryptionKeyIds);

        /**
         * Main Actions
         */
        if (ACTION_ENCRYPT.equals(action) && textData != null) {
            // encrypt text based on given extra

            mMessage.setText(textData);
            mSource.setInAnimation(null);
            mSource.setOutAnimation(null);
            while (mSource.getCurrentView().getId() != R.id.sourceMessage) {
                mSource.showNext();
            }
        } else if (ACTION_ENCRYPT.equals(action) && uri != null) {
            // encrypt file based on Uri

            // get file path from uri
            String path = FileHelper.getPath(this, uri);

            if (path != null) {
                mInputFilename = path;
                mFilename.setText(mInputFilename);

                mSource.setInAnimation(null);
                mSource.setOutAnimation(null);
                while (mSource.getCurrentView().getId() != R.id.sourceFile) {
                    mSource.showNext();
                }
            } else {
                Log.e(Constants.TAG,
                        "Direct binary data without actual file in filesystem is not supported by Intents. Please use the Remote Service API!");
                Toast.makeText(this, R.string.error_only_files_are_supported, Toast.LENGTH_LONG)
                        .show();
                // end activity
                finish();
            }
        } else {
            Log.e(Constants.TAG,
                    "Include the extra 'text' or an Uri with setData() in your Intent!");
        }
    }

    /**
     * If an Intent gives a signatureKeyId and/or encryptionKeyIds, preselect those!
     * 
     * @param preselectedSignatureKeyId
     * @param preselectedEncryptionKeyIds
     */
    private void preselectKeys(long preselectedSignatureKeyId, long[] preselectedEncryptionKeyIds) {
        if (preselectedSignatureKeyId != 0) {
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(this,
                    preselectedSignatureKeyId);
            PGPSecretKey masterKey = null;
            if (keyRing != null) {
                masterKey = PgpKeyHelper.getMasterKey(keyRing);
                if (masterKey != null) {
                    Vector<PGPSecretKey> signKeys = PgpKeyHelper.getUsableSigningKeys(keyRing);
                    if (signKeys.size() > 0) {
                        mSecretKeyId = masterKey.getKeyID();
                    }
                }
            }
        }

        if (preselectedEncryptionKeyIds != null) {
            Vector<Long> goodIds = new Vector<Long>();
            for (int i = 0; i < preselectedEncryptionKeyIds.length; ++i) {
                PGPPublicKeyRing keyRing = ProviderHelper.getPGPPublicKeyRingByMasterKeyId(this,
                        preselectedEncryptionKeyIds[i]);
                PGPPublicKey masterKey = null;
                if (keyRing == null) {
                    continue;
                }
                masterKey = PgpKeyHelper.getMasterKey(keyRing);
                if (masterKey == null) {
                    continue;
                }
                Vector<PGPPublicKey> encryptKeys = PgpKeyHelper.getUsableEncryptKeys(keyRing);
                if (encryptKeys.size() == 0) {
                    continue;
                }
                goodIds.add(masterKey.getKeyID());
            }
            if (goodIds.size() > 0) {
                mEncryptionKeyIds = new long[goodIds.size()];
                for (int i = 0; i < goodIds.size(); ++i) {
                    mEncryptionKeyIds[i] = goodIds.get(i);
                }
            }
        }
    }

    /**
     * Guess output filename based on input path
     * 
     * @param path
     * @return Suggestion for output filename
     */
    private String guessOutputFilename(String path) {
        // output in the same directory but with additional ending
        File file = new File(path);
        String ending = (mAsciiArmor.isChecked() ? ".asc" : ".gpg");
        String outputFilename = file.getParent() + File.separator + file.getName() + ending;

        return outputFilename;
    }

    private void updateSource() {
        switch (mSource.getCurrentView().getId()) {
        case R.id.sourceFile: {
            mSourceLabel.setText(R.string.label_file);
            break;
        }

        case R.id.sourceMessage: {
            mSourceLabel.setText(R.string.label_message);
            break;
        }

        default: {
            break;
        }
        }
        updateActionBarButtons();
    }

    /**
     * Set ActionBar buttons based on parameters
     * 
     * @param encryptEnabled
     * @param encryptStringRes
     * @param encryptToClipboardEnabled
     * @param encryptToClipboardStringRes
     */
    @SuppressLint("NewApi")
    private void setActionbarButtons(boolean encryptEnabled, int encryptStringRes,
            boolean encryptToClipboardEnabled, int encryptToClipboardStringRes) {
        mEncryptEnabled = encryptEnabled;
        if (encryptEnabled) {
            mEncryptString = getString(encryptStringRes);
        }
        mEncryptToClipboardEnabled = encryptToClipboardEnabled;
        if (encryptToClipboardEnabled) {
            mEncryptToClipboardString = getString(encryptToClipboardStringRes);
        }

        // build new action bar based on these class variables
        invalidateOptionsMenu();
    }

    /**
     * Update ActionBar buttons based on current selection in view
     */
    private void updateActionBarButtons() {
        switch (mSource.getCurrentView().getId()) {
        case R.id.sourceFile: {
            setActionbarButtons(true, R.string.btn_encrypt_file, false, 0);

            break;
        }

        case R.id.sourceMessage: {
            mSourceLabel.setText(R.string.label_message);

            if (mMode.getCurrentView().getId() == R.id.modeSymmetric) {
                setActionbarButtons(true, R.string.btn_encrypt_and_send, true,
                        R.string.btn_encrypt_to_clipboard);
            } else {
                if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
                    if (mSecretKeyId == 0) {
                        setActionbarButtons(false, 0, false, 0);
                    } else {
                        setActionbarButtons(true, R.string.btn_sign_and_send, true,
                                R.string.btn_sign_to_clipboard);
                    }
                } else {
                    setActionbarButtons(true, R.string.btn_encrypt_and_send, true,
                            R.string.btn_encrypt_to_clipboard);
                }
            }
            break;
        }

        default: {
            break;
        }
        }

    }

    private void updateMode() {
        switch (mMode.getCurrentView().getId()) {
        case R.id.modeAsymmetric: {
            mModeLabel.setText(R.string.label_asymmetric);
            break;
        }

        case R.id.modeSymmetric: {
            mModeLabel.setText(R.string.label_symmetric);
            break;
        }

        default: {
            break;
        }
        }
        updateActionBarButtons();
    }

    private void encryptToClipboardClicked() {
        mEncryptTarget = Id.target.clipboard;
        initiateEncryption();
    }

    private void encryptClicked() {
        Log.d(Constants.TAG, "encryptClicked invoked!");

        if (mSource.getCurrentView().getId() == R.id.sourceFile) {
            mEncryptTarget = Id.target.file;
        } else {
            mEncryptTarget = Id.target.email;
        }
        initiateEncryption();
    }

    private void initiateEncryption() {
        if (mEncryptTarget == Id.target.file) {
            String currentFilename = mFilename.getText().toString();
            if (mInputFilename == null || !mInputFilename.equals(currentFilename)) {
                mInputFilename = mFilename.getText().toString();
            }

            mOutputFilename = guessOutputFilename(mInputFilename);

            if (mInputFilename.equals("")) {
                Toast.makeText(this, R.string.no_file_selected, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!mInputFilename.startsWith("content")) {
                File file = new File(mInputFilename);
                if (!file.exists() || !file.isFile()) {
                    Toast.makeText(
                            this,
                            getString(R.string.error_message,
                                    getString(R.string.error_file_not_found)), Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
            }
        }

        // symmetric encryption
        if (mMode.getCurrentView().getId() == R.id.modeSymmetric) {
            boolean gotPassPhrase = false;
            String passPhrase = mPassPhrase.getText().toString();
            String passPhraseAgain = mPassPhraseAgain.getText().toString();
            if (!passPhrase.equals(passPhraseAgain)) {
                Toast.makeText(this, R.string.passphrases_do_not_match, Toast.LENGTH_SHORT).show();
                return;
            }

            gotPassPhrase = (passPhrase.length() != 0);
            if (!gotPassPhrase) {
                Toast.makeText(this, R.string.passphrase_must_not_be_empty, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
        } else {
            boolean encryptIt = (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0);
            // for now require at least one form of encryption for files
            if (!encryptIt && mEncryptTarget == Id.target.file) {
                Toast.makeText(this, R.string.select_encryption_key, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!encryptIt && mSecretKeyId == 0) {
                Toast.makeText(this, R.string.select_encryption_or_signature_key,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (mSecretKeyId != 0
                    && PassphraseCacheService.getCachedPassphrase(this, mSecretKeyId) == null) {
                showPassphraseDialog();

                return;
            }
        }

        if (mEncryptTarget == Id.target.file) {
            showOutputFileDialog();
        } else {
            encryptStart();
        }
    }

    /**
     * Shows passphrase dialog to cache a new passphrase the user enters for using it later for
     * encryption
     */
    private void showPassphraseDialog() {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    if (mEncryptTarget == Id.target.file) {
                        showOutputFileDialog();
                    } else {
                        encryptStart();
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(
                    EncryptActivity.this, messenger, mSecretKeyId);

            passphraseDialog.show(getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    private void showOutputFileDialog() {
        // Message is received after file is selected
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == FileDialogFragment.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    mOutputFilename = data.getString(FileDialogFragment.MESSAGE_DATA_FILENAME);
                    encryptStart();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        mFileDialog = FileDialogFragment.newInstance(messenger,
                getString(R.string.title_encrypt_to_file),
                getString(R.string.specify_file_to_encrypt_to), mOutputFilename, null,
                Id.request.output_filename);

        mFileDialog.show(getSupportFragmentManager(), "fileDialog");
    }

    private void encryptStart() {
        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(this, KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();

        boolean useAsciiArmor = true;
        long encryptionKeyIds[] = null;
        int compressionId = 0;
        boolean signOnly = false;
        long mSecretKeyIdToPass = 0;

        if (mMode.getCurrentView().getId() == R.id.modeSymmetric) {
            Log.d(Constants.TAG, "Symmetric encryption enabled!");
            String passPhrase = mPassPhrase.getText().toString();
            if (passPhrase.length() == 0) {
                passPhrase = null;
            }
            data.putString(KeychainIntentService.GENERATE_KEY_SYMMETRIC_PASSPHRASE, passPhrase);
        } else {
            mSecretKeyIdToPass = mSecretKeyId;
            encryptionKeyIds = mEncryptionKeyIds;
            signOnly = (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0);
        }

        intent.setAction(KeychainIntentService.ACTION_ENCRYPT_SIGN);

        // choose default settings, target and data bundle by target
        if (mEncryptTarget == Id.target.file) {
            useAsciiArmor = mAsciiArmor.isChecked();
            compressionId = ((Choice) mFileCompression.getSelectedItem()).getId();

            data.putInt(KeychainIntentService.TARGET, KeychainIntentService.TARGET_FILE);

            Log.d(Constants.TAG, "mInputFilename=" + mInputFilename + ", mOutputFilename="
                    + mOutputFilename);

            data.putString(KeychainIntentService.ENCRYPT_INPUT_FILE, mInputFilename);
            data.putString(KeychainIntentService.ENCRYPT_OUTPUT_FILE, mOutputFilename);

        } else {
            useAsciiArmor = true;
            compressionId = Preferences.getPreferences(this).getDefaultMessageCompression();

            data.putInt(KeychainIntentService.TARGET, KeychainIntentService.TARGET_BYTES);

            String message = mMessage.getText().toString();
            if (signOnly) {
                fixBadCharactersForGmail(message);
            }
            data.putByteArray(KeychainIntentService.ENCRYPT_MESSAGE_BYTES, message.getBytes());
        }

        if (mOverrideAsciiArmor) {
            useAsciiArmor = mAsciiArmorDemand;
        }

        data.putLong(KeychainIntentService.ENCRYPT_SECRET_KEY_ID, mSecretKeyIdToPass);
        data.putBoolean(KeychainIntentService.ENCRYPT_USE_ASCII_ARMOR, useAsciiArmor);
        data.putLongArray(KeychainIntentService.ENCRYPT_ENCRYPTION_KEYS_IDS, encryptionKeyIds);
        data.putInt(KeychainIntentService.ENCRYPT_COMPRESSION_ID, compressionId);
        data.putBoolean(KeychainIntentService.ENCRYPT_GENERATE_SIGNATURE, mGenerateSignature);
        data.putBoolean(KeychainIntentService.ENCRYPT_SIGN_ONLY, signOnly);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in ApgService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(this,
                R.string.progress_encrypting, ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle data = message.getData();

                    String output;
                    switch (mEncryptTarget) {
                    case Id.target.clipboard:
                        output = data.getString(KeychainIntentService.RESULT_ENCRYPTED_STRING);
                        Log.d(Constants.TAG, "output: " + output);
                        ClipboardReflection.copyToClipboard(EncryptActivity.this, output);
                        Toast.makeText(EncryptActivity.this,
                                R.string.encryption_to_clipboard_successful, Toast.LENGTH_SHORT)
                                .show();
                        break;

                    case Id.target.email:

                        output = data.getString(KeychainIntentService.RESULT_ENCRYPTED_STRING);
                        Log.d(Constants.TAG, "output: " + output);

                        Intent sendIntent = new Intent(Intent.ACTION_SEND);

                        // Type is set to text/plain so that encrypted messages can
                        // be sent with Whatsapp, Hangouts, SMS etc...
                        sendIntent.setType("text/plain");

                        sendIntent.putExtra(Intent.EXTRA_TEXT, output);
                        startActivity(Intent.createChooser(sendIntent,
                                getString(R.string.title_send_email)));
                        break;

                    case Id.target.file:
                        Toast.makeText(EncryptActivity.this, R.string.encryption_successful,
                                Toast.LENGTH_SHORT).show();

                        if (mDeleteAfter.isChecked()) {
                            // Create and show dialog to delete original file
                            DeleteFileDialogFragment deleteFileDialog = DeleteFileDialogFragment
                                    .newInstance(mInputFilename);
                            deleteFileDialog.show(getSupportFragmentManager(), "deleteDialog");
                        }
                        break;

                    default:
                        // shouldn't happen
                        break;

                    }
                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }

    /**
     * Fixes bad message characters for gmail
     * 
     * @param message
     * @return
     */
    private String fixBadCharactersForGmail(String message) {
        // fix the message a bit, trailing spaces and newlines break stuff,
        // because GMail sends as HTML and such things fuck up the
        // signature,
        // TODO: things like "<" and ">" also fuck up the signature
        message = message.replaceAll(" +\n", "\n");
        message = message.replaceAll("\n\n+", "\n\n");
        message = message.replaceFirst("^\n+", "");
        // make sure there'll be exactly one newline at the end
        message = message.replaceFirst("\n*$", "\n");

        return message;
    }

    private void initView() {
        mSource = (ViewFlipper) findViewById(R.id.source);
        mSourceLabel = (TextView) findViewById(R.id.sourceLabel);
        mSourcePrevious = (ImageView) findViewById(R.id.sourcePrevious);
        mSourceNext = (ImageView) findViewById(R.id.sourceNext);

        mSourcePrevious.setClickable(true);
        mSourcePrevious.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mSource.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                        R.anim.push_right_in));
                mSource.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                        R.anim.push_right_out));
                mSource.showPrevious();
                updateSource();
            }
        });

        mSourceNext.setClickable(true);
        OnClickListener nextSourceClickListener = new OnClickListener() {
            public void onClick(View v) {
                mSource.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                        R.anim.push_left_in));
                mSource.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                        R.anim.push_left_out));
                mSource.showNext();
                updateSource();
            }
        };
        mSourceNext.setOnClickListener(nextSourceClickListener);

        mSourceLabel.setClickable(true);
        mSourceLabel.setOnClickListener(nextSourceClickListener);

        mMode = (ViewFlipper) findViewById(R.id.mode);
        mModeLabel = (TextView) findViewById(R.id.modeLabel);
        mModePrevious = (ImageView) findViewById(R.id.modePrevious);
        mModeNext = (ImageView) findViewById(R.id.modeNext);

        mModePrevious.setClickable(true);
        mModePrevious.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mMode.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                        R.anim.push_right_in));
                mMode.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                        R.anim.push_right_out));
                mMode.showPrevious();
                updateMode();
            }
        });

        OnClickListener nextModeClickListener = new OnClickListener() {
            public void onClick(View v) {
                mMode.setInAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                        R.anim.push_left_in));
                mMode.setOutAnimation(AnimationUtils.loadAnimation(EncryptActivity.this,
                        R.anim.push_left_out));
                mMode.showNext();
                updateMode();
            }
        };
        mModeNext.setOnClickListener(nextModeClickListener);

        mModeLabel.setClickable(true);
        mModeLabel.setOnClickListener(nextModeClickListener);

        mMessage = (EditText) findViewById(R.id.message);
        mSelectKeysButton = (BootstrapButton) findViewById(R.id.btn_selectEncryptKeys);
        mSign = (CheckBox) findViewById(R.id.sign);
        mMainUserId = (TextView) findViewById(R.id.mainUserId);
        mMainUserIdRest = (TextView) findViewById(R.id.mainUserIdRest);

        mPassPhrase = (EditText) findViewById(R.id.passPhrase);
        mPassPhraseAgain = (EditText) findViewById(R.id.passPhraseAgain);

        // measure the height of the source_file view and set the message view's min height to that,
        // so it fills mSource fully... bit of a hack.
        View tmp = findViewById(R.id.sourceFile);
        tmp.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int height = tmp.getMeasuredHeight();
        mMessage.setMinimumHeight(height);

        mFilename = (EditText) findViewById(R.id.filename);
        mBrowse = (BootstrapButton) findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FileHelper.openFile(EncryptActivity.this, mFilename.getText().toString(), "*/*",
                        Id.request.filename);
            }
        });

        mFileCompression = (Spinner) findViewById(R.id.fileCompression);
        Choice[] choices = new Choice[] {
                new Choice(Id.choice.compression.none, getString(R.string.choice_none) + " ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Id.choice.compression.zip, "ZIP ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Id.choice.compression.zlib, "ZLIB ("
                        + getString(R.string.compression_fast) + ")"),
                new Choice(Id.choice.compression.bzip2, "BZIP2 ("
                        + getString(R.string.compression_very_slow) + ")"), };
        ArrayAdapter<Choice> adapter = new ArrayAdapter<Choice>(this,
                android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFileCompression.setAdapter(adapter);

        int defaultFileCompression = Preferences.getPreferences(this).getDefaultFileCompression();
        for (int i = 0; i < choices.length; ++i) {
            if (choices[i].getId() == defaultFileCompression) {
                mFileCompression.setSelection(i);
                break;
            }
        }

        mDeleteAfter = (CheckBox) findViewById(R.id.deleteAfterEncryption);

        mAsciiArmor = (CheckBox) findViewById(R.id.asciiArmour);
        mAsciiArmor.setChecked(Preferences.getPreferences(this).getDefaultAsciiArmour());

        mSelectKeysButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                selectPublicKeys();
            }
        });

        mSign.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    selectSecretKey();
                } else {
                    mSecretKeyId = Id.key.none;
                    updateView();
                }
            }
        });
    }

    private void updateView() {
        if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
            mSelectKeysButton.setText(getString(R.string.no_keys_selected));
        } else if (mEncryptionKeyIds.length == 1) {
            mSelectKeysButton.setText(getString(R.string.one_key_selected));
        } else {
            mSelectKeysButton.setText("" + mEncryptionKeyIds.length + " "
                    + getResources().getString(R.string.n_keys_selected));
        }

        if (mSecretKeyId == Id.key.none) {
            mSign.setChecked(false);
            mMainUserId.setText("");
            mMainUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknown_user_id);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(this,
                    mSecretKeyId);
            if (keyRing != null) {
                PGPSecretKey key = PgpKeyHelper.getMasterKey(keyRing);
                if (key != null) {
                    String userId = PgpKeyHelper.getMainUserIdSafe(this, key);
                    String chunks[] = userId.split(" <", 2);
                    uid = chunks[0];
                    if (chunks.length > 1) {
                        uidExtra = "<" + chunks[1];
                    }
                }
            }
            mMainUserId.setText(uid);
            mMainUserIdRest.setText(uidExtra);
            mSign.setChecked(true);
        }

        updateActionBarButtons();
    }

    private void selectPublicKeys() {
        Intent intent = new Intent(this, SelectPublicKeyActivity.class);
        Vector<Long> keyIds = new Vector<Long>();
        if (mSecretKeyId != 0) {
            keyIds.add(mSecretKeyId);
        }
        if (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0) {
            for (int i = 0; i < mEncryptionKeyIds.length; ++i) {
                keyIds.add(mEncryptionKeyIds[i]);
            }
        }
        long[] initialKeyIds = null;
        if (keyIds.size() > 0) {
            initialKeyIds = new long[keyIds.size()];
            for (int i = 0; i < keyIds.size(); ++i) {
                initialKeyIds[i] = keyIds.get(i);
            }
        }
        intent.putExtra(SelectPublicKeyActivity.EXTRA_SELECTED_MASTER_KEY_IDS, initialKeyIds);
        startActivityForResult(intent, Id.request.public_keys);
    }

    private void selectSecretKey() {
        Intent intent = new Intent(this, SelectSecretKeyActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Id.request.filename: {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    String path = FileHelper.getPath(this, data.getData());
                    Log.d(Constants.TAG, "path=" + path);

                    mFilename.setText(path);
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "Nullpointer while retrieving path!");
                }
            }
            return;
        }

        case Id.request.output_filename: {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    String path = data.getData().getPath();
                    Log.d(Constants.TAG, "path=" + path);

                    mFileDialog.setFilename(path);
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "Nullpointer while retrieving path!");
                }
            }
            return;
        }

        case Id.request.public_keys: {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                mEncryptionKeyIds = bundle
                        .getLongArray(SelectPublicKeyActivity.RESULT_EXTRA_MASTER_KEY_IDS);
            }
            updateView();
            break;
        }

        case Id.request.secret_keys: {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                mSecretKeyId = bundle.getLong(SelectSecretKeyActivity.RESULT_EXTRA_MASTER_KEY_ID);
            } else {
                mSecretKeyId = Id.key.none;
            }
            updateView();
            break;
        }

        default: {
            break;
        }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}
