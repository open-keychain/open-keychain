package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.devspark.appmsg.AppMsg;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.ui.dialog.FileDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import java.util.regex.Matcher;


public class DecryptMessageFragment extends Fragment {


    DecryptionFunctions decryptionfunctions;

    public interface DecryptionFunctions{
        public void guessOutputFilename(EditText v);
        public void showPassphraseDialog(View v);
        public void askForOutputFilename(View v, int code);
        public void decryptClicked(View fragment_view, int code);
        public void decryptStart(View view, int code);
        public void mSignatureLayout_OnClick();
        public void lookupUnknownKey(long id);
    }

    public View mMainView;
    private boolean mReturnBinary = false;
    private boolean mReturnResult = false;
    private LinearLayout mSignatureLayout = null;
    private ImageView mSignatureStatusImage = null;
    private TextView mUserId = null;
    private TextView mUserIdRest = null;
    private long mSecretKeyId = Id.key.none;
    private boolean mSignedOnly = false;
    private static final int RESULT_CODE_LOOKUP_KEY = 0x00007006;
    private static final int RESULT_CODE_FILE = 0x00007003;
    public static final String ACTION_DECRYPT = Constants.INTENT_PREFIX + "DECRYPT";
    private BootstrapButton mLookupKey = null;
    private BootstrapButton mDecryptButton;

    private EditText mMessage = null;
    private long mSignatureKeyId = 0;
    private final int mDecryptTarget = Id.target.message;
    private Uri mContentUri = null;
    private String mInputFilename = null;
    private String mOutputFilename = null;;
    private boolean mAssumeSymmetricEncryption = false;

    private void initView(){

        mMessage = (EditText)mMainView.findViewById(R.id.message);
        mSignatureLayout = (LinearLayout) mMainView.findViewById(R.id.signature);
        mSignatureLayout.setVisibility(View.GONE);
        mSignatureStatusImage = (ImageView) mMainView.findViewById(R.id.ic_signature_status);
        mUserId = (TextView) mMainView.findViewById(R.id.mainUserId);
        mUserIdRest = (TextView) mMainView.findViewById(R.id.mainUserIdRest);


        mLookupKey = (BootstrapButton) mMainView.findViewById(R.id.lookup_key);
        mLookupKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptionfunctions.lookupUnknownKey(mSignatureKeyId);
            }
        });

        mDecryptButton = (BootstrapButton) mMainView.findViewById(R.id.action_decrypt);
        mDecryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptionfunctions.decryptClicked(mMainView, mDecryptTarget);
            }
        });
        mSignatureLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSignatureKeyId == 0) {
                    return;
                }
                decryptionfunctions.mSignatureLayout_OnClick();
            }
        });
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mMainView = inflater.inflate(R.layout.decrypt_message_fragment, null);
        Bundle bundle = getArguments();
        if(bundle != null) {
            handleArguments(getArguments());
        }
        initView();
        checkforClipboardContent();
        return mMainView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            decryptionfunctions = (DecryptionFunctions) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement Decrypted Functions");
        }
    }


    private void handleArguments(Bundle arguments){
        String action = arguments.getString(DecryptActivity.FRAGMENT_BUNDLE_ACTION);
        Uri uri = arguments.getParcelable(DecryptActivity.FRAGMENT_BUNDLE_URI);
        String type= arguments.getString(DecryptActivity.FRAGMENT_BUNDLE_TYPE);
        String extratext = arguments.getString(DecryptActivity.FRAGMENT_BUNDLE_EXTRATEXT);
        Bundle extras = new Bundle();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // When sending to Keychain Decrypt via share menu
            if ("text/plain".equals(type)) {
                // Plain text
                String sharedText = extratext;
                if (sharedText != null) {
                    // handle like normal text decryption, override action and extras to later
                    // executeServiceMethod ACTION_DECRYPT in main actions
                    extras.putString(DecryptActivity.FRAGMENT_BUNDLE_EXTRATEXT, sharedText);
                    action = ACTION_DECRYPT;
                }
            } else {
                // Binary via content provider (could also be files)
                // override uri to get stream from send

                action = ACTION_DECRYPT;
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            // Android's Action when opening file associated to Keychain (see AndroidManifest.xml)

            // override action
            action = ACTION_DECRYPT;
        }

        String textData = extras.getString(DecryptActivity.FRAGMENT_BUNDLE_EXTRATEXT);

/**
 * Main Actions
 */
        if (ACTION_DECRYPT.equals(action) && textData != null) {
            Log.d(Constants.TAG, "textData null, matching text ...");
            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(textData);
            if (matcher.matches()) {
                Log.d(Constants.TAG, "PGP_MESSAGE matched");
                textData = matcher.group(1);
                // replace non breakable spaces
                textData = textData.replaceAll("\\xa0", " ");
                mMessage.setText(textData);
            } else {
                matcher = PgpHelper.PGP_SIGNED_MESSAGE.matcher(textData);
                if (matcher.matches()) {
                    Log.d(Constants.TAG, "PGP_SIGNED_MESSAGE matched");
                    textData = matcher.group(1);
                    // replace non breakable spaces
                    textData = textData.replaceAll("\\xa0", " ");
                    mMessage.setText(textData);
                } else {
                    Log.d(Constants.TAG, "Nothing matched!");
                }
            }
        } else if (ACTION_DECRYPT.equals(action) && uri != null) {
            // get file path from uri
        } else {
            Log.e(Constants.TAG,
                    "Include the extra 'text' or an Uri with setData() in your Intent!");
        }

    }

    private void checkforClipboardContent(){
        CharSequence clipboardText = ClipboardReflection.getClipboardText(getActivity());

        String data = "";
        if (clipboardText != null) {
            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(clipboardText);
            if (!matcher.matches()) {
                matcher = PgpHelper.PGP_SIGNED_MESSAGE.matcher(clipboardText);
            }
            if (matcher.matches()) {
                data = matcher.group(1);
                mMessage.setText(data);
                AppMsg.makeText(getActivity(), R.string.using_clipboard_content, AppMsg.STYLE_INFO)
                        .show();
            }
        }


    }


}
