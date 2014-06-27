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

import android.content.Context;
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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;

import java.util.regex.Matcher;


public class WizardActivity extends ActionBarActivity {

    private State mCurrentState;

    // values for mCurrentScreen
    private enum State {
        START, CREATE_KEY, NFC, FINISH
    }

    Button mBackButton;
    Button mNextButton;
    StartFragment mStartFragment;
    CreateKeyFragment mCreateKeyFragment;
    GenericFragment mNFCFragment;
    GenericFragment mFinishFragment;

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
            View view =  inflater.inflate(R.layout.wizard_create_key_fragment,
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
//                    if (mEditorListener != null) {
//                        mEditorListener.onEdited();
//                    }
                }
            });
            return view;
        }
    }

    public static class GenericFragment extends Fragment {
        public static GenericFragment newInstance(String text) {
            GenericFragment myFragment = new GenericFragment();

            Bundle args = new Bundle();
            args.putString("text", text);
            myFragment.setArguments(args);

            return myFragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.wizard_generic_fragment,
                    container, false);

            TextView text = (TextView) v
                    .findViewById(R.id.fragment_vehicle_reg_generic_text);
            text.setText(getArguments().getString("text"));

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

        mStartFragment = StartFragment.newInstance();
        loadFragment(mStartFragment);
        mCurrentState = State.START;
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
                        mCurrentState = State.CREATE_KEY;
                        mCreateKeyFragment = CreateKeyFragment.newInstance();
                        loadFragment(mCreateKeyFragment);
                        break;
                    }
                }

                mBackButton.setText(R.string.btn_back);

//			if (isEditTextNotEmpty(this, asd)) {
//				mLicensePlate = asd.getText().toString();
//
//				showProgress(ProgressState.WORKING,
//						"doing something";
//			}
                break;
            }
            case CREATE_KEY:

                AsyncTask<String, Boolean, Boolean> generateTask = new AsyncTask<String, Boolean, Boolean>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();

                        showProgress(ProgressState.WORKING, "generating key...");
                    }

                    @Override
                    protected Boolean doInBackground(String... params) {
                        return false;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);

//					if (result) {
//						showProgress(
//								ProgressState.WORKING,
//								"asd");
//
//					} else {
//						showProgress(
//								ProgressState.ERROR, "asd");
//					}
                    }

                };

                generateTask.execute("");

                break;
            case NFC:

                mCurrentState = State.FINISH;
                hideProgress();
                mFinishFragment = GenericFragment
                        .newInstance("asd");
                loadFragment(mFinishFragment);
                mNextButton.setText("finish");

                break;
            case FINISH:
                finish();
                break;
            default:
                break;
        }
    }

    public void backOnClick(View view) {
        switch (mCurrentState) {
            case START:
                finish();
                break;

            case CREATE_KEY:
                loadFragment(mStartFragment);
                mCurrentState = State.START;
                mBackButton.setText(android.R.string.cancel);
                mNextButton.setText(R.string.btn_next);
                break;
            case NFC:
                loadFragment(mCreateKeyFragment);
                mCurrentState = State.CREATE_KEY;
                mBackButton.setText(R.string.btn_back);
                mNextButton.setText(R.string.btn_next);
                break;
            case FINISH:
                loadFragment(mNFCFragment);
                mCurrentState = State.NFC;
                mBackButton.setText(R.string.btn_back);
                mNextButton.setText(R.string.btn_next);
                break;

            default:
                loadFragment(mStartFragment);
                mCurrentState = State.START;
                mBackButton.setText(android.R.string.cancel);
                mNextButton.setText(R.string.btn_next);
                break;
        }
    }

}
