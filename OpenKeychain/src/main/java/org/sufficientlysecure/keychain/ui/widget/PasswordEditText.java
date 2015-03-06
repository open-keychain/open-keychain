package org.sufficientlysecure.keychain.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import org.sufficientlysecure.keychain.ui.widget.passwordstrengthindicator.PasswordStrengthView;

/**
 * Developer: chipset
 * Package : org.sufficientlysecure.keychain.layouts
 * Project : open-keychain
 * Date : 6/3/15
 */
public class PasswordEditText extends EditText {

    PasswordEditText passwordEditText;
    PasswordStrengthView passwordStrengthView;

    public PasswordEditText(Context context) {
        super(context);
        passwordEditText = this;
        this.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        this.addTextChangedListener(textWatcher);
    }

    public PasswordEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        passwordEditText = this;
        this.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        this.addTextChangedListener(textWatcher);
    }

    public PasswordEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        passwordEditText = this;
        this.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        this.addTextChangedListener(textWatcher);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PasswordEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        passwordEditText = this;
        this.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        this.addTextChangedListener(textWatcher);
    }


    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            String passphrase = editable.toString();
            passwordStrengthView.setPassword(passphrase);
        }
    };

//    public PasswordStrengthView getPasswordStrengthView() {
//        return passwordStrengthView;
//    }

    public void setPasswordStrengthView(PasswordStrengthView mPasswordStrengthView) {
        this.passwordStrengthView = mPasswordStrengthView;
    }
}
