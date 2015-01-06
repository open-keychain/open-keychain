/*
 *   Copyright 2012 Hai Bison
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use getActivity() file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.haibison.android.lockpattern;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.haibison.android.lockpattern.util.IEncrypter;
import com.haibison.android.lockpattern.util.InvalidEncrypterException;
import com.haibison.android.lockpattern.util.LoadingDialog;
import com.haibison.android.lockpattern.util.Settings;
import com.haibison.android.lockpattern.util.UI;
import com.haibison.android.lockpattern.widget.LockPatternUtils;
import com.haibison.android.lockpattern.widget.LockPatternView;
import com.haibison.android.lockpattern.widget.LockPatternView.Cell;
import com.haibison.android.lockpattern.widget.LockPatternView.DisplayMode;

import org.sufficientlysecure.keychain.ui.PassphraseWizardActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.haibison.android.lockpattern.util.Settings.Display.METADATA_CAPTCHA_WIRED_DOTS;
import static com.haibison.android.lockpattern.util.Settings.Display.METADATA_MAX_RETRIES;
import static com.haibison.android.lockpattern.util.Settings.Display.METADATA_MIN_WIRED_DOTS;
import static com.haibison.android.lockpattern.util.Settings.Display.METADATA_STEALTH_MODE;
import static com.haibison.android.lockpattern.util.Settings.Security.METADATA_AUTO_SAVE_PATTERN;
import static com.haibison.android.lockpattern.util.Settings.Security.METADATA_ENCRYPTER_CLASS;

/**
 * Main activity for getActivity() library.
 * <p>
 * You can deliver result to {@link android.app.PendingIntent}'s and/ or
 * {@link android.os.ResultReceiver} too. See {@link #EXTRA_PENDING_INTENT_OK},
 * {@link #EXTRA_PENDING_INTENT_CANCELLED} and {@link #EXTRA_RESULT_RECEIVER}
 * for more details.
 * </p>
 *
 * <h1>NOTES</h1>
 * <ul>
 * <li>
 * You must use one of built-in actions when calling getActivity() activity. They start
 * with {@code ACTION_*}. Otherwise the library might behave strangely (we don't
 * cover those cases).</li>
 * <li>You must use one of the themes that getActivity() library supports. They start
 * with {@code R.style.Alp_42447968_Theme_*}. The reason is the themes contain
 * resources that the library needs.</li>
 * <li>With {@link #ACTION_COMPARE_PATTERN}, there are <b><i>4 possible result
 * codes</i></b>: {@link android.app.Activity#RESULT_OK}, {@link android.app.Activity#RESULT_CANCELED},
 * {@link #RESULT_FAILED} and {@link #RESULT_FORGOT_PATTERN}.</li>
 * <li>With {@link #ACTION_VERIFY_CAPTCHA}, there are <b><i>3 possible result
 * codes</i></b>: {@link android.app.Activity#RESULT_OK}, {@link android.app.Activity#RESULT_CANCELED},
 * and {@link #RESULT_FAILED}.</li>
 * </ul>
 *
 * @author Hai Bison
 * @since v1.0
 */
public class LockPatternFragmentOld extends Fragment {

    private static final String CLASSNAME = LockPatternFragmentOld.class.getName();

    public static final String ACTION_CREATE_PATTERN = "create";

    /**
     * Use getSelectedMethod() to compare pattern. You provide the pattern to be
     * compared with {@link #EXTRA_PATTERN}.
     * <p/>
     * If you enabled feature auto-save pattern before (with
     * {@link com.haibison.android.lockpattern.util.Settings.Security#setAutoSavePattern(android.content.Context, boolean)} ),
     * then you don't need {@link #EXTRA_PATTERN} at getActivity() time.
     * <p/>
     * You can use {@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN} to help your
     * users in case they forgot the patterns.
     * <p/>
     * If the user passes, {@link android.app.Activity#RESULT_OK} returns. If not,
     * {@link #RESULT_FAILED} returns.
     * <p/>
     * If the user cancels the task, {@link android.app.Activity#RESULT_CANCELED} returns.
     * <p/>
     * In any case, there will have extra {@link #EXTRA_RETRY_COUNT} available
     * in the intent result.
     *
     * @see #EXTRA_PATTERN
     * @see #EXTRA_PENDING_INTENT_OK
     * @see #EXTRA_PENDING_INTENT_CANCELLED
     * @see #RESULT_FAILED
     * @see #EXTRA_RETRY_COUNT
     * @since v2.4 beta
     */
    public static final String ACTION_COMPARE_PATTERN = "authenticate";//CLASSNAME + ".compare_pattern";

