/*
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

package org.sufficientlysecure.keychain.ui.widget;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class UserIdEditor extends LinearLayout implements Editor, OnClickListener {
    private EditorListener mEditorListener = null;

    private BootstrapButton mDeleteButton;
    private RadioButton mIsMainUserId;
    private EditText mName;
    private String mOriginalName;
    private EditText mEmail;
    private String mOriginalEmail;
    private EditText mComment;
    private String mOriginalComment;
    private boolean mOriginallyMainUserID;
    private boolean mIsNewId;

    // see http://www.regular-expressions.info/email.html
    // RFC 2822 if we omit the syntax using double quotes and square brackets
    // android.util.Patterns.EMAIL_ADDRESS is only available as of Android 2.2+
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile(
                    "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?",
                    Pattern.CASE_INSENSITIVE);

    public void setCanEdit(boolean bCanEdit) {
        if (!bCanEdit) {
            mDeleteButton.setVisibility(View.INVISIBLE);
            mName.setEnabled(false);
            mIsMainUserId.setEnabled(false);
            mEmail.setEnabled(false);
            mComment.setEnabled(false);
        }
    }

    public static class InvalidEmailException extends Exception {
        static final long serialVersionUID = 0xf812773345L;

        public InvalidEmailException(String message) {
            super(message);
        }
    }

    public UserIdEditor(Context context) {
        super(context);
    }

    public UserIdEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            if (mEditorListener != null) {
                mEditorListener.onEdited();
            }
        }
    };

    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mDeleteButton = (BootstrapButton) findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        mIsMainUserId = (RadioButton) findViewById(R.id.isMainUserId);
        mIsMainUserId.setOnClickListener(this);

        mName = (EditText) findViewById(R.id.name);
        mName.addTextChangedListener(mTextWatcher);
        mEmail = (EditText) findViewById(R.id.email);
        mEmail.addTextChangedListener(mTextWatcher);
        mComment = (EditText) findViewById(R.id.comment);
        mComment.addTextChangedListener(mTextWatcher);

        super.onFinishInflate();
    }

    public void setValue(String userId, boolean isMainID, boolean isNewId) {

        mName.setText("");
        mOriginalName = "";
        mComment.setText("");
        mOriginalComment = "";
        mEmail.setText("");
        mOriginalEmail = "";
        mIsNewId = isNewId;

        String[] result = PgpKeyHelper.splitUserId(userId);
        if (result[0] != null) {
            mName.setText(result[0]);
            mOriginalName = result[0];
        }
        if (result[1] != null) {
            mComment.setText(result[1]);
            mOriginalComment = result[1];
        }
        if (result[2] != null) {
            mEmail.setText(result[2]);
            mOriginalEmail = result[2];
        }

        mOriginallyMainUserID = isMainID;
        setIsMainUserId(isMainID);
    }

    public String getValue() throws InvalidEmailException {
        String name = ("" + mName.getText()).trim();
        String email = ("" + mEmail.getText()).trim();
        String comment = ("" + mComment.getText()).trim();

        if (email.length() > 0) {
            Matcher emailMatcher = EMAIL_PATTERN.matcher(email);
            if (!emailMatcher.matches()) {
                throw new InvalidEmailException(getContext().getString(R.string.error_invalid_email,
                        email));
            }
        }

        String userId = name;
        if (comment.length() > 0) {
            userId += " (" + comment + ")";
        }
        if (email.length() > 0) {
            userId += " <" + email + ">";
        }

        if (userId.equals("")) {
            // ok, empty one...
            return userId;
        }
        //TODO: check gpg accepts an entirely empty ID packet. specs say this is allowed
        return userId;
    }

    public void onClick(View v) {
        final ViewGroup parent = (ViewGroup) getParent();
        if (v == mDeleteButton) {
            boolean wasMainUserId = mIsMainUserId.isChecked();
            parent.removeView(this);
            if (mEditorListener != null) {
                mEditorListener.onDeleted(this, mIsNewId);
            }
            if (wasMainUserId && parent.getChildCount() > 0) {
                UserIdEditor editor = (UserIdEditor) parent.getChildAt(0);
                editor.setIsMainUserId(true);
            }
        } else if (v == mIsMainUserId) {
            for (int i = 0; i < parent.getChildCount(); ++i) {
                UserIdEditor editor = (UserIdEditor) parent.getChildAt(i);
                if (editor == this) {
                    editor.setIsMainUserId(true);
                } else {
                    editor.setIsMainUserId(false);
                }
            }
            if (mEditorListener != null) {
                mEditorListener.onEdited();
            }
        }
    }

    public void setIsMainUserId(boolean value) {
        mIsMainUserId.setChecked(value);
    }

    public boolean isMainUserId() {
        return mIsMainUserId.isChecked();
    }

    public void setEditorListener(EditorListener listener) {
        mEditorListener = listener;
    }

    @Override
    public boolean needsSaving() {
        boolean retval = (mOriginallyMainUserID != isMainUserId());
        retval |= !(mOriginalName.equals( ("" + mName.getText()).trim() ) );
        retval |= !(mOriginalEmail.equals( ("" + mEmail.getText()).trim() ) );
        retval |= !(mOriginalComment.equals( ("" + mComment.getText()).trim() ) );
        retval |= mIsNewId;
        return retval;
    }
}
