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

package org.thialfihar.android.apg.ui;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.helper.FileHelper;
import org.thialfihar.android.apg.helper.OtherHelper;
import org.thialfihar.android.apg.helper.PGPHelper;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.helper.Preferences;
import org.thialfihar.android.apg.service.ApgServiceHandler;
import org.thialfihar.android.apg.service.ApgService;
import org.thialfihar.android.apg.service.PassphraseCacheService;
import org.thialfihar.android.apg.ui.dialog.DeleteFileDialogFragment;
import org.thialfihar.android.apg.ui.dialog.FileDialogFragment;
import org.thialfihar.android.apg.ui.dialog.PassphraseDialogFragment;
import org.thialfihar.android.apg.util.Choice;
import org.thialfihar.android.apg.util.Compatibility;
import org.thialfihar.android.apg.R;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import org.thialfihar.android.apg.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.io.File;
import java.util.Vector;

public class EncryptActivity extends SherlockFragmentActivity {

    // possible intent actions for this activity
    public static final String ACTION_ENCRYPT = Constants.INTENT_PREFIX + "ENCRYPT";
    public static final String ACTION_ENCRYPT_FILE = Constants.INTENT_PREFIX + "ENCRYPT_FILE";
    public static final String ACTION_ENCRYPT_AND_RETURN = Constants.INTENT_PREFIX
            + "ENCRYPT_AND_RETURN";
    public static final String ACTION_GENERATE_SIGNATURE = Constants.INTENT_PREFIX
            + "GENERATE_SIGNATURE";

    // possible extra keys
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_ASCII_ARMOUR = "asciiArmour";
    public static final String EXTRA_SEND_TO = "sendTo";
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryptionKeyIds";

    private String mSubject = null;
    private String mSendTo = null;

    private long mEncryptionKeyIds[] = null;

    private boolean mReturnResult = false;
    private EditText mMessage = null;
    private Button mSelectKeysButton = null;

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
    private CheckBox mAsciiArmour = null;
    private Spinner mFileCompression = null;

    private EditText mFilename = null;
    private CheckBox mDeleteAfter = null;
    private ImageButton mBrowse = null;

    private String mInputFilename = null;
    private String mOutputFilename = null;

    private boolean mAsciiArmourDemand = false;
    private boolean mOverrideAsciiArmour = false;
    private Uri mContentUri = null;
    private byte[] mData = null;

    private boolean mGenerateSignature = false;

    private long mSecretKeyId = Id.key.none;

    private FileDialogFragment mFileDialog;

    public void setSecretKeyId(long id) {
        mSecretKeyId = id;
    }

    public long getSecretKeyId() {
        return mSecretKeyId;
    }

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

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

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
        setContentView(R.layout.encrypt);

        // set actionbar without home button if called from another app
        OtherHelper.setActionBarBackButton(this);

