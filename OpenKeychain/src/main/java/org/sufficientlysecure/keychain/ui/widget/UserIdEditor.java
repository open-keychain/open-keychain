/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ImageButton;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;
import org.sufficientlysecure.keychain.pgp.KeyRing;

import java.util.regex.Matcher;

public class UserIdEditor extends LinearLayout implements Editor, OnClickListener {
    private EditorListener mEditorListener = null;

    private ImageButton mDeleteButton;
    private RadioButton mIsMainUserId;
    private String mOriginalID;
    private EditText mName;
    private String mOriginalName;
    private AutoCompleteTextView mEmail;
    private String mOriginalEmail;
    private EditText mComment;
    private String mOriginalComment;
    private boolean mOriginallyMainUserID;
    private boolean mIsNewId;

    public void setCanBeEdited(boolean canBeEdited) {
        if (!canBeEdited) {
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
        public void afterTextChanged(Editable s) {
            if (mEditorListener != null) {
                mEditorListener.onEdited();
            }
        }
    };

    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mDeleteButton = (ImageButton) findViewById(R.id.delete);
        mDeleteButton.setOnClickListener(this);
        mIsMainUserId = (RadioButton) findViewById(R.id.isMainUserId);
        mIsMainUserId.setOnClickListener(this);

        mName = (EditText) findViewById(R.id.name);
        mName.addTextChangedListener(mTextWatcher);
        mEmail = (AutoCompleteTextView) findViewById(R.id.email);
        mComment = (EditText) findViewById(R.id.user_id_item_comment);
        mComment.addTextChangedListener(mTextWatcher);


        mEmail.setThreshold(1); // Start working from first character
        mEmail.setAdapter(
                new ArrayAdapter<String>
                        (this.getContext(), android.R.layout.simple_dropdown_item_1line,
                                ContactHelper.getPossibleUserEmails(getContext())
                        ));
        mEmail.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) { }

            @Override
            public void afterTextChanged(Editable editable) {
                String email = editable.toString();
                if (email.length() > 0) {
                    Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(email);
                    if (emailMatcher.matches()) {
                        mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                    R.drawable.uid_mail_ok, 0);
                    } else {
                        mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                                    R.drawable.uid_mail_bad, 0);
                    }
                } else {
                    // remove drawable if email is empty
                    mEmail.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
                if (mEditorListener != null) {
                    mEditorListener.onEdited();
                }
            }
        });

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
        mOriginalID = userId;

        String[] result = KeyRing.splitUserId(userId);
        if (result[0] != null) {
            mName.setText(result[0]);
            mOriginalName = result[0];
        }
        if (result[1] != null) {
            mEmail.setText(result[1]);
            mOriginalEmail = result[1];
        }
        if (result[2] != null) {
            mComment.setText(result[2]);
            mOriginalComment = result[2];
        }

        mOriginallyMainUserID = isMainID;
        setIsMainUserId(isMainID);
    }

    public String getValue() {
        String name = ("" + mName.getText()).trim();
        String email = ("" + mEmail.getText()).trim();
        String comment = ("" + mComment.getText()).trim();

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
        boolean retval = false; //(mOriginallyMainUserID != isMainUserId());
        retval |= !(mOriginalName.equals(("" + mName.getText()).trim()));
        retval |= !(mOriginalEmail.equals(("" + mEmail.getText()).trim()));
        retval |= !(mOriginalComment.equals(("" + mComment.getText()).trim()));
        retval |= mIsNewId;
        return retval;
    }

    public  boolean getIsOriginallyMainUserID() {
        return mOriginallyMainUserID;
    }

    public boolean primarySwapped() {
        return (mOriginallyMainUserID != isMainUserId());
    }

    public String getOriginalID() {
        return mOriginalID;
    }

    public boolean getIsNewID() { return mIsNewId; }
}