    /**
     * Use getActivity() action to let the activity generate a random pattern and ask the
     * user to re-draw it to verify.
     * <p/>
     * The default length of the auto-generated pattern is {@code 4}. You can
     * change it with
     * {@link com.haibison.android.lockpattern.util.Settings.Display#setCaptchaWiredDots(android.content.Context, int)}.
     *
     * @since v2.7 beta
     */
    public static final String ACTION_VERIFY_CAPTCHA = CLASSNAME + ".verify_captcha";

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user fails to "login"
     * after a number of tries, getActivity() activity will finish with getActivity() result code.
     *
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     */
    public final int RESULT_FAILED = Activity.RESULT_FIRST_USER + 1;

    /**
     * If you use {@link #ACTION_COMPARE_PATTERN} and the user forgot his/ her
     * pattern and decided to ask for your help with recovering the pattern (
     * {@link #EXTRA_PENDING_INTENT_FORGOT_PATTERN}), getActivity() activity will finish
     * with getActivity() result code.
     *
     * @see #ACTION_COMPARE_PATTERN
     * @see #EXTRA_RETRY_COUNT
     * @see #EXTRA_PENDING_INTENT_FORGOT_PATTERN
     * @since v2.8 beta
     */
    public static final int RESULT_FORGOT_PATTERN = Activity.RESULT_FIRST_USER + 2;

    /**
     * For actions {@link #ACTION_COMPARE_PATTERN} and
     * {@link #ACTION_VERIFY_CAPTCHA}, getActivity() key holds the number of tries that
     * the user attempted to verify the input pattern.
     */
    public static final String EXTRA_RETRY_COUNT = CLASSNAME + ".retry_count";

    /**
     * Sets value of getActivity() key to a theme in {@code R.style.Alp_42447968_Theme_*}
     * . Default is the one you set in your {@code AndroidManifest.xml}. Note
     * that theme {@link R.style#Alp_42447968_Theme_Light_DarkActionBar} is
     * available in API 4+, but it only works in API 14+.
     *
     * @since v1.5.3 beta
     */
    public static final String EXTRA_THEME = CLASSNAME + ".theme";

    /**
     * Key to hold the pattern. It must be a {@code char[]} array.
     * <p/>
     * <ul>
     * <li>If you use encrypter, it should be an encrypted array.</li>
     * <li>If you don't use encrypter, it should be the SHA-1 value of the
     * actual pattern. You can generate the value by
     * {@link com.haibison.android.lockpattern.widget.LockPatternUtils#patternToSha1(java.util.List)}.</li>
     * </ul>
     *
     * @since v2 beta
     */
    public static final String EXTRA_PATTERN = CLASSNAME + ".pattern";

    /**
     * You can provide an {@link android.os.ResultReceiver} with getActivity() key. The activity
     * will notify your receiver the same result code and intent data as you
     * will receive them in {@link #onActivityResult(int, int, android.content.Intent)}.
     *
     * @since v2.4 beta
     */
    public static final String EXTRA_RESULT_RECEIVER = CLASSNAME
            + ".result_receiver";

    /**
     * Put a {@link android.app.PendingIntent} into getActivity() key. It will be sent before
     * {@link android.app.Activity#RESULT_OK} will be returning. If you were calling getActivity()
     * activity with {@link #ACTION_CREATE_PATTERN}, key {@link #EXTRA_PATTERN}
     * will be attached to the original intent which the pending intent holds.
     *
     * <h1>Notes</h1>
     * <ul>
     * <li>If you're going to use an activity, you don't need
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link LockPatternFragmentOld} .</li>
     * </ul>
     */
    public static final String EXTRA_PENDING_INTENT_OK = CLASSNAME
            + ".pending_intent_ok";