        mGenerateSignature = false;

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
        mSelectKeysButton = (Button) findViewById(R.id.btn_selectEncryptKeys);
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
        mBrowse = (ImageButton) findViewById(R.id.btn_browse);
        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FileHelper.openFile(EncryptActivity.this, mFilename.getText().toString(), "*/*",
                        Id.request.filename);
            }
        });

        mFileCompression = (Spinner) findViewById(R.id.fileCompression);
        Choice[] choices = new Choice[] {
                new Choice(Id.choice.compression.none, getString(R.string.choice_none) + " ("
                        + getString(R.string.fast) + ")"),
                new Choice(Id.choice.compression.zip, "ZIP (" + getString(R.string.fast) + ")"),
                new Choice(Id.choice.compression.zlib, "ZLIB (" + getString(R.string.fast) + ")"),
                new Choice(Id.choice.compression.bzip2, "BZIP2 (" + getString(R.string.very_slow)
                        + ")"), };
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

        mAsciiArmour = (CheckBox) findViewById(R.id.asciiArmour);
        mAsciiArmour.setChecked(Preferences.getPreferences(this).getDefaultAsciiArmour());
        mAsciiArmour.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                guessOutputFilename();
            }
        });

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
                    setSecretKeyId(Id.key.none);
                    updateView();
                }
            }
        });

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        mContentUri = intent.getData();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // Android's Action when sending to APG Encrypt

            if ("text/plain".equals(type)) {
                // plain text
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    intent.setAction(ACTION_ENCRYPT);
                    intent.putExtra(EXTRA_TEXT, sharedText);
                    intent.putExtra(EXTRA_ASCII_ARMOUR, true);
                    handleActionEncryptSign(intent);
                }
            } else {
                // binary via content provider (could also be files)
                Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri != null) {
                    mContentUri = uri;
                    handleActionEncryptSign(intent);
                }
            }
        } else if (ACTION_ENCRYPT.equals(action) || ACTION_ENCRYPT_FILE.equals(action)
                || ACTION_ENCRYPT_AND_RETURN.equals(action)
                || ACTION_GENERATE_SIGNATURE.equals(action)) {
            // APG's own Actions

            handleActionEncryptSign(intent);
        }

        updateView();
        updateSource();
        updateMode();

        if (mReturnResult) {
            mSourcePrevious.setClickable(false);
            mSourcePrevious.setEnabled(false);
            mSourcePrevious.setVisibility(View.INVISIBLE);

            mSourceNext.setClickable(false);
            mSourceNext.setEnabled(false);
            mSourceNext.setVisibility(View.INVISIBLE);

            mSourceLabel.setClickable(false);
            mSourceLabel.setEnabled(false);
        }

        updateActionBarButtons();

        if (mReturnResult
                && (mMessage.getText().length() > 0 || mData != null || mContentUri != null)
                && ((mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0) || getSecretKeyId() != 0)) {
            encryptClicked();
        }
    }

    /**
     * Handles all actions with this intent
     * 
     * @param intent
     */
    private void handleActionEncryptSign(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            extras = new Bundle();
        }

        if (ACTION_ENCRYPT_AND_RETURN.equals(action) || ACTION_GENERATE_SIGNATURE.equals(action)) {
            mReturnResult = true;
        }

        if (ACTION_GENERATE_SIGNATURE.equals(action)) {
            mGenerateSignature = true;
            mOverrideAsciiArmour = true;
            mAsciiArmourDemand = false;
        }

        if (extras.containsKey(EXTRA_ASCII_ARMOUR)) {
            mAsciiArmourDemand = extras.getBoolean(EXTRA_ASCII_ARMOUR, true);
            mOverrideAsciiArmour = true;
            mAsciiArmour.setChecked(mAsciiArmourDemand);
        }

        mData = extras.getByteArray(EXTRA_DATA);
        String textData = null;
        if (mData == null) {
            textData = extras.getString(EXTRA_TEXT);
        }
        mSendTo = extras.getString(EXTRA_SEND_TO);
        mSubject = extras.getString(EXTRA_SUBJECT);
        long signatureKeyId = extras.getLong(EXTRA_SIGNATURE_KEY_ID);
        long encryptionKeyIds[] = extras.getLongArray(EXTRA_ENCRYPTION_KEY_IDS);
        if (signatureKeyId != 0) {
            PGPSecretKeyRing keyRing = PGPMain.getSecretKeyRing(signatureKeyId);
            PGPSecretKey masterKey = null;
            if (keyRing != null) {
                masterKey = PGPHelper.getMasterKey(keyRing);
                if (masterKey != null) {
                    Vector<PGPSecretKey> signKeys = PGPHelper.getUsableSigningKeys(keyRing);
                    if (signKeys.size() > 0) {
                        setSecretKeyId(masterKey.getKeyID());
                    }
                }
            }
        }

        if (encryptionKeyIds != null) {
            Vector<Long> goodIds = new Vector<Long>();
            for (int i = 0; i < encryptionKeyIds.length; ++i) {
                PGPPublicKeyRing keyRing = PGPMain.getPublicKeyRing(encryptionKeyIds[i]);
                PGPPublicKey masterKey = null;
                if (keyRing == null) {
                    continue;
                }
                masterKey = PGPHelper.getMasterKey(keyRing);
                if (masterKey == null) {
                    continue;
                }
                Vector<PGPPublicKey> encryptKeys = PGPHelper.getUsableEncryptKeys(keyRing);
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

        if (ACTION_ENCRYPT.equals(action) || ACTION_ENCRYPT_AND_RETURN.equals(action)
                || ACTION_GENERATE_SIGNATURE.equals(action)) {
            if (textData != null) {
                mMessage.setText(textData);
            }
            mSource.setInAnimation(null);
            mSource.setOutAnimation(null);
            while (mSource.getCurrentView().getId() != R.id.sourceMessage) {
                mSource.showNext();
            }
        } else if (ACTION_ENCRYPT_FILE.equals(action)) {
            mInputFilename = intent.getData().getPath();
            mFilename.setText(mInputFilename);
            guessOutputFilename();
            mSource.setInAnimation(null);
            mSource.setOutAnimation(null);
            while (mSource.getCurrentView().getId() != R.id.sourceFile) {
                mSource.showNext();
            }
        }
    }

    private void guessOutputFilename() {
        mInputFilename = mFilename.getText().toString();
        File file = new File(mInputFilename);
        String ending = (mAsciiArmour.isChecked() ? ".asc" : ".gpg");
        mOutputFilename = Constants.path.APP_DIR + "/" + file.getName() + ending;
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
            setActionbarButtons(true, R.string.btn_encryptFile, false, 0);

            break;
        }

        case R.id.sourceMessage: {
            mSourceLabel.setText(R.string.label_message);

            if (mMode.getCurrentView().getId() == R.id.modeSymmetric) {
                if (mReturnResult) {
                    setActionbarButtons(true, R.string.btn_encrypt, false, 0);
                } else {
                    setActionbarButtons(true, R.string.btn_encryptAndEmail, true,
                            R.string.btn_encryptToClipboard);
                }
            } else {
                if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
                    if (getSecretKeyId() == 0) {
                        setActionbarButtons(false, 0, false, 0);
                    } else {
                        if (mReturnResult) {
                            setActionbarButtons(true, R.string.btn_sign, false, 0);
                        } else {
                            setActionbarButtons(true, R.string.btn_signAndEmail, true,
                                    R.string.btn_signToClipboard);
                        }
                    }
                } else {
                    if (mReturnResult) {
                        setActionbarButtons(true, R.string.btn_encrypt, false, 0);
                    } else {
                        setActionbarButtons(true, R.string.btn_encryptAndEmail, true,
                                R.string.btn_encryptToClipboard);
                    }
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
                guessOutputFilename();
            }

            if (mInputFilename.equals("")) {
                Toast.makeText(this, R.string.noFileSelected, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!mInputFilename.startsWith("content")) {
                File file = new File(mInputFilename);
                if (!file.exists() || !file.isFile()) {
                    Toast.makeText(
                            this,
                            getString(R.string.errorMessage, getString(R.string.error_fileNotFound)),
                            Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.passPhrasesDoNotMatch, Toast.LENGTH_SHORT).show();
                return;
            }

            gotPassPhrase = (passPhrase.length() != 0);
            if (!gotPassPhrase) {
                Toast.makeText(this, R.string.passPhraseMustNotBeEmpty, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            boolean encryptIt = (mEncryptionKeyIds != null && mEncryptionKeyIds.length > 0);
            // for now require at least one form of encryption for files
            if (!encryptIt && mEncryptTarget == Id.target.file) {
                Toast.makeText(this, R.string.selectEncryptionKey, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!encryptIt && getSecretKeyId() == 0) {
                Toast.makeText(this, R.string.selectEncryptionOrSignatureKey, Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            if (getSecretKeyId() != 0
                    && PassphraseCacheService.getCachedPassphrase(this, getSecretKeyId()) == null) {
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
        } catch (PGPMain.GeneralException e) {
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
                getString(R.string.title_encryptToFile),
                getString(R.string.specifyFileToEncryptTo), mOutputFilename, null,
                Id.request.output_filename);

        mFileDialog.show(getSupportFragmentManager(), "fileDialog");
    }

    private void encryptStart() {
        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(this, ApgService.class);

        // fill values for this action
        Bundle data = new Bundle();

        boolean useAsciiArmour = true;
        long encryptionKeyIds[] = null;
        long signatureKeyId = Id.key.none;
        int compressionId = 0;
        boolean signOnly = false;

        if (mMode.getCurrentView().getId() == R.id.modeSymmetric) {
            Log.d(Constants.TAG, "Symmetric encryption enabled!");
            String passPhrase = mPassPhrase.getText().toString();
            if (passPhrase.length() == 0) {
                passPhrase = null;
            }

            data.putString(ApgService.SYMMETRIC_PASSPHRASE, passPhrase);
        } else {
            encryptionKeyIds = mEncryptionKeyIds;
            signatureKeyId = getSecretKeyId();
            signOnly = (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0);
        }

        intent.putExtra(ApgService.EXTRA_ACTION, ApgService.ACTION_ENCRYPT_SIGN);

        // choose default settings, target and data bundle by target
        if (mContentUri != null) {
            data.putInt(ApgService.TARGET, ApgService.TARGET_STREAM);
            data.putParcelable(ApgService.PROVIDER_URI, mContentUri);

        } else if (mEncryptTarget == Id.target.file) {
            useAsciiArmour = mAsciiArmour.isChecked();
            compressionId = ((Choice) mFileCompression.getSelectedItem()).getId();

            data.putInt(ApgService.TARGET, ApgService.TARGET_FILE);

            Log.d(Constants.TAG, "mInputFilename=" + mInputFilename + ", mOutputFilename="
                    + mOutputFilename);

            data.putString(ApgService.INPUT_FILE, mInputFilename);
            data.putString(ApgService.OUTPUT_FILE, mOutputFilename);

        } else {
            useAsciiArmour = true;
            compressionId = Preferences.getPreferences(this).getDefaultMessageCompression();

            data.putInt(ApgService.TARGET, ApgService.TARGET_BYTES);

            if (mData != null) {
                data.putByteArray(ApgService.MESSAGE_BYTES, mData);
            } else {
                String message = mMessage.getText().toString();
                if (signOnly && !mReturnResult) {
                    fixBadCharactersForGmail(message);
                }
                data.putByteArray(ApgService.MESSAGE_BYTES, message.getBytes());
            }
        }

        if (mOverrideAsciiArmour) {
            useAsciiArmour = mAsciiArmourDemand;
        }

        data.putLong(ApgService.SECRET_KEY_ID, getSecretKeyId());
        data.putBoolean(ApgService.USE_ASCII_AMOR, useAsciiArmour);
        data.putLongArray(ApgService.ENCRYPTION_KEYS_IDS, encryptionKeyIds);
        data.putLong(ApgService.SIGNATURE_KEY_ID, signatureKeyId);
        data.putInt(ApgService.COMPRESSION_ID, compressionId);
        data.putBoolean(ApgService.GENERATE_SIGNATURE, mGenerateSignature);
        data.putBoolean(ApgService.SIGN_ONLY, signOnly);

        intent.putExtra(ApgService.EXTRA_DATA, data);

        // Message is received after encrypting is done in ApgService
        ApgServiceHandler saveHandler = new ApgServiceHandler(this, R.string.progress_encrypting,
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgServiceHandler.MESSAGE_OKAY) {
                    // get returned data bundle
                    Bundle data = message.getData();

                    String output;
                    switch (mEncryptTarget) {
                    case Id.target.clipboard:
                        output = data.getString(ApgService.RESULT_ENCRYPTED_MESSAGE);
                        Log.d(Constants.TAG, "output: " + output);
                        Compatibility.copyToClipboard(EncryptActivity.this, output);
                        Toast.makeText(EncryptActivity.this,
                                R.string.encryptionToClipboardSuccessful, Toast.LENGTH_SHORT)
                                .show();
                        break;

                    case Id.target.email:
                        if (mReturnResult) {
                            Intent intent = new Intent();
                            intent.putExtras(data);
                            setResult(RESULT_OK, intent);
                            finish();
                            return;
                        }

                        output = data.getString(ApgService.RESULT_ENCRYPTED_MESSAGE);
                        Log.d(Constants.TAG, "output: " + output);

                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                        emailIntent.setType("text/plain; charset=utf-8");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, output);
                        if (mSubject != null) {
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, mSubject);
                        }
                        if (mSendTo != null) {
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { mSendTo });
                        }
                        startActivity(Intent.createChooser(emailIntent,
                                getString(R.string.title_sendEmail)));
                        break;

                    case Id.target.file:
                        Toast.makeText(EncryptActivity.this, R.string.encryptionSuccessful,
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
        intent.putExtra(ApgService.EXTRA_MESSENGER, messenger);

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

    private void updateView() {
        if (mEncryptionKeyIds == null || mEncryptionKeyIds.length == 0) {
            mSelectKeysButton.setText(R.string.noKeysSelected);
        } else if (mEncryptionKeyIds.length == 1) {
            mSelectKeysButton.setText(R.string.oneKeySelected);
        } else {
            mSelectKeysButton.setText("" + mEncryptionKeyIds.length + " "
                    + getResources().getString(R.string.nKeysSelected));
        }

        if (getSecretKeyId() == Id.key.none) {
            mSign.setChecked(false);
            mMainUserId.setText("");
            mMainUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknownUserId);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = PGPMain.getSecretKeyRing(getSecretKeyId());
            if (keyRing != null) {
                PGPSecretKey key = PGPHelper.getMasterKey(keyRing);
                if (key != null) {
                    String userId = PGPHelper.getMainUserIdSafe(this, key);
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
        Intent intent = new Intent(this, SelectPublicKeyListActivity.class);
        Vector<Long> keyIds = new Vector<Long>();
        if (getSecretKeyId() != 0) {
            keyIds.add(getSecretKeyId());
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
        intent.putExtra(SelectPublicKeyListActivity.RESULT_EXTRA_SELECTION, initialKeyIds);
        startActivityForResult(intent, Id.request.public_keys);
    }

    private void selectSecretKey() {
        Intent intent = new Intent(this, SelectSecretKeyListActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Id.request.filename: {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    String path = data.getData().getPath();
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
                        .getLongArray(SelectPublicKeyListActivity.RESULT_EXTRA_SELECTION);
            }
            updateView();
            break;
        }

        case Id.request.secret_keys: {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                setSecretKeyId(bundle.getLong(SelectSecretKeyListActivity.RESULT_EXTRA_KEY_ID));
            } else {
                setSecretKeyId(Id.key.none);
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
