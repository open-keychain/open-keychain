/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.sufficientlysecure.htmltextview.HtmlTextView;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.util.regex.Matcher;


public class WizardActivity extends ActionBarActivity {

    private State mCurrentState;

    // values for mCurrentScreen
    private enum State {
        START, CREATE_KEY, IMPORT_KEY, K9
    }

    public static final int REQUEST_CODE_IMPORT = 0x00007703;

    Button mBackButton;
    Button mNextButton;
    StartFragment mStartFragment;
    CreateKeyFragment mCreateKeyFragment;
    K9Fragment mK9Fragment;

    private static final String K9_PACKAGE = "com.fsck.k9";
    //    private static final String K9_MARKET_INTENT_URI_BASE = "market://details?id=%s";
//    private static final Intent K9_MARKET_INTENT = new Intent(Intent.ACTION_VIEW, Uri.parse(
//            String.format(K9_MARKET_INTENT_URI_BASE, K9_PACKAGE)));
    private static final Intent K9_MARKET_INTENT = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/k9mail/k-9/releases/tag/4.904"));

    LinearLayout mProgressLayout;
    View mProgressLine;
    ProgressBar mProgressBar;
    ImageView mProgressImage;
    TextView mProgressText;

    /**
     * Checks if text of given EditText is not empty. If it is empty an error is
     * set and the EditText gets the focus.
     *
     * @param context
     * @param editText
     * @return true if EditText is not empty
     */
    private static boolean isEditTextNotEmpty(Context context, EditText editText) {
        boolean output = true;
        if (editText.getText().toString().length() == 0) {
            editText.setError("empty!");
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }

    public static class StartFragment extends Fragment {
        public static StartFragment newInstance() {
            StartFragment myFragment = new StartFragment();

            Bundle args = new Bundle();
            myFragment.setArguments(args);

            return myFragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.wizard_start_fragment,
                    container, false);
        }
    }

    public static class CreateKeyFragment extends Fragment {
        public static CreateKeyFragment newInstance() {
            CreateKeyFragment myFragment = new CreateKeyFragment();

            Bundle args = new Bundle();
            myFragment.setArguments(args);

            return myFragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.wizard_create_key_fragment,
                    container, false);