    /**
     * Put a {@link android.app.PendingIntent} into getActivity() key. It will be sent before
     * {@link android.app.Activity#RESULT_CANCELED} will be returning.
     *
     * <h1>Notes</h1>
     * <ul>
     * <li>If you're going to use an activity, you don't need
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link LockPatternFragmentOld} .</li>
     * </ul>
     */
    public static final String EXTRA_PENDING_INTENT_CANCELLED = CLASSNAME
            + ".pending_intent_cancelled";

    /**
     * You put a {@link android.app.PendingIntent} into getActivity() extra. The library will show a
     * button <i>"Forgot pattern?"</i> and call your intent later when the user
     * taps it.
     * <p/>
     * <h1>Notes</h1>
     * <ul>
     * <li>If you use an activity, you don't need
     * {@link android.content.Intent#FLAG_ACTIVITY_NEW_TASK} for the intent, since the library
     * will call it inside {@link LockPatternFragmentOld} .</li>
     * <li>{@link LockPatternFragmentOld} will finish with
     * {@link #RESULT_FORGOT_PATTERN} <i><b>after</b> making a call</i> to start
     * your pending intent.</li>
     * <li>It is your responsibility to make sure the Intent is good. The
     * library doesn't cover any errors when calling your intent.</li>
     * </ul>
     *
     * @see #ACTION_COMPARE_PATTERN
     * @since v2.8 beta
     */
    public static final String EXTRA_PENDING_INTENT_FORGOT_PATTERN = CLASSNAME
            + ".pending_intent_forgot_pattern";

    /**
     * Helper enum for button OK commands. (Because we use only one "OK" button
     * for different commands).
     *
     * @author Hai Bison
     */
    private static enum ButtonOkCommand {
        CONTINUE,DONE
    }// ButtonOkCommand

    /**
     * Delay time to reload the lock pattern view after a wrong pattern.
     */
    private static final long DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW = DateUtils.SECOND_IN_MILLIS;

    /*
     * FIELDS
     */
    private int mMaxRetries, mMinWiredDots, mRetryCount = 0, mCaptchaWiredDots;
    private boolean mAutoSave, mStealthMode;
    private IEncrypter mEncrypter;
    private ButtonOkCommand mBtnOkCmd;
    private Intent mIntentResult;

    /*
     * CONTROLS
     */
    private TextView mTextInfo;
    private LockPatternView mLockPatternView;
    private Button mBtnConfirm;

    /*
     * FRAGMENTS
     */
    private FragmentActivity fa;

    /**
     * Called when the activity is first created.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        fa = getActivity();

        /*
         * EXTRA_THEME
         */
        if (fa.getIntent().hasExtra(EXTRA_THEME))
            fa.setTheme(fa.getIntent().getIntExtra(EXTRA_THEME,
                    R.style.Alp_42447968_Theme_Dark));
        View view = inflater.inflate(R.layout.alp_42447968_lock_pattern_activity, container, false);
        loadSettings();

        mIntentResult = new Intent();
        fa.setResult(Activity.RESULT_CANCELED, mIntentResult);
        initContentView(view);
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Loads settings, either from manifest or {@link com.haibison.android.lockpattern.util.Settings}.
     */
    private void loadSettings() {
        Bundle metaData = null;
        try {
            metaData = fa.getPackageManager().getActivityInfo(fa.getComponentName(),
                    PackageManager.GET_META_DATA).metaData;
        } catch (NameNotFoundException e) {
            /*
             * Never catch getActivity().
             */
            e.printStackTrace();
        }

        if (metaData != null && metaData.containsKey(METADATA_MIN_WIRED_DOTS))
            mMinWiredDots = Settings.Display.validateMinWiredDots(getActivity(),
                    metaData.getInt(METADATA_MIN_WIRED_DOTS));
        else
            mMinWiredDots = Settings.Display.getMinWiredDots(getActivity());

        if (metaData != null && metaData.containsKey(METADATA_MAX_RETRIES))
            mMaxRetries = Settings.Display.validateMaxRetries(getActivity(),
                    metaData.getInt(METADATA_MAX_RETRIES));
        else
            mMaxRetries = Settings.Display.getMaxRetries(getActivity());

        if (metaData != null
                && metaData.containsKey(METADATA_AUTO_SAVE_PATTERN))
            mAutoSave = metaData.getBoolean(METADATA_AUTO_SAVE_PATTERN);
        else
            mAutoSave = Settings.Security.isAutoSavePattern(getActivity());

        if (metaData != null
                && metaData.containsKey(METADATA_CAPTCHA_WIRED_DOTS))
            mCaptchaWiredDots = Settings.Display.validateCaptchaWiredDots(getActivity(),
                    metaData.getInt(METADATA_CAPTCHA_WIRED_DOTS));
        else
            mCaptchaWiredDots = Settings.Display.getCaptchaWiredDots(getActivity());

        if (metaData != null && metaData.containsKey(METADATA_STEALTH_MODE))
            mStealthMode = metaData.getBoolean(METADATA_STEALTH_MODE);
        else
            mStealthMode = Settings.Display.isStealthMode(getActivity());

        /*
         * Encrypter.
         */
        char[] encrypterClass;
        if (metaData != null && metaData.containsKey(METADATA_ENCRYPTER_CLASS))
            encrypterClass = metaData.getString(METADATA_ENCRYPTER_CLASS)
                    .toCharArray();
        else
            encrypterClass = Settings.Security.getEncrypterClass(getActivity());

        if (encrypterClass != null) {
            try {
                mEncrypter = (IEncrypter) Class.forName(
                        new String(encrypterClass), false, fa.getClassLoader())
                        .newInstance();
            } catch (Throwable t) {
                throw new InvalidEncrypterException();
            }
        }
    }