            final AutoCompleteTextView emailView = (AutoCompleteTextView) view.findViewById(R.id.email);
            emailView.setThreshold(1); // Start working from first character
            emailView.setAdapter(
                    new ArrayAdapter<String>
                            (getActivity(), android.R.layout.simple_dropdown_item_1line,
                                    ContactHelper.getMailAccounts(getActivity())
                            )
            );
            emailView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    String email = editable.toString();
                    if (email.length() > 0) {
                        Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(email);
                        if (emailMatcher.matches()) {
                            emailView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                    R.drawable.uid_mail_ok, 0);
                        } else {
                            emailView.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                    R.drawable.uid_mail_bad, 0);
                        }
                    } else {
                        // remove drawable if email is empty
                        emailView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    }
                }
            });
            return view;
        }
    }

    public static class K9Fragment extends Fragment {
        public static K9Fragment newInstance() {
            K9Fragment myFragment = new K9Fragment();

            Bundle args = new Bundle();
            myFragment.setArguments(args);

            return myFragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.wizard_k9_fragment,
                    container, false);

            HtmlTextView text = (HtmlTextView) v
                    .findViewById(R.id.wizard_k9_text);
            text.setHtmlFromString("Install K9. It's good for you! Here is a screenhot how to enable OK in K9: (TODO)", true);

            return v;
        }

    }

    /**
     * Loads new fragment
     *
     * @param fragment
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager
                .beginTransaction();
        fragmentTransaction.replace(R.id.wizard_container,
                fragment);
        fragmentTransaction.commit();
    }

    /**
     * Instantiate View and initialize fragments for this Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.wizard_activity);
        mBackButton = (Button) findViewById(R.id.wizard_back);
        mNextButton = (Button) findViewById(R.id.wizard_next);

        // progress layout
        mProgressLayout = (LinearLayout) findViewById(R.id.wizard_progress);
        mProgressLine = findViewById(R.id.wizard_progress_line);
        mProgressBar = (ProgressBar) findViewById(R.id.wizard_progress_progressbar);
        mProgressImage = (ImageView) findViewById(R.id.wizard_progress_image);
        mProgressText = (TextView) findViewById(R.id.wizard_progress_text);

        changeToState(State.START);
    }

    private enum ProgressState {
        WORKING, ENABLED, DISABLED, ERROR
    }

    private void showProgress(ProgressState state, String text) {
        switch (state) {
            case WORKING:
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressImage.setVisibility(View.GONE);
                break;
            case ENABLED:
                mProgressBar.setVisibility(View.GONE);
                mProgressImage.setVisibility(View.VISIBLE);
//			mProgressImage.setImageDrawable(getResources().getDrawable(
//					R.drawable.status_enabled));
                break;
            case DISABLED:
                mProgressBar.setVisibility(View.GONE);
                mProgressImage.setVisibility(View.VISIBLE);
//			mProgressImage.setImageDrawable(getResources().getDrawable(
//					R.drawable.status_disabled));
                break;
            case ERROR:
                mProgressBar.setVisibility(View.GONE);
                mProgressImage.setVisibility(View.VISIBLE);
//			mProgressImage.setImageDrawable(getResources().getDrawable(
//					R.drawable.status_fail));
                break;

            default:
                break;
        }
        mProgressText.setText(text);

        mProgressLine.setVisibility(View.VISIBLE);
        mProgressLayout.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mProgressLine.setVisibility(View.GONE);
        mProgressLayout.setVisibility(View.GONE);
    }

    public void nextOnClick(View view) {
        // close keyboard
        if (getCurrentFocus() != null) {
            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getCurrentFocus()
                    .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        switch (mCurrentState) {
            case START: {
                RadioGroup radioGroup = (RadioGroup) findViewById(R.id.wizard_start_radio_group);
                int selectedId = radioGroup.getCheckedRadioButtonId();
                switch (selectedId) {
                    case R.id.wizard_start_new_key: {
                        changeToState(State.CREATE_KEY);
                        break;
                    }
                    case R.id.wizard_start_import: {
                        changeToState(State.IMPORT_KEY);
                        break;
                    }
                    case R.id.wizard_start_skip: {
                        finish();
                        break;
                    }
                }

                mBackButton.setText(R.string.btn_back);
                break;
            }
            case CREATE_KEY:
                EditText nameEdit = (EditText) findViewById(R.id.name);
                EditText emailEdit = (EditText) findViewById(R.id.email);
                EditText passphraseEdit = (EditText) findViewById(R.id.passphrase);

                if (isEditTextNotEmpty(this, nameEdit)
                        && isEditTextNotEmpty(this, emailEdit)
                        && isEditTextNotEmpty(this, passphraseEdit)) {

//                    SaveKeyringParcel newKey = new SaveKeyringParcel();
//                    newKey.addUserIds.add(nameEdit.getText().toString() + " <"
//                            + emailEdit.getText().toString() + ">");


                    AsyncTask<String, Boolean, Boolean> generateTask = new AsyncTask<String, Boolean, Boolean>() {

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();

                            showProgress(ProgressState.WORKING, "generating key...");
                        }

                        @Override
                        protected Boolean doInBackground(String... params) {
                            return true;
                        }

                        @Override
                        protected void onPostExecute(Boolean result) {
                            super.onPostExecute(result);

                            if (result) {
                                showProgress(ProgressState.ENABLED, "key generated successfully!");

                                changeToState(State.K9);
                            } else {
                                showProgress(ProgressState.ERROR, "error in key gen");
                            }
                        }

                    };

                    generateTask.execute("");
                }
                break;
            case K9: {
                RadioGroup radioGroup = (RadioGroup) findViewById(R.id.wizard_k9_radio_group);
                int selectedId = radioGroup.getCheckedRadioButtonId();
                switch (selectedId) {
                    case R.id.wizard_k9_install: {
                        try {
                            startActivity(K9_MARKET_INTENT);
                        } catch (ActivityNotFoundException e) {
                            Log.e(Constants.TAG, "Activity not found for: " + K9_MARKET_INTENT);
                        }
                        break;
                    }
                    case R.id.wizard_k9_skip: {
                        finish();
                        break;
                    }
                }

                finish();
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_IMPORT: {
                if (resultCode == Activity.RESULT_OK) {
                    // imported now...
                    changeToState(State.K9);
                } else {
                    // back to start
                    changeToState(State.START);
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    public void backOnClick(View view) {
        switch (mCurrentState) {
            case START:
                finish();
                break;
            case CREATE_KEY:
                changeToState(State.START);
                break;
            case IMPORT_KEY:
                changeToState(State.START);
                break;
            default:
                changeToState(State.START);
                break;
        }
    }

    private void changeToState(State state) {
        switch (state) {
            case START: {
                mCurrentState = State.START;
                mStartFragment = StartFragment.newInstance();
                loadFragment(mStartFragment);
                mBackButton.setText(android.R.string.cancel);
                mNextButton.setText(R.string.btn_next);
                break;
            }
            case CREATE_KEY: {
                mCurrentState = State.CREATE_KEY;
                mCreateKeyFragment = CreateKeyFragment.newInstance();
                loadFragment(mCreateKeyFragment);
                break;
            }
            case IMPORT_KEY: {
                mCurrentState = State.IMPORT_KEY;
                Intent intent = new Intent(this, ImportKeysActivity.class);
                intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
                startActivityForResult(intent, REQUEST_CODE_IMPORT);
                break;
            }
            case K9: {
                mCurrentState = State.K9;
                mBackButton.setEnabled(false); // don't go back to import/create key
                mK9Fragment = K9Fragment.newInstance();
                loadFragment(mK9Fragment);
                break;
            }
        }
    }

}