    /**
     * Initializes UI...
     */
    private void initContentView(View view) {

        /*
         * Save all controls' state to restore later.
         */
        CharSequence infoText = mTextInfo != null ? mTextInfo.getText() : null;
        Boolean btnOkEnabled = mBtnConfirm != null ? mBtnConfirm.isEnabled()
                : null;
        DisplayMode lastDisplayMode = mLockPatternView != null ? mLockPatternView
                .getDisplayMode() : null;
        List<Cell> lastPattern = mLockPatternView != null ? mLockPatternView
                .getPattern() : null;

        UI.adjustDialogSizeForLargeScreens(fa.getWindow());

        View mFooter;
        Button mBtnCancel;

        mTextInfo = (TextView) view.findViewById(R.id.alp_42447968_textview_info);
        mLockPatternView = (LockPatternView) view.findViewById(R.id.alp_42447968_view_lock_pattern);

        mFooter = view.findViewById(R.id.alp_42447968_viewgroup_footer);
        mBtnCancel = (Button) view.findViewById(R.id.alp_42447968_button_cancel);
        mBtnConfirm = (Button) view.findViewById(R.id.alp_42447968_button_confirm);

        /*
         * LOCK PATTERN VIEW
         */

        switch (getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: {
                final int size = getResources().getDimensionPixelSize(
                        R.dimen.alp_42447968_lockpatternview_size);
                LayoutParams lp = mLockPatternView.getLayoutParams();
                lp.width = size;
                lp.height = size;
                mLockPatternView.setLayoutParams(lp);

                break;
            }
        }

        /*
         * Haptic feedback.
         */
        boolean hapticFeedbackEnabled = false;
        try {
            hapticFeedbackEnabled = android.provider.Settings.System
                    .getInt(fa.getContentResolver(),
                            android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED,
                            0) != 0;
        } catch (Throwable t) {
            /*
             * Ignore it.
             */
        }
        mLockPatternView.setTactileFeedbackEnabled(hapticFeedbackEnabled);

        mLockPatternView.setInStealthMode(mStealthMode
                && !ACTION_VERIFY_CAPTCHA.equals(fa.getIntent().getAction()));
        mLockPatternView.setOnPatternListener(mLockPatternViewListener);
        if (lastPattern != null && lastDisplayMode != null
                && !ACTION_VERIFY_CAPTCHA.equals(fa.getIntent().getAction()))
            mLockPatternView.setPattern(lastDisplayMode, lastPattern);
        /*
         * COMMAND BUTTONS
         */

        if (ACTION_CREATE_PATTERN.equals(getSelectedMethod())) {
            mBtnCancel.setOnClickListener(mBtnCancelOnClickListener);
            mBtnConfirm.setOnClickListener(mBtnConfirmOnClickListener);

            mBtnCancel.setVisibility(View.VISIBLE);
            mFooter.setVisibility(View.VISIBLE);
            mTextInfo.setVisibility(View.VISIBLE);
            if (infoText != null)
                mTextInfo.setText(infoText);
            else
                mTextInfo  //TODO nfc text glaube ich hier oder so
                        .setText(R.string.alp_42447968_msg_draw_an_unlock_pattern);

            /*
             * BUTTON OK
             */
            if (mBtnOkCmd == null)
                mBtnOkCmd = ButtonOkCommand.CONTINUE;
            switch (mBtnOkCmd) {
                case CONTINUE:
                    mBtnConfirm.setText(R.string.alp_42447968_cmd_continue);
                    break;
                case DONE:
                    mBtnConfirm.setText(R.string.alp_42447968_cmd_confirm);
                    break;
                default:
                    break;
            }
            if (btnOkEnabled != null)
                mBtnConfirm.setEnabled(btnOkEnabled);
        }
        else if (ACTION_COMPARE_PATTERN.equals(getSelectedMethod())) {
            if (TextUtils.isEmpty(infoText))
                mTextInfo
                        .setText(R.string.alp_42447968_msg_draw_pattern_to_unlock);
            else
                mTextInfo.setText(infoText);
            if (fa.getIntent().hasExtra(EXTRA_PENDING_INTENT_FORGOT_PATTERN)) {
                mBtnConfirm.setOnClickListener(mBtnConfirmOnClickListener);
                mBtnConfirm.setText(R.string.alp_42447968_cmd_forgot_pattern);
                mBtnConfirm.setEnabled(true);
                mFooter.setVisibility(View.VISIBLE);
            }
        }
        else if (ACTION_VERIFY_CAPTCHA.equals(fa.getIntent().getAction())) {
            mTextInfo
                    .setText(R.string.alp_42447968_msg_redraw_pattern_to_confirm);

            /*
             * NOTE: EXTRA_PATTERN should hold a char[] array. In getActivity() case we
             * use it as a temporary variable to hold a list of Cell.
             */

            final ArrayList<Cell> pattern;
            if (fa.getIntent().hasExtra(EXTRA_PATTERN))
                pattern = fa.getIntent()
                        .getParcelableArrayListExtra(EXTRA_PATTERN);
            else
                fa.getIntent().putParcelableArrayListExtra(
                        EXTRA_PATTERN,
                        pattern = LockPatternUtils
                                .genCaptchaPattern(mCaptchaWiredDots));

            mLockPatternView.setPattern(DisplayMode.Animate, pattern);
        }
    }

    /**
     * Compares {@code pattern} to the given pattern (
     * {@link #ACTION_COMPARE_PATTERN}) or to the generated "CAPTCHA" pattern (
     * {@link #ACTION_VERIFY_CAPTCHA}). Then finishes the activity if they
     * match.
     *
     * @param pattern
     *            the pattern to be compared.
     */
    private void doComparePattern(final List<Cell> pattern) {
        if (pattern == null)
            return;

        /*
         * Use a LoadingDialog because decrypting pattern might take time...
         */
        new LoadingDialog<Void, Void, Boolean>(getActivity(), false) {

            @Override
            protected Boolean doInBackground(Void... params) {
                if (ACTION_COMPARE_PATTERN.equals(getSelectedMethod())) {
                    char[] currentPattern = PassphraseWizardActivity.pattern;
                    if (currentPattern == null)
                        currentPattern = Settings.Security
                                .getPattern(getActivity());
                    if (currentPattern != null) {
                        if (mEncrypter != null) {
                            return pattern.equals(mEncrypter.decrypt(
                                    getActivity(), currentPattern));
                        } else
                            return Arrays.equals(currentPattern,
                                    LockPatternUtils.patternToSha1(pattern)
                                            .toCharArray());
                    }
                }
                else if (ACTION_VERIFY_CAPTCHA.equals(fa.getIntent().getAction())) {
                    return pattern.equals(fa.getIntent()
                            .getParcelableArrayListExtra(EXTRA_PATTERN));
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result) {
                    Toast.makeText(getActivity(), "unlocked", Toast.LENGTH_SHORT).show();
                    finishWithResultOk(null);
                }else {
                    mRetryCount++;
                    mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

                    if (mRetryCount >= mMaxRetries)
                        finishWithNegativeResult(RESULT_FAILED);
                    else {
                        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                        mTextInfo.setText(R.string.alp_42447968_msg_try_again);
                        mLockPatternView.postDelayed(mLockPatternViewReloader,
                                DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }
            }

        }.execute();
    }

    /**
     * Checks and creates the pattern.
     *
     * @param pattern
     *            the current pattern of lock pattern view.
     */
    private void doCheckAndCreatePattern(final List<Cell> pattern) {
        if (pattern.size() < mMinWiredDots) {
            mLockPatternView.setDisplayMode(DisplayMode.Wrong);
            mTextInfo.setText(getResources().getQuantityString(
                    R.plurals.alp_42447968_pmsg_connect_x_dots, mMinWiredDots,
                    mMinWiredDots));
            mLockPatternView.postDelayed(mLockPatternViewReloader,
                    DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
            return;
        }

        if (fa.getIntent().hasExtra(EXTRA_PATTERN)) {
            /*
             * Use a LoadingDialog because decrypting pattern might take time...
             */
            new LoadingDialog<Void, Void, Boolean>(getActivity(), false) {

                @Override
                protected Boolean doInBackground(Void... params) {
                    if (mEncrypter != null)
                        return pattern.equals(mEncrypter.decrypt(
                                getActivity(), fa.getIntent()
                                        .getCharArrayExtra(EXTRA_PATTERN)));
                    else
                        return Arrays.equals(
                                fa.getIntent().getCharArrayExtra(EXTRA_PATTERN),
                                LockPatternUtils.patternToSha1(pattern)
                                        .toCharArray());
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    super.onPostExecute(result);

                    if (result) {
                        mTextInfo
                                .setText(R.string.alp_42447968_msg_your_new_unlock_pattern);
                        mBtnConfirm.setEnabled(true);
                        PassphraseWizardActivity.pattern = fa.getIntent()
                                .getCharArrayExtra(EXTRA_PATTERN);
                    } else {
                        mTextInfo
                                .setText(R.string.alp_42447968_msg_redraw_pattern_to_confirm);
                        mBtnConfirm.setEnabled(false);
                        mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                        mLockPatternView.postDelayed(mLockPatternViewReloader,
                                DELAY_TIME_TO_RELOAD_LOCK_PATTERN_VIEW);
                    }
                }
            }.execute();
        } else {
            /*
             * Use a LoadingDialog because encrypting pattern might take time...
             */
            new LoadingDialog<Void, Void, char[]>(getActivity(), false) {

                @Override
                protected char[] doInBackground(Void... params) {
                    return mEncrypter != null ? mEncrypter.encrypt(
                            getActivity(), pattern)
                            : LockPatternUtils.patternToSha1(pattern)
                            .toCharArray();
                }

                @Override
                protected void onPostExecute(char[] result) {
                    super.onPostExecute(result);

                    fa.getIntent().putExtra(EXTRA_PATTERN, result);
                    mTextInfo
                            .setText(R.string.alp_42447968_msg_pattern_recorded);
                    mBtnConfirm.setEnabled(true);
                }

            }.execute();
        }
    }

    /**
     * Finishes activity with {@link android.app.Activity#RESULT_OK}.
     *
     * @param pattern
     *            the pattern, if getActivity() is in mode creating pattern. In any
     *            cases, it can be set to {@code null}.
     */
    private void finishWithResultOk(char[] pattern) {
        if (ACTION_CREATE_PATTERN.equals(getSelectedMethod()))
            mIntentResult.putExtra(EXTRA_PATTERN, pattern);
        else {
            /*
             * If the user was "logging in", minimum try count can not be zero.
             */
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount + 1);
        }

        fa.setResult(fa.RESULT_OK, mIntentResult);

        /*
         * ResultReceiver
         */
        ResultReceiver receiver = fa.getIntent().getParcelableExtra(
                EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle bundle = new Bundle();
            if (ACTION_CREATE_PATTERN.equals(getSelectedMethod()))
                bundle.putCharArray(EXTRA_PATTERN, pattern);
            else {
                /*
                 * If the user was "logging in", minimum try count can not be
                 * zero.
                 */
                bundle.putInt(EXTRA_RETRY_COUNT, mRetryCount + 1);
            }
            receiver.send(fa.RESULT_OK, bundle);
        }

        /*
         * PendingIntent
         */
        PendingIntent pi = fa.getIntent().getParcelableExtra(
                EXTRA_PENDING_INTENT_OK);
        if (pi != null) {
            try {
                pi.send(getActivity(), fa.RESULT_OK, mIntentResult);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        fa.finish();
    }

    /**
     * Finishes the activity with negative result (
     * {@link android.app.Activity#RESULT_CANCELED}, {@link #RESULT_FAILED} or
     * {@link #RESULT_FORGOT_PATTERN}).
     */
    private void finishWithNegativeResult(int resultCode) {
        if (ACTION_COMPARE_PATTERN.equals(getSelectedMethod()))
            mIntentResult.putExtra(EXTRA_RETRY_COUNT, mRetryCount);

        fa.setResult(resultCode, mIntentResult);

        /*
         * ResultReceiver
         */
        ResultReceiver receiver = fa.getIntent().getParcelableExtra(
                EXTRA_RESULT_RECEIVER);
        if (receiver != null) {
            Bundle resultBundle = null;
            if (ACTION_COMPARE_PATTERN.equals(getSelectedMethod())) {
                resultBundle = new Bundle();
                resultBundle.putInt(EXTRA_RETRY_COUNT, mRetryCount);
            }
            receiver.send(resultCode, resultBundle);
        }

        /*
         * PendingIntent
         */
        PendingIntent pi = fa.getIntent().getParcelableExtra(
                EXTRA_PENDING_INTENT_CANCELLED);
        if (pi != null) {
            try {
                pi.send(getActivity(), resultCode, mIntentResult);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        fa.finish();
    }

    /*
     * LISTENERS
     */

    private final LockPatternView.OnPatternListener mLockPatternViewListener = new LockPatternView.OnPatternListener() {

        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);
            mLockPatternView.setDisplayMode(DisplayMode.Correct);

            if (ACTION_CREATE_PATTERN.equals(getSelectedMethod())) {
                mTextInfo
                        .setText(R.string.alp_42447968_msg_release_finger_when_done);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE)
                    fa.getIntent().removeExtra(EXTRA_PATTERN);
            }
            else if (ACTION_COMPARE_PATTERN.equals(getSelectedMethod())) {
                mTextInfo
                        .setText(R.string.alp_42447968_msg_draw_pattern_to_unlock);
            }
            else if (ACTION_VERIFY_CAPTCHA.equals(getSelectedMethod())) {
                mTextInfo
                        .setText(R.string.alp_42447968_msg_redraw_pattern_to_confirm);
            }
        }

        @Override
        public void onPatternDetected(List<Cell> pattern) {
            if (ACTION_CREATE_PATTERN.equals(getSelectedMethod())) {
                doCheckAndCreatePattern(pattern);
            }
            else if (ACTION_COMPARE_PATTERN.equals(getSelectedMethod())) {
                doComparePattern(pattern);
            }
            else if (ACTION_VERIFY_CAPTCHA.equals(getSelectedMethod())) {
                if (!DisplayMode.Animate.equals(mLockPatternView
                        .getDisplayMode()))
                    doComparePattern(pattern);
            }
        }

        @Override
        public void onPatternCleared() {
            mLockPatternView.removeCallbacks(mLockPatternViewReloader);

            if (ACTION_CREATE_PATTERN.equals(getSelectedMethod())) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                mBtnConfirm.setEnabled(false);
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    fa.getIntent().removeExtra(EXTRA_PATTERN);
                    mTextInfo
                            .setText(R.string.alp_42447968_msg_draw_an_unlock_pattern);
                } else
                    mTextInfo
                            .setText(R.string.alp_42447968_msg_redraw_pattern_to_confirm);
            }
            else if (ACTION_COMPARE_PATTERN.equals(getSelectedMethod())) {
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                mTextInfo
                        .setText(R.string.alp_42447968_msg_draw_pattern_to_unlock);
            }
            else if (ACTION_VERIFY_CAPTCHA.equals(fa.getIntent().getAction())) {
                mTextInfo
                        .setText(R.string.alp_42447968_msg_redraw_pattern_to_confirm);
                List<Cell> pattern = fa.getIntent().getParcelableArrayListExtra(
                        EXTRA_PATTERN);
                mLockPatternView.setPattern(DisplayMode.Animate, pattern);
            }
        }

        @Override
        public void onPatternCellAdded(List<Cell> pattern) {
        }
    };

    private final View.OnClickListener mBtnCancelOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            finishWithNegativeResult(fa.RESULT_CANCELED);
        }
    };

    private final View.OnClickListener mBtnConfirmOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (ACTION_CREATE_PATTERN.equals(getSelectedMethod())) {
                if (mBtnOkCmd == ButtonOkCommand.CONTINUE) {
                    mBtnOkCmd = ButtonOkCommand.DONE;
                    mLockPatternView.clearPattern();
                    mTextInfo
                            .setText(R.string.alp_42447968_msg_redraw_pattern_to_confirm);
                    mBtnConfirm.setText(R.string.alp_42447968_cmd_confirm);
                    mBtnConfirm.setEnabled(false);
                } else {
                    final char[] pattern = fa.getIntent().getCharArrayExtra(
                            EXTRA_PATTERN);
                    if (mAutoSave)
                        Settings.Security.setPattern(getActivity(),
                                pattern);
                    finishWithResultOk(pattern);
                }
            }
            else if (ACTION_COMPARE_PATTERN.equals(getSelectedMethod())) {
                /*
                 * We don't need to verify the extra. First, getActivity() button is only
                 * visible if there is getActivity() extra in the intent. Second, it is
                 * the responsibility of the caller to make sure the extra is
                 * good.
                 */
                PendingIntent pi;
                try {
                    pi = fa.getIntent().getParcelableExtra(
                            EXTRA_PENDING_INTENT_FORGOT_PATTERN);
                    pi.send();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                finishWithNegativeResult(RESULT_FORGOT_PATTERN);
            }
        }
    };

    /**
     * getActivity() reloads the {@link #mLockPatternView} after a wrong pattern.
     */
    private final Runnable mLockPatternViewReloader = new Runnable() {

        @Override
        public void run() {
            mLockPatternView.clearPattern();
            mLockPatternViewListener.onPatternCleared();
        }
    };

    /**
     * Fragment constructor allowing to add a bundle with all necessary information to the fragment
     * @param method contains information about which method to choose (set
     * @return LockPatternFragment with bundle
     */
    public static LockPatternFragmentOld newInstance(String method){
        LockPatternFragmentOld fragment = new LockPatternFragmentOld();
        Bundle args = new Bundle();
        args.putString("ACTION", method);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Getter for the method string saved in fragment arguments
     * @return String telling which method was selected
     */
     public String getSelectedMethod () {
        return getArguments().getString("ACTION");
    }
}
